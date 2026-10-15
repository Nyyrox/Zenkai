import crypto from "node:crypto";

const DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

function getScriptStrings(script) {
  const strings = [];
  let index = 0;
  let previous = "";
  while (index < script.length) {
    const char = script[index];
    if (char === "/" && script[index + 1] === "/") {
      index = script.indexOf("\n", index + 2);
      if (index < 0) break;
      continue;
    }
    if (char === "/" && script[index + 1] === "*") {
      index = script.indexOf("*/", index + 2);
      if (index < 0) break;
      index += 2;
      continue;
    }
    if (char === "/" && /[=(:,[!&|?{};]/.test(previous)) {
      index++;
      let inClass = false;
      while (index < script.length) {
        if (script[index] === "\\") {
          index += 2;
          continue;
        }
        if (script[index] === "[") inClass = true;
        if (script[index] === "]") inClass = false;
        if (script[index] === "/" && !inClass) {
          index++;
          while (/[a-z]/i.test(script[index] ?? "")) index++;
          break;
        }
        index++;
      }
      continue;
    }
    if (char !== "'" && char !== '"') {
      if (!/\s/.test(char)) previous = char;
      index++;
      continue;
    }
    const quote = char;
    let value = "";
    index++;
    while (index < script.length && script[index] !== quote) {
      if (script[index] === "\\" && index + 1 < script.length) value += script[index++];
      value += script[index++];
    }
    strings.push(value.replace(/\\([\\'"bnfrtv])/g, (_, escaped) => ({ b: "\b", n: "\n", f: "\f", r: "\r", t: "\t", v: "\v" }[escaped] ?? escaped)));
    index++;
  }
  return [...new Set(strings)];
}

function getTemplateSuffixes(script) {
  return [...script.matchAll(/`[^`]*\$\{[^}]+\}([^`]+)`/g)].map((match) => match[1]);
}

function parseConfig(html) {
  for (const match of html.matchAll(/\b(?:var|let|const)\s+\w+\s*=\s*(\{[^;]+\})\s*;/g)) {
    try {
      const config = JSON.parse(match[1]);
      if (typeof config.sid === "string" && typeof config.pk === "string") return config;
    } catch {}
  }
  return null;
}

function getApiRoutes(html) {
  const routes = [...html.matchAll(/fetch\(\s*["']([^"']+)["']/g)].map((match) => match[1]);
  return {
    resolve: routes.find((route) => /resolve/i.test(route)) ?? null,
    verify: routes.find((route) => /verify/i.test(route)) ?? null,
  };
}

function getCryptoOptions(html, key) {
  const algorithm = html.match(/importKey\([^,]+,[^,]+,\s*\{\s*name\s*:\s*["'](AES-[A-Z]+)["']/i)?.[1];
  const ivLength = Number(html.match(/getRandomValues\(new Uint8Array\((\d+)\)\)/)?.[1]);
  const tagLength = Number(html.match(/tagLength\s*:\s*(\d+)/)?.[1]) || 128;
  if (!algorithm || !Number.isInteger(ivLength) || ivLength < 1 || !key.length) return null;
  return { cipher: `aes-${key.length * 8}-${algorithm.slice(4).toLowerCase()}`, ivLength, tagLength: tagLength / 8 };
}

function encrypt(value, key, options) {
  const iv = crypto.randomBytes(options.ivLength);
  const cipher = crypto.createCipheriv(options.cipher, key, iv, { authTagLength: options.tagLength });
  return Buffer.concat([iv, cipher.update(value, "utf8"), cipher.final(), cipher.getAuthTag()]).toString("base64");
}

function decrypt(value, key, options) {
  const raw = Buffer.from(value, "base64");
  const tagStart = raw.length - options.tagLength;
  if (tagStart <= options.ivLength) throw new Error("BabaStream encrypted payload is invalid");
  const decipher = crypto.createDecipheriv(options.cipher, key, raw.subarray(0, options.ivLength), { authTagLength: options.tagLength });
  decipher.setAuthTag(raw.subarray(tagStart));
  return Buffer.concat([decipher.update(raw.subarray(options.ivLength, tagStart)), decipher.final()]).toString("utf8");
}

function seededHex(value, length) {
  let state = 2166136261;
  for (let index = 0; index < value.length; index++) {
    state ^= value.charCodeAt(index);
    state += (state << 1) + (state << 4) + (state << 7) + (state << 8) + (state << 24);
  }
  state >>>= 0;
  let output = "";
  while (output.length < length) {
    state ^= state << 13;
    state ^= state >>> 17;
    state ^= state << 5;
    state >>>= 0;
    output += state.toString(16).padStart(8, "0");
  }
  return output.slice(0, length);
}

function solveProof(salt, target) {
  for (let nonce = 0; ; nonce++) {
    const hash = crypto.createHash("sha256").update(`${salt}${nonce}`).digest("hex");
    if (hash.startsWith(target)) return nonce;
  }
}

function solveProofs(proofs) {
  return proofs.map(({ index, salt, target }) => ({ index, nonce: solveProof(salt, target) }));
}

async function solveChallenge(payload) {
  if (Array.isArray(payload.challenges)) {
    const proofs = payload.challenges.map((challenge, index) => {
      if (challenge?.protocol !== "sha256-pow") throw new Error(`Unsupported BabaStream challenge protocol: ${challenge?.protocol}`);
      return { index, salt: challenge.payload.salt, target: challenge.payload.target };
    });
    return (await solveProofs(proofs)).map(({ nonce }) => ({ nonce }));
  }
  const { token, challenge } = payload;
  if (!token || !Number.isInteger(challenge?.c) || !Number.isInteger(challenge?.s) || !Number.isInteger(challenge?.d)) {
    throw new Error("BabaStream challenge is invalid");
  }
  const proofs = Array.from({ length: challenge.c }, (_, index) => {
    const counter = index + 1;
    return { index, salt: seededHex(`${token}${counter}`, challenge.s), target: seededHex(`${token}${counter}d`, challenge.d) };
  });
  return (await solveProofs(proofs)).map(({ nonce }) => nonce);
}

async function fetchText(fetchImpl, url, headers) {
  const response = await fetchImpl(url, { headers });
  if (!response.ok) throw new Error(`BabaStream HTTP ${response.status}: ${url}`);
  return response.text();
}

async function fetchJson(fetchImpl, url, options) {
  const response = await fetchImpl(url, options);
  if (!response.ok) throw new Error(`BabaStream HTTP ${response.status}: ${url}`);
  return response.json();
}

export function canExtractBabaStream(url) {
  return /babastream\.[^/]+\/embed\//i.test(String(url));
}

export async function extractBabaStreamDetails(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT, referer } = {}) {
  const pageUrl = new URL(String(embedUrl));
  const pageHeaders = {
    "User-Agent": userAgent,
    "Accept": "text/html,*/*",
    "Referer": referer ?? `${pageUrl.origin}/`,
  };
  const html = await fetchText(fetchImpl, pageUrl, pageHeaders);
  const config = parseConfig(html);
  if (!config?.cap) throw new Error(`BabaStream config not found: ${embedUrl}`);
  const key = Buffer.from(config.pk, "base64");
  const cryptoOptions = getCryptoOptions(html, key);
  const routes = getApiRoutes(html);
  if (!cryptoOptions || !routes.resolve || !routes.verify) throw new Error(`BabaStream client routes not found: ${embedUrl}`);
  const encryptedRequest = async (route, body) => {
    const response = await fetchJson(fetchImpl, new URL(route, pageUrl), {
      method: "POST",
      headers: { "Content-Type": "application/json", "User-Agent": userAgent, "Referer": pageUrl.href },
      body: JSON.stringify({ s: config.sid, d: encrypt(JSON.stringify(body), key, cryptoOptions) }),
    });
    if (!response?.d) throw new Error("BabaStream response is missing encrypted data");
    return JSON.parse(decrypt(response.d, key, cryptoOptions));
  };
  let resolved = await encryptedRequest(routes.resolve, { ts: Date.now() });
  if (resolved?.t === "error" && /verify/i.test(resolved.m ?? "")) {
    const capScripts = [...html.matchAll(/<script[^>]+src=["']([^"']+)["']/gi)].map((match) => new URL(match[1], pageUrl).href);
    const widget = (await Promise.all(capScripts.map(async (url) => {
      try { return await fetchText(fetchImpl, url, { "User-Agent": userAgent, "Referer": pageUrl.href }); } catch { return null; }
    }))).find((script) => /challenge/i.test(script) && /redeem/i.test(script) && /SHA-256/i.test(script));
    const widgetStrings = widget ? [...getScriptStrings(widget), ...getTemplateSuffixes(widget)] : [];
    const challengePath = widgetStrings.find((value) => value === "challenge");
    const redeemPath = widgetStrings.find((value) => value === "redeem");
    if (!widget || !challengePath || !redeemPath) throw new Error("BabaStream challenge client not found");
    const capHeaders = { "Content-Type": "application/json", "User-Agent": userAgent, "Referer": pageUrl.href };
    const challenge = await fetchJson(fetchImpl, new URL(challengePath, config.cap), { method: "POST", headers: capHeaders });
    const solutions = await solveChallenge(challenge);
    const redeemed = await fetchJson(fetchImpl, new URL(redeemPath, config.cap), {
      method: "POST",
      headers: capHeaders,
      body: JSON.stringify({ token: challenge.token, solutions }),
    });
    if (!redeemed?.success || !redeemed.token) throw new Error("BabaStream challenge verification failed");
    const verified = await encryptedRequest(routes.verify, { ts: Date.now(), token: redeemed.token, mode: "invisible" });
    if (verified?.t !== "ok") throw new Error("BabaStream cap verification failed");
    resolved = await encryptedRequest(routes.resolve, { ts: Date.now() });
  }
  if (!resolved?.u) throw new Error(resolved?.m || "BabaStream did not return a stream");
  return {
    url: resolved.u,
    type: /\.m3u8(?:$|[?&])/i.test(resolved.u) ? "hls" : resolved.t === "embed" ? "embed" : "mp4",
    origin: pageUrl.origin,
  };
}

export async function extractBabaStream(embedUrl, options = {}) {
  const source = await extractBabaStreamDetails(embedUrl, options);
  return [source];
}
