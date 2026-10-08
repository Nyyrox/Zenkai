var __defProp = Object.defineProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });

// core/anilist.js
var __name2 = /* @__PURE__ */ __name((fn, _) => fn, "__name");
var resolved = /* @__PURE__ */ new Map();
var inflight = /* @__PURE__ */ new Map();
var UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var ARM = "https://arm.haglund.dev/api/v2/ids";
var JIKAN = "https://api.jikan.moe/v4";
var STATUS_MAP = {
  "Currently Airing": "RELEASING",
  "Finished Airing": "FINISHED",
  "Not yet aired": "NOT_YET_RELEASED",
  "On Hiatus": "HIATUS"
};
var AL_STATUS_MAP = {
  RELEASING: "RELEASING",
  FINISHED: "FINISHED",
  NOT_YET_RELEASED: "NOT_YET_RELEASED",
  CANCELLED: "FINISHED",
  HIATUS: "HIATUS"
};
async function fetchFromAniList(id) {
  const fullQuery = `query($id:Int){Media(id:$id,type:ANIME){id title{english romaji native} status format episodes seasonYear startDate{year} synonyms nextAiringEpisode{episode airingAt timeUntilAiring}}}`;
  const res = await fetch("https://graphql.anilist.co", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json", "User-Agent": UA },
    body: JSON.stringify({ query: fullQuery, variables: { id } })
  }).catch(() => null);
  if (!res || !res.ok) return null;
  const json5 = await res.json();
  return json5.data?.Media ?? null;
}
__name(fetchFromAniList, "fetchFromAniList");
async function getMedia(anilistId) {
  const id = Number(anilistId);
  if (resolved.has(id)) return resolved.get(id);
  if (inflight.has(id)) return inflight.get(id);
  const promise = (async () => {
    const arm = await fetch(`${ARM}?source=anilist&id=${id}`, {
      headers: { "User-Agent": UA, "Accept": "application/json" }
    }).then((r) => {
      if (!r.ok) return null;
      return r.json();
    }).catch(() => null);
    const malId = arm?.myanimelist ?? null;
    if (!malId) {
      const al2 = await fetchFromAniList(id);
      if (!al2) throw new Error(`No data found for AniList ID ${id}`);
      const media2 = {
        id,
        idMal: null,
        title: {
          english: al2.title?.english ?? null,
          romaji: al2.title?.romaji ?? null,
          native: al2.title?.native ?? null
        },
        status: AL_STATUS_MAP[al2.status] ?? "RELEASING",
        format: al2.format ?? null,
        episodes: al2.episodes ?? null,
        seasonYear: al2.seasonYear ?? null,
        startDate: al2.startDate ?? null,
        nextAiringEpisode: al2.nextAiringEpisode ?? null,
        synonyms: Array.isArray(al2.synonyms) ? al2.synonyms : []
      };
      resolved.set(id, media2);
      inflight.delete(id);
      return media2;
    }
    const al = await fetchFromAniList(id).catch(() => null);
    let jikan = null;
    for (let attempt = 0; attempt <= 4; attempt++) {
      const r = await fetch(`${JIKAN}/anime/${malId}`, { headers: { "User-Agent": UA, Accept: "application/json" } });
      if (r.status === 429) {
        const wait = (parseInt(r.headers.get("Retry-After") ?? "1") || 1) * 1e3 + attempt * 500;
        if (attempt < 4) {
          await new Promise((res) => setTimeout(res, wait));
          continue;
        }
        throw new Error(`Jikan 429 for MAL ID ${malId} (exhausted retries)`);
      }
      if (!r.ok) {
        if (al) break;
        throw new Error(`Jikan ${r.status}`);
      }
      jikan = await r.json();
      break;
    }
    const d = jikan?.data ?? null;
    if (!d && al) {
      const media2 = {
        id,
        idMal: malId,
        title: {
          english: al.title?.english ?? null,
          romaji: al.title?.romaji ?? null,
          native: al.title?.native ?? null
        },
        status: AL_STATUS_MAP[al.status] ?? "RELEASING",
        format: al.format ?? null,
        episodes: al.episodes ?? null,
        seasonYear: al.seasonYear ?? null,
        startDate: al.startDate ?? null,
        nextAiringEpisode: al.nextAiringEpisode ?? null,
        synonyms: Array.isArray(al.synonyms) ? al.synonyms : []
      };
      resolved.set(id, media2);
      inflight.delete(id);
      return media2;
    }
    if (!d) throw new Error(`Jikan returned no data for MAL ID ${malId}`);
    const media = {
      id,
      idMal: malId,
      title: {
        english: al?.title?.english ?? d.title_english ?? null,
        romaji: al?.title?.romaji ?? d.title ?? null,
        native: al?.title?.native ?? d.title_japanese ?? null
      },
      status: AL_STATUS_MAP[al?.status] ?? STATUS_MAP[d.status] ?? "RELEASING",
      format: al?.format ?? d.type ?? null,
      episodes: al?.episodes ?? d.episodes ?? null,
      seasonYear: al?.seasonYear ?? d.year ?? null,
      startDate: al?.startDate ?? (d.aired?.from ? { year: new Date(d.aired.from).getFullYear() } : null),
      nextAiringEpisode: al?.nextAiringEpisode ?? null,
      synonyms: [
        ...d.titles?.map((t) => t.title).filter(Boolean) ?? [],
        ...Array.isArray(al?.synonyms) ? al.synonyms : []
      ]
    };
    resolved.set(id, media);
    inflight.delete(id);
    return media;
  })().catch((e) => {
    inflight.delete(id);
    throw e;
  });
  inflight.set(id, promise);
  return promise;
}
__name(getMedia, "getMedia");
__name2(getMedia, "getMedia");
function forgetMedia(anilistId) {
  resolved.delete(Number(anilistId));
}
__name(forgetMedia, "forgetMedia");

// core/mapper.js
var __name3 = /* @__PURE__ */ __name((fn, _) => fn, "__name");
var ARM2 = "https://arm.haglund.dev/api/v2/ids";
var UA2 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0";
function hashFranchiseId(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i) | 0;
  }
  return h >>> 0;
}
__name(hashFranchiseId, "hashFranchiseId");
__name3(hashFranchiseId, "hashFranchiseId");
async function fetchARM(anilistId) {
  const res = await fetch(`${ARM2}?source=anilist&id=${anilistId}`, {
    headers: { "User-Agent": UA2, "Accept": "application/json" }
  }).catch(() => null);
  if (!res || !res.ok) return null;
  return res.json().catch(() => null);
}
__name(fetchARM, "fetchARM");
__name3(fetchARM, "fetchARM");
async function fetchAniListRelations(anilistId) {
  const q = `
  query ($id: Int) {
    Media(id: $id, type: ANIME) {
      id synonyms
      relations {
        edges {
          relationType(version: 2)
          node {
            id type format title { romaji english native }
            relations {
              edges {
                relationType(version: 2)
                node { id type format title { romaji english native } }
              }
            }
          }
        }
      }
    }
  }`;
  try {
    const res = await fetch("https://graphql.anilist.co", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      body: JSON.stringify({ query: q, variables: { id: Number(anilistId) } })
    });
    if (!res.ok) return null;
    const json6 = await res.json();
    return json6.data?.Media ?? null;
  } catch {
    return null;
  }
}
__name(fetchAniListRelations, "fetchAniListRelations");
__name3(fetchAniListRelations, "fetchAniListRelations");
async function mapAnimeIds(anilistId) {
  const [arm, media, alRelations] = await Promise.all([
    fetchARM(anilistId),
    getMedia(anilistId).catch(() => null),
    fetchAniListRelations(anilistId)
  ]);
  const malId = arm?.myanimelist ?? null;
  const format = media?.format ?? null;
  const year = media?.seasonYear ?? null;
  const titleEn = media?.title?.english || null;
  const titleRom = media?.title?.romaji || null;
  const synonyms = [...media?.synonyms ?? []];
  if (alRelations?.synonyms) {
    for (const s of alRelations.synonyms) {
      if (!synonyms.includes(s)) synonyms.push(s);
    }
  }
  const franchiseMap = /* @__PURE__ */ new Map();
  if (alRelations?.relations?.edges) {
    for (const e1 of alRelations.relations.edges) {
      if (!franchiseMap.has(e1.node.id)) {
        franchiseMap.set(e1.node.id, {
          relation: e1.relationType,
          anilistId: e1.node.id,
          title: e1.node.title.romaji || e1.node.title.english,
          type: e1.node.type,
          format: e1.node.format
        });
      }
      if (e1.node.relations?.edges) {
        for (const e2 of e1.node.relations.edges) {
          if (e2.node.id === Number(anilistId)) continue;
          if (!franchiseMap.has(e2.node.id)) {
            franchiseMap.set(e2.node.id, {
              relation: e2.relationType,
              anilistId: e2.node.id,
              title: e2.node.title.romaji || e2.node.title.english,
              type: e2.node.type,
              format: e2.node.format
            });
          }
        }
      }
    }
  }
  const thetvdbId = arm?.thetvdb ?? null;
  const themoviedbId = arm?.themoviedb ?? null;
  const imdbId = arm?.imdb ?? null;
  return {
    mappings: {
      id: Number(anilistId),
      title: titleEn || titleRom,
      type: arm?.media ?? null,
      format,
      episodes: media?.episodes ?? null,
      malId,
      aniId: Number(anilistId),
      anidbId: arm?.anidb ?? null,
      animePlanetId: arm?.["anime-planet"] ?? null,
      kitsuId: arm?.kitsu ?? null,
      animeCountdownId: arm?.animecountdown ?? null,
      anisearchId: arm?.anisearch ?? null,
      notifyMoeId: null,
      simklId: arm?.simkl ?? null,
      imdbId,
      themoviedbId,
      thetvdbId,
      livechartId: arm?.livechart ?? null,
      annId: arm?.animenewsnetwork ?? null,
      animescheduleId: null,
      animethemesId: null,
      animefillerlistId: null,
      franchiseAnchor: thetvdbId ? `tvdb:${thetvdbId}` : null,
      franchiseId: thetvdbId ? hashFranchiseId(`tvdb:${thetvdbId}`) : null,
      defaultTvdbSeason: arm?.["thetvdb-season"] != null ? String(arm["thetvdb-season"]) : null,
      tmdbSeason: arm?.["themoviedb-season"] != null ? String(arm["themoviedb-season"]) : null,
      episodeOffset: null,
      tmdbOffset: null,
      malIds: null,
      aniskip: null,
      animefillerlist: null,
      synonyms,
      franchise: Array.from(franchiseMap.values())
    }
  };
}
__name(mapAnimeIds, "mapAnimeIds");
__name3(mapAnimeIds, "mapAnimeIds");

// providers/mkissa.js
import crypto from "node:crypto";
var __name4 = /* @__PURE__ */ __name((fn, _) => fn, "__name");
var UA4 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36";
var REFERER = "https://mkissa.to";
var API = "https://api.mkissa.net";
var API_URL = `${API}/api`;
var CDN_ROOT = "https://cdn.mkissa.net/all/mk";
var ANIZIP = "https://api.ani.zip/mappings";
var CONTENT_LANE = "k7";
var REFERER_HOST = "mkissa.to";
var KEY_GROUP = "mkissa";
var BOOT_EPOCH_MS = 6048e5;
var BOOT_GRACE_MS = 864e5;
var AA_REQ_MS = 3e5;
var WATCH_MEMORY_TTL = 3 * 60 * 60 * 1e3;
var DISCOVERY_CONCURRENCY = 16;
var DISCOVERY_LIMIT = 600;
var FETCH_TIMEOUT_MS = 1e4;
var EXTRACT_TIMEOUT_MS = 5e3;
var TMDB_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlYjdkMWM0ZTgwMGUzM2FiMmE3Y2I3NDA5YmM4NjQ2YSIsIm5iZiI6MTc3OTUzMDcxOS40MzIsInN1YiI6IjZhMTE3YmRmYTlhNjNlYmFiOWUzYjc4YyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.Z9pa96oJEyicf6wAoaKGKJd9ldapeiOdktoJd4xcgLo";
var HEX_TABLE = {
  "79": "A",
  "7a": "B",
  "7b": "C",
  "7c": "D",
  "7d": "E",
  "7e": "F",
  "7f": "G",
  "70": "H",
  "71": "I",
  "72": "J",
  "73": "K",
  "74": "L",
  "75": "M",
  "76": "N",
  "77": "O",
  "68": "P",
  "69": "Q",
  "6a": "R",
  "6b": "S",
  "6c": "T",
  "6d": "U",
  "6e": "V",
  "6f": "W",
  "60": "X",
  "61": "Y",
  "62": "Z",
  "59": "a",
  "5a": "b",
  "5b": "c",
  "5c": "d",
  "5d": "e",
  "5e": "f",
  "5f": "g",
  "50": "h",
  "51": "i",
  "52": "j",
  "53": "k",
  "54": "l",
  "55": "m",
  "56": "n",
  "57": "o",
  "48": "p",
  "49": "q",
  "4a": "r",
  "4b": "s",
  "4c": "t",
  "4d": "u",
  "4e": "v",
  "4f": "w",
  "40": "x",
  "41": "y",
  "42": "z",
  "08": "0",
  "09": "1",
  "0a": "2",
  "0b": "3",
  "0c": "4",
  "0d": "5",
  "0e": "6",
  "0f": "7",
  "00": "8",
  "01": "9",
  "15": "-",
  "16": ".",
  "67": "_",
  "46": "~",
  "02": ":",
  "17": "/",
  "07": "?",
  "1b": "#",
  "63": "[",
  "65": "]",
  "78": "@",
  "19": "!",
  "1c": "$",
  "1e": "&",
  "10": "(",
  "11": ")",
  "12": "*",
  "13": "+",
  "14": ",",
  "03": ";",
  "05": "=",
  "1d": "%"
};
var cryptoConfigCache = null;
var bootstrapCache = null;
var episodeQueryCache = null;
var sessionCookies = /* @__PURE__ */ new Map();
var watchMemoryCache = /* @__PURE__ */ new Map();
function decodeHexUrl(hex) {
  let out = "";
  for (let i = 0; i < hex.length; i += 2) {
    const pair = hex.substring(i, i + 2).toLowerCase();
    out += HEX_TABLE[pair] ?? pair;
  }
  return out;
}
__name(decodeHexUrl, "decodeHexUrl");
__name4(decodeHexUrl, "decodeHexUrl");
function sha256Hex(value) {
  return crypto.createHash("sha256").update(value).digest("hex");
}
__name(sha256Hex, "sha256Hex");
__name4(sha256Hex, "sha256Hex");
function hmacBytes(key, value) {
  return crypto.createHmac("sha256", key).update(value).digest();
}
__name(hmacBytes, "hmacBytes");
__name4(hmacBytes, "hmacBytes");
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
__name(sleep, "sleep");
__name4(sleep, "sleep");
function storeCookies(headers) {
  const raw = typeof headers.getSetCookie === "function" ? headers.getSetCookie() : [headers.get("set-cookie")].filter(Boolean);
  for (const value of raw) {
    for (const part of String(value).split(/,(?=[^;,]+=)/)) {
      const pair = part.split(";")[0]?.trim();
      const index = pair?.indexOf("=");
      if (index > 0) sessionCookies.set(pair.slice(0, index), pair.slice(index + 1));
    }
  }
}
__name(storeCookies, "storeCookies");
__name4(storeCookies, "storeCookies");
function cookieHeader() {
  return [...sessionCookies].map(([key, value]) => `${key}=${value}`).join("; ");
}
__name(cookieHeader, "cookieHeader");
__name4(cookieHeader, "cookieHeader");
function browserHeaders(headers = {}) {
  const cookie = cookieHeader();
  return {
    "User-Agent": UA4,
    "Accept": "*/*",
    "Accept-Language": "en-US,en;q=0.9",
    "sec-ch-ua": '"Not=A?Brand";v="99", "Google Chrome";v="151", "Chromium";v="151"',
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": '"Windows"',
    ...cookie ? { Cookie: cookie } : {},
    ...headers
  };
}
__name(browserHeaders, "browserHeaders");
__name4(browserHeaders, "browserHeaders");
function apiHeaders(buildId, headers = {}) {
  return {
    "Referer": `${REFERER}/`,
    "Origin": REFERER,
    "x-build-id": buildId,
    "Cache-Control": "no-cache",
    "Pragma": "no-cache",
    "Priority": "u=1, i",
    "Sec-Fetch-Dest": "empty",
    "Sec-Fetch-Mode": "cors",
    "Sec-Fetch-Site": "cross-site",
    ...headers
  };
}
__name(apiHeaders, "apiHeaders");
__name4(apiHeaders, "apiHeaders");
async function sessionFetch(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: browserHeaders(options.headers || {})
  });
  storeCookies(res.headers);
  return res;
}
__name(sessionFetch, "sessionFetch");
__name4(sessionFetch, "sessionFetch");
function absoluteAssetUrl(value, base = CDN_ROOT) {
  return new URL(value, base.endsWith("/") ? base : `${base}/`).toString();
}
__name(absoluteAssetUrl, "absoluteAssetUrl");
__name4(absoluteAssetUrl, "absoluteAssetUrl");
function findBalancedBlock(text, start) {
  let depth = 0;
  let seen = false;
  for (let i = start; i < text.length; i++) {
    const ch = text[i];
    if (ch === "{") {
      depth++;
      seen = true;
    } else if (ch === "}") {
      depth--;
      if (seen && depth === 0) return i + 1;
    }
  }
  return -1;
}
__name(findBalancedBlock, "findBalancedBlock");
__name4(findBalancedBlock, "findBalancedBlock");
function findOpeningParen(text, end) {
  let depth = 0;
  for (let i = end; i >= 0; i--) {
    const ch = text[i];
    if (ch === ")") depth++;
    else if (ch === "(" && --depth === 0) return i;
  }
  return -1;
}
__name(findOpeningParen, "findOpeningParen");
__name4(findOpeningParen, "findOpeningParen");
function normalizeCryptoConfig(out) {
  if (!out?.buildId || !Array.isArray(out.maskParts) || out.maskParts.length < 4) return null;
  return {
    scheme: "legacy",
    buildId: String(out.buildId),
    maskParts: out.maskParts.slice(0, 4).map(String)
  };
}
__name(normalizeCryptoConfig, "normalizeCryptoConfig");
__name4(normalizeCryptoConfig, "normalizeCryptoConfig");
function functionSource(text, name) {
  const match = new RegExp(`function\\s+${name}\\s*\\(`).exec(text);
  if (!match) return null;
  const start = text.indexOf("{", match.index);
  const end = findBalancedBlock(text, start);
  return end === -1 ? null : text.slice(match.index, end);
}
__name(functionSource, "functionSource");
__name4(functionSource, "functionSource");
function findStatementEnd(text, start) {
  let parens = 0;
  let brackets = 0;
  let braces = 0;
  let quote = "";
  for (let i = start; i < text.length; i++) {
    const ch = text[i];
    if (quote) {
      if (ch === "\\") i++;
      else if (ch === quote) quote = "";
      continue;
    }
    if (ch === '"' || ch === "'" || ch === "`") {
      quote = ch;
      continue;
    }
    if (ch === "(") parens++;
    else if (ch === ")") parens--;
    else if (ch === "[") brackets++;
    else if (ch === "]") brackets--;
    else if (ch === "{") braces++;
    else if (ch === "}") braces--;
    else if (ch === ";" && !parens && !brackets && !braces) return i;
  }
  return -1;
}
__name(findStatementEnd, "findStatementEnd");
__name4(findStatementEnd, "findStatementEnd");
function splitTopLevel(text) {
  const out = [];
  let start = 0;
  let parens = 0;
  let brackets = 0;
  let braces = 0;
  let quote = "";
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (quote) {
      if (ch === "\\") i++;
      else if (ch === quote) quote = "";
      continue;
    }
    if (ch === '"' || ch === "'" || ch === "`") {
      quote = ch;
      continue;
    }
    if (ch === "(") parens++;
    else if (ch === ")") parens--;
    else if (ch === "[") brackets++;
    else if (ch === "]") brackets--;
    else if (ch === "{") braces++;
    else if (ch === "}") braces--;
    else if (ch === "," && !parens && !brackets && !braces) {
      out.push(text.slice(start, i));
      start = i + 1;
    }
  }
  out.push(text.slice(start));
  return out;
}
__name(splitTopLevel, "splitTopLevel");
__name4(splitTopLevel, "splitTopLevel");
function declarationStatementAt(text, index) {
  const start = Math.max(text.lastIndexOf("const ", index), text.lastIndexOf("let ", index), text.lastIndexOf("var ", index));
  if (start < 0) return null;
  const end = findStatementEnd(text, start);
  if (end < 0 || index > end) return null;
  const keyword = /^(?:const|let|var)\s+/.exec(text.slice(start));
  if (!keyword) return null;
  const entries = splitTopLevel(text.slice(start + keyword[0].length, end)).map((value) => {
    const entry = /^\s*([A-Za-z_$][\w$]*)\s*=\s*([\s\S]+)$/.exec(value);
    return entry ? { name: entry[1], expression: entry[2], start, end } : null;
  }).filter(Boolean);
  return { start, end, entries };
}
__name(declarationStatementAt, "declarationStatementAt");
__name4(declarationStatementAt, "declarationStatementAt");
function templateDependencies(expression) {
  return [...expression.matchAll(/\$\{\s*([A-Za-z_$][\w$]*)\b[^}]*\}/g)].map((match) => match[1]);
}
__name(templateDependencies, "templateDependencies");
__name4(templateDependencies, "templateDependencies");
function templateDeclaration(chunk, name, before = chunk.length) {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const matches = [...chunk.matchAll(new RegExp(`\\b${escaped}\\s*=`, "g"))];
  let fallback = null;
  for (const match of matches) {
    const statement = declarationStatementAt(chunk, match.index);
    const entry = statement?.entries.find((value) => value.name === name);
    if (!entry) continue;
    if (!fallback) fallback = entry;
    if (entry.start < before) fallback = entry;
  }
  return fallback;
}
__name(templateDeclaration, "templateDeclaration");
__name4(templateDeclaration, "templateDeclaration");
function evalEpisodeQueryChunk(chunk) {
  const operation = /\bepisode\s*\(\s*showId\s*:\s*\$showId\s*translationType\s*:\s*\$translationType\s*episodeString\s*:\s*\$episodeString\s*\)/g;
  const operationInExpression = new RegExp(operation.source);
  for (const match of chunk.matchAll(operation)) {
    const statement = declarationStatementAt(chunk, match.index);
    const candidate = statement?.entries.find((entry) => operationInExpression.test(entry.expression));
    if (!candidate) continue;
    const definitions = /* @__PURE__ */ new Map();
    const resolving = /* @__PURE__ */ new Set();
    const resolve = /* @__PURE__ */ __name((name, before) => {
      if (definitions.has(name) || resolving.has(name)) return;
      const entry = templateDeclaration(chunk, name, before);
      if (!entry) return;
      resolving.add(name);
      for (const dependency of templateDependencies(entry.expression)) resolve(dependency, entry.start);
      resolving.delete(name);
      definitions.set(name, entry);
    }, "resolve");
    resolve(candidate.name, candidate.start + 1);
    try {
      const source = [...definitions.values()].map((entry) => `const ${entry.name}=${entry.expression};`).join("\n");
      const query = Function(`${source}
return ${candidate.name}();`)();
      if (typeof query === "string" && /\bepisode\s*\(/.test(query) && query.includes("$episodeString") && !query.includes("${")) return query;
    } catch {
    }
  }
  return null;
}
__name(evalEpisodeQueryChunk, "evalEpisodeQueryChunk");
__name4(evalEpisodeQueryChunk, "evalEpisodeQueryChunk");
function evalFragmentCryptoChunk(chunk) {
  for (const match of chunk.matchAll(/\bsaltMul\s*:/g)) {
    const declarationAt = chunk.lastIndexOf("const ", match.index);
    const declarationEnd = declarationAt === -1 ? -1 : findStatementEnd(chunk, declarationAt);
    if (declarationEnd === -1 || match.index > declarationEnd) continue;
    const declarations = splitTopLevel(chunk.slice(declarationAt + 6, declarationEnd)).map((value) => {
      const entry = /^\s*([A-Za-z_$][\w$]*)\s*=\s*([\s\S]+)$/.exec(value);
      return entry ? { name: entry[1], expression: entry[2] } : null;
    }).filter(Boolean);
    const configIndex = declarations.findIndex((entry) => /\b(?:saltMul|fragMul)\s*:/.test(entry.expression));
    const partsIndex = declarations.slice(0, configIndex).map((entry, index) => ({ entry, index })).reverse().find(({ entry }) => entry.expression.trim().startsWith("["))?.index;
    if (configIndex < 1 || partsIndex === void 0) continue;
    const [build] = declarations;
    const parts = declarations[partsIndex];
    const params = declarations[configIndex];
    const expression = `${build.expression};${parts.expression};${params.expression}`;
    const names = new Set([...expression.matchAll(/\b([A-Za-z_$][\w$]*)\s*\(/g)].map((entry) => entry[1]));
    const helpers = [];
    for (let pass = 0; pass < 8; pass++) {
      let changed = false;
      for (const name of [...names]) {
        if (helpers.some((source3) => new RegExp(`function\\s+${name}\\s*\\(`).test(source3))) continue;
        const source2 = functionSource(chunk, name);
        if (!source2) continue;
        helpers.push(source2);
        for (const reference of source2.matchAll(/\b([A-Za-z_$][\w$]*)\s*\(/g)) {
          if (!names.has(reference[1])) {
            names.add(reference[1]);
            changed = true;
          }
        }
      }
      if (helpers.some((source2) => /^function\s+[A-Za-z_$][\w$]*\s*\(\)\s*\{const\s+e=\[/.test(source2))) break;
      if (!changed) break;
    }
    const tableSource = helpers.find((source2) => /^function\s+[A-Za-z_$][\w$]*\s*\(\)\s*\{const\s+e=\[/.test(source2)) ?? null;
    const tableName = tableSource?.match(/^function\s+([A-Za-z_$][\w$]*)\s*\(/)?.[1] ?? null;
    const tableInitEnd = tableName ? chunk.lastIndexOf(`)(${tableName},`, declarationAt) : -1;
    const tableInitStart = tableInitEnd === -1 ? -1 : findOpeningParen(chunk, tableInitEnd);
    const tableInit = tableInitStart === -1 || tableInitEnd === -1 ? "" : chunk.slice(tableInitStart, chunk.indexOf(";", tableInitEnd) + 1);
    if (!tableSource || !tableInit) continue;
    const source = `${tableSource}
${tableInit}
${helpers.filter((helper) => !helper.startsWith(`function ${tableName}`)).join("\n")}
return { buildId: (${build.expression}), maskParts: (${parts.expression}), params: (${params.expression}) };`;
    const out = Function(source)();
    const config = out?.params;
    if (!out?.buildId || !Array.isArray(out.maskParts) || out.maskParts.length < 4 || !config || typeof config !== "object") continue;
    const numeric = ["saltMul", "saltAdd", "fragMul", "fragAdd"];
    if (numeric.some((key) => !Number.isFinite(Number(config[key])))) continue;
    if (!Array.isArray(config.parts) || !config.parts.length || typeof config.bootPrefix !== "string" || typeof config.join !== "string") continue;
    return {
      scheme: "fragments",
      buildId: String(out.buildId),
      maskParts: out.maskParts.slice(0, 4).map(String),
      saltMul: Number(config.saltMul),
      saltAdd: Number(config.saltAdd),
      fragMul: Number(config.fragMul),
      fragAdd: Number(config.fragAdd),
      bootPrefix: config.bootPrefix,
      join: config.join,
      parts: config.parts.map(String),
      omitEmptyLane: Boolean(config.omitEmptyLane)
    };
  }
  return null;
}
__name(evalFragmentCryptoChunk, "evalFragmentCryptoChunk");
__name4(evalFragmentCryptoChunk, "evalFragmentCryptoChunk");
function evalOldCryptoChunk(chunk) {
  const cryptoStart = chunk.search(/const\s+[A-Za-z_$][\w$]*\s*=[^;]{0,180}\?"\d+":"",\s*[A-Za-z_$][\w$]*=\[/);
  if (cryptoStart < 0) return null;
  const tableMatches = [...chunk.slice(0, cryptoStart).matchAll(/function\s+([A-Za-z_$][\w$]*)\s*\(\)\{const e=\[/g)];
  const tableStart = tableMatches.at(-1)?.index ?? -1;
  const asyncStart = chunk.indexOf("async function", cryptoStart);
  if (tableStart < 0 || asyncStart < 0) return null;
  let code = chunk.slice(tableStart, asyncStart);
  const buildMatch = code.match(/const\s+([A-Za-z_$][\w$]*)\s*=([^;]+?\?"(\d+)":"")\s*,\s*([A-Za-z_$][\w$]*)=\[/);
  if (!buildMatch) return null;
  const buildName = buildMatch[1];
  const maskName = buildMatch[4];
  code = code.replace(new RegExp(`\\b[A-Za-z_$][\\w$]*\\(\\);\\s*const\\s+${buildName}=`), `const ${buildName}=`);
  code = code.replace(new RegExp(`const\\s+${buildName}=`), `var ${buildName}=`);
  code = code.replace(new RegExp(`,\\s*${maskName}=\\[`), `;var ${maskName}=[`);
  code += `
return { buildId: ${buildName}, maskParts: ${maskName} };`;
  return normalizeCryptoConfig(Function(code)());
}
__name(evalOldCryptoChunk, "evalOldCryptoChunk");
__name4(evalOldCryptoChunk, "evalOldCryptoChunk");
function evalModernCryptoChunk(chunk) {
  const cryptoStart = chunk.search(/const\s+[A-Za-z_$][\w$]*\s*=[^;]{0,220}\?"\d+":"",\s*[A-Za-z_$][\w$]*=\[/);
  if (cryptoStart < 0) return null;
  const wrapperMatch = [...chunk.slice(0, cryptoStart).matchAll(/const\s+[A-Za-z_$][\w$]*=\(function\(\)\{/g)].at(-1);
  const wrapperStart = wrapperMatch?.index ?? -1;
  const tableMatch = [...chunk.slice(0, wrapperStart).matchAll(/function\s+[A-Za-z_$][\w$]*\s*\(\)\{const\s+[A-Za-z_$][\w$]*=\[/g)].at(-1);
  const tableStart = tableMatch?.index ?? -1;
  const decoderMatch = [...chunk.slice(0, tableStart).matchAll(/function\s+[A-Za-z_$][\w$]*\s*\([A-Za-z_$][\w$]*(?:,[A-Za-z_$][\w$]*)?\)\{return\s+[A-Za-z_$][\w$]*=[A-Za-z_$][\w$]*-\d+,[A-Za-z_$][\w$]*\(\)\[[A-Za-z_$][\w$]*\]\}/g)].at(-1);
  const tiStart = decoderMatch?.index ?? -1;
  const asyncStart = chunk.indexOf("async function", cryptoStart);
  if (tiStart < 0 || wrapperStart < 0 || asyncStart < 0) return null;
  const head = chunk.slice(tiStart, wrapperStart);
  let body = chunk.slice(cryptoStart, asyncStart);
  const buildMatch = body.match(/const\s+([A-Za-z_$][\w$]*)=/);
  const maskMatch = body.match(/,([A-Za-z_$][\w$]*)=\[/);
  const maskFunction = body.match(/function\s+([A-Za-z_$][\w$]*)\s*\([^)]*=\s*([A-Za-z_$][\w$]*)\)/);
  if (!buildMatch || !maskMatch || !maskFunction) return null;
  const buildName = buildMatch[1];
  const maskName = maskMatch[1];
  const maskFunctionName = maskFunction[1];
  body = body.replace(new RegExp(`const\\s+${buildName}=`), `var ${buildName}=`);
  body = body.replace(new RegExp(`,${maskName}=\\[`), `;var ${maskName}=[`);
  body += `
return { buildId: ${buildName}, maskParts: ${maskName}, mask: Array.from(${maskFunctionName}(${buildName}) || []) };`;
  return normalizeCryptoConfig(Function(head + body)());
}
__name(evalModernCryptoChunk, "evalModernCryptoChunk");
__name4(evalModernCryptoChunk, "evalModernCryptoChunk");
function evalCryptoChunk(chunk) {
  try {
    const config = evalFragmentCryptoChunk(chunk);
    if (config) return config;
  } catch {
  }
  try {
    return evalModernCryptoChunk(chunk);
  } catch {
  }
  try {
    return evalOldCryptoChunk(chunk);
  } catch {
  }
  return null;
}
__name(evalCryptoChunk, "evalCryptoChunk");
__name4(evalCryptoChunk, "evalCryptoChunk");
async function fetchText(url, headers = {}) {
  const ac = new AbortController();
  const timer = setTimeout(() => ac.abort(), FETCH_TIMEOUT_MS);
  try {
    const res = await sessionFetch(url, {
      signal: ac.signal,
      headers: {
        "Referer": `${REFERER}/`,
        ...headers
      }
    });
    if (!res.ok) throw new Error(`Fetch ${res.status}: ${url}`);
    return res.text();
  } finally {
    clearTimeout(timer);
  }
}
__name(fetchText, "fetchText");
__name4(fetchText, "fetchText");
async function fetchWithTimeout(url, options = {}, timeout = EXTRACT_TIMEOUT_MS) {
  const ac = new AbortController();
  const timer = setTimeout(() => ac.abort(), timeout);
  try {
    const res = await fetch(url, { ...options, signal: options.signal || ac.signal });
    storeCookies(res.headers);
    return res;
  } finally {
    clearTimeout(timer);
  }
}
__name(fetchWithTimeout, "fetchWithTimeout");
__name4(fetchWithTimeout, "fetchWithTimeout");
async function discoverCryptoConfig(force = false) {
  if (!force && cryptoConfigCache?.expiresAt && Date.now() < cryptoConfigCache.expiresAt) return cryptoConfigCache;
  try {
    const html2 = await fetchText(`${REFERER}/`, { Accept: "text/html,*/*" });
    const appUrl = html2.match(/(?:import\(|src=)["']([^"']+\/_app\/immutable\/entry\/app\.[^"']+\.js)["']/)?.[1];
    if (!appUrl) throw new Error("MKissa app entry not found");
    const app = await fetchText(appUrl, { Accept: "application/javascript,*/*" });
    const queue = [appUrl];
    const seen = /* @__PURE__ */ new Set();
    while (queue.length && seen.size < DISCOVERY_LIMIT) {
      const batch = queue.splice(0, DISCOVERY_CONCURRENCY).filter((url) => {
        if (seen.has(url)) return false;
        seen.add(url);
        return true;
      });
      const chunks = await Promise.all(batch.map(async (url) => {
        try {
          return { url, text: url === appUrl ? app : await fetchText(url, { Accept: "application/javascript,*/*" }) };
        } catch {
          return null;
        }
      }));
      for (const item of chunks.filter(Boolean)) {
        const imported = [
          ...item.text.matchAll(/(?:import\(|from\s*)["']([^"']+\.js)["']/g),
          ...item.text.matchAll(/"(\.\.\/(?:chunks|nodes)\/[^"\n]+\.js)"/g)
        ].map((m) => m[1]).filter((value) => value.startsWith(".") || value.startsWith("/"));
        for (const value of imported) {
          const next = new URL(value, item.url).toString();
          if (!seen.has(next)) queue.push(next);
        }
        if (!/client-crypto|x-aa-boot|aaReq|partB/.test(item.text)) continue;
        const config = evalCryptoChunk(item.text);
        if (config) {
          cryptoConfigCache = { ...config, sourceUrl: item.url, expiresAt: Date.now() + 18e5 };
          return cryptoConfigCache;
        }
      }
    }
    throw new Error("MKissa crypto chunk not found");
  } catch (error) {
    cryptoConfigCache = null;
    throw error;
  }
}
__name(discoverCryptoConfig, "discoverCryptoConfig");
__name4(discoverCryptoConfig, "discoverCryptoConfig");
async function discoverEpisodeQuery(force = false) {
  const config = await discoverCryptoConfig(force);
  if (!force && episodeQueryCache?.buildId === config.buildId && Date.now() < episodeQueryCache.expiresAt) return episodeQueryCache.query;
  const inspect = /* @__PURE__ */ __name((text) => {
    const query = evalEpisodeQueryChunk(text);
    if (query) {
      episodeQueryCache = { buildId: config.buildId, query, expiresAt: config.expiresAt };
      return query;
    }
    return null;
  }, "inspect");
  if (config.sourceUrl) {
    try {
      const query = inspect(await fetchText(config.sourceUrl, { Accept: "application/javascript,*/*" }));
      if (query) return query;
    } catch {
    }
  }
  const html2 = await fetchText(`${REFERER}/`, { Accept: "text/html,*/*" });
  const appUrl = html2.match(/(?:import\(|src=)["']([^"']+\/_app\/immutable\/entry\/app\.[^"']+\.js)["']/)?.[1];
  if (!appUrl) return episodeQuery();
  const queue = [appUrl];
  const seen = /* @__PURE__ */ new Set();
  while (queue.length && seen.size < DISCOVERY_LIMIT) {
    const batch = queue.splice(0, DISCOVERY_CONCURRENCY).filter((url) => {
      if (seen.has(url)) return false;
      seen.add(url);
      return true;
    });
    const chunks = await Promise.all(batch.map(async (url) => {
      try {
        return { url, text: await fetchText(url, { Accept: "application/javascript,*/*" }) };
      } catch {
        return null;
      }
    }));
    for (const item of chunks.filter(Boolean)) {
      const query = inspect(item.text);
      if (query) return query;
      const imported = [
        ...item.text.matchAll(/(?:import\(|from\s*)["']([^"']+\.js)["']/g),
        ...item.text.matchAll(/["'](\.\.\/(?:chunks|nodes)\/[^"'\n]+\.js)["']/g)
      ].map((match) => match[1]).filter((value) => value.startsWith(".") || value.startsWith("/"));
      for (const value of imported) {
        const next = new URL(value, item.url).toString();
        if (!seen.has(next)) queue.push(next);
      }
    }
  }
  return episodeQuery();
}
__name(discoverEpisodeQuery, "discoverEpisodeQuery");
__name4(discoverEpisodeQuery, "discoverEpisodeQuery");
function buildMaskSeed(buildId) {
  const n = String(buildId || "");
  const out = Buffer.alloc(32);
  for (let i = 0; i < 32; i++) {
    out[i] = (n.charCodeAt(i % n.length) || 0) ^ i * 17 + 31 & 255;
  }
  return out;
}
__name(buildMaskSeed, "buildMaskSeed");
__name4(buildMaskSeed, "buildMaskSeed");
function buildMask(config) {
  const { buildId, maskParts } = config;
  if (config.scheme === "fragments") {
    const salt = Buffer.alloc(32);
    const name = String(buildId || "");
    for (let i = 0; i < salt.length; i++) {
      salt[i] = (name.charCodeAt(i % name.length) || 0) ^ i * config.saltMul + config.saltAdd & 255;
    }
    const out2 = Buffer.alloc(32);
    for (let i = 0; i < maskParts.length; i++) {
      const part = Buffer.from(maskParts[i], "base64");
      const offset = i * 8;
      for (let j = 0; j < 8; j++) {
        out2[offset + j] = part[j] ^ salt[offset + j] ^ i * config.fragMul + j * config.fragAdd & 255;
      }
    }
    return out2;
  }
  const seed = buildMaskSeed(buildId);
  const out = Buffer.alloc(32);
  for (let i = 0; i < maskParts.length; i++) {
    const part = Buffer.from(maskParts[i], "base64");
    const offset = i * 8;
    for (let j = 0; j < 8; j++) {
      out[offset + j] = part[j] ^ seed[offset + j] ^ i * 41 + j * 7 & 255;
    }
  }
  return out;
}
__name(buildMask, "buildMask");
__name4(buildMask, "buildMask");
function currentEpochs(now = Date.now()) {
  const epoch = Math.floor(now / BOOT_EPOCH_MS);
  const previousGrace = now - epoch * BOOT_EPOCH_MS < BOOT_GRACE_MS && epoch > 0 ? epoch - 1 : epoch;
  return [.../* @__PURE__ */ new Set([previousGrace, epoch])];
}
__name(currentEpochs, "currentEpochs");
__name4(currentEpochs, "currentEpochs");
function makeBootToken(config, epoch, lane = CONTENT_LANE) {
  const mask = buildMask(config);
  const bootKey = hmacBytes(mask, `${config.bootPrefix ?? "aa-boot:"}${config.buildId}`);
  if (config.scheme !== "fragments") {
    return hmacBytes(bootKey, `${config.buildId}:${KEY_GROUP}:${REFERER_HOST}:${epoch}:${lane}`).toString("hex");
  }
  const fields = { buildId: config.buildId, group: KEY_GROUP, host: REFERER_HOST, epoch: String(epoch), lane: String(lane || "") };
  const parts = config.omitEmptyLane && !fields.lane ? config.parts.filter((part) => part !== "lane") : config.parts;
  return hmacBytes(bootKey, parts.map((part) => fields[part] ?? "").join(config.join)).toString("hex");
}
__name(makeBootToken, "makeBootToken");
__name4(makeBootToken, "makeBootToken");
async function fetchBootstrap(lane = CONTENT_LANE, force = false) {
  const config = await discoverCryptoConfig(force);
  if (!force && bootstrapCache?.lane === lane && bootstrapCache.buildId === config.buildId && bootstrapCache.switchAt && Date.now() < bootstrapCache.switchAt) {
    return bootstrapCache;
  }
  let lastError = null;
  for (const epoch of currentEpochs()) {
    const res = await sessionFetch(`${API}/client-crypto/v1/bootstrap?buildId=${encodeURIComponent(config.buildId)}&k=${encodeURIComponent(lane)}`, {
      headers: {
        "Referer": `${REFERER}/`,
        "Origin": REFERER,
        "x-build-id": config.buildId,
        "x-aa-boot": makeBootToken(config, epoch, lane)
      }
    });
    const raw = await res.text();
    if (!res.ok) {
      lastError = new Error(`Bootstrap ${res.status}: ${raw.slice(0, 180)}`);
      continue;
    }
    const data = JSON.parse(raw);
    if (!data?.partB) {
      lastError = new Error("Bootstrap missing partB");
      continue;
    }
    bootstrapCache = { ...data, ...config, lane, buildId: config.buildId };
    return bootstrapCache;
  }
  throw lastError || new Error("MKissa bootstrap failed");
}
__name(fetchBootstrap, "fetchBootstrap");
__name4(fetchBootstrap, "fetchBootstrap");
function deriveLaneKey(partB, config) {
  const encrypted = Buffer.from(partB, "base64");
  const mask = buildMask(config);
  const key = Buffer.alloc(32);
  for (let i = 0; i < 32; i++) {
    key[i] = encrypted[i] ^ mask[i % mask.length];
  }
  return key;
}
__name(deriveLaneKey, "deriveLaneKey");
__name4(deriveLaneKey, "deriveLaneKey");
async function getLaneKey(lane = CONTENT_LANE, force = false) {
  const boot = await fetchBootstrap(lane, force);
  return { key: deriveLaneKey(boot.partB, boot), epoch: boot.epoch, buildId: boot.buildId };
}
__name(getLaneKey, "getLaneKey");
__name4(getLaneKey, "getLaneKey");
function makeAaReq(key, epoch, buildId, queryHash, lane = CONTENT_LANE) {
  const ts = Math.floor(Date.now() / AA_REQ_MS) * AA_REQ_MS;
  const payload = Buffer.from(JSON.stringify({ v: 1, ts, epoch, buildId, qh: queryHash, k: lane }));
  const iv = crypto.createHash("sha256").update(`${epoch}:${buildId}:${queryHash}:${ts}:${lane}`).digest().subarray(0, 12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const body = Buffer.concat([cipher.update(payload), cipher.final(), cipher.getAuthTag()]);
  return Buffer.concat([Buffer.from([1]), iv, body]).toString("base64");
}
__name(makeAaReq, "makeAaReq");
__name4(makeAaReq, "makeAaReq");
function decryptTobeparsed(b64, key) {
  const buf = Buffer.from(b64, "base64");
  const version = buf[0];
  if (version !== 1) throw new Error(`Unsupported MKissa encryption version: ${version}`);
  const iv = buf.subarray(1, 13);
  const body = buf.subarray(13);
  const ct = body.subarray(0, body.length - 16);
  const tag = body.subarray(body.length - 16);
  const decipher = crypto.createDecipheriv("aes-256-gcm", key, iv);
  decipher.setAuthTag(tag);
  return JSON.parse(Buffer.concat([decipher.update(ct), decipher.final()]).toString("utf8"));
}
__name(decryptTobeparsed, "decryptTobeparsed");
__name4(decryptTobeparsed, "decryptTobeparsed");
function episodeQuery() {
  const zt = `
tbObj {
  u
  sm
  md
  ts
}
`;
  const pu = `
_id
name
englishName
nativeName
slugTime
`;
  const xa = `
${pu}
thumbnail
${zt}
lastEpisodeInfo
lastEpisodeDate
type
season
score
airedStart
availableEpisodes
episodeDuration
episodeCount
lastUpdateEnd
characterCount
`;
  const ef = `
  _id
  username
  displayName
  createdAt
  picture
  reputation
  roleLevel

  
  brief
  followerCount
  followingCount
  pDec
  equippedBadgeKey
  equippedBadge {
    key
    name
    rank
    iconPath
    date
  }
  ugcContributorStats {
    mediaEditReviewSubmitCount
    mediaEditApprovedCount
    mediaEditRejectedCount
    mediaEditAppliedCount
    mediaEditContributionPoints
    mediaEditModContributionPoints
  }

  hideMe
`;
  const fr = `
views
likesCount
commentCount
dislikesCount
boostsCount
reviewCount
userScoreCount
userScoreTotalValue
userScoreAverValue
viewers{
firstViewers{
viewCount
lastWatchedDate
user{
${ef}
}
}
recViewers{
viewCount
lastWatchedDate
user{
${ef}
}
}
}
`;
  return `
query(
$showId: String!
$translationType: VaildTranslationTypeEnumType!
$episodeString: String!
) {
episode(
showId: $showId
translationType: $translationType
episodeString: $episodeString
) {
episodeString
uploadDate
sourceUrls
thumbnail
notes
show{
${xa}
description
broadcastInterval
banner
characters
availableEpisodesDetail
nameOnlyString
characters
isAdult
relatedShows
relatedMangas
altNames
disqusIds
}
pageStatus{
_id
notes
pageId
showId
${fr}
}
episodeInfo{
notes
thumbnails
${zt}
vidInforssub
uploadDates
vidInforsdub
vidInforsraw
description
}
versionFix
}
}
`;
}
__name(episodeQuery, "episodeQuery");
__name4(episodeQuery, "episodeQuery");
async function apiPost(query, variables, options = {}) {
  const config = options.buildId ? options : await discoverCryptoConfig();
  const body = options.extensions ? { query, variables, extensions: options.extensions } : { query, variables };
  const res = await sessionFetch(API_URL, {
    method: "POST",
    headers: apiHeaders(config.buildId, {
      "Content-Type": "application/json"
    }),
    body: JSON.stringify(body)
  });
  const raw = await res.text();
  if (!res.ok) {
    const err = new Error(`API POST ${res.status}`);
    err.rawBody = raw;
    throw err;
  }
  const json5 = JSON.parse(raw);
  if (json5.errors?.length) {
    const messages = json5.errors.map((e) => e.message || e.extensions?.code || "GraphQL error");
    const err = new Error(messages.join(" \xB7 "));
    if (messages.includes("NEED_CAPTCHA")) err.code = "NEED_CAPTCHA";
    err.rawBody = raw;
    err.graphql = json5;
    throw err;
  }
  return json5.data;
}
__name(apiPost, "apiPost");
__name4(apiPost, "apiPost");
async function apiEpisode(query, variables, options = {}) {
  const { force = false, captchaRetry = 0, captcha = null, postFallback = false } = options;
  const hash2 = sha256Hex(query);
  const { key, epoch, buildId } = await getLaneKey(CONTENT_LANE, force);
  const extensions = {
    persistedQuery: { version: 1, sha256Hash: hash2 },
    k: CONTENT_LANE,
    aaReq: makeAaReq(key, epoch, buildId, hash2, CONTENT_LANE)
  };
  if (captcha) extensions.captcha = captcha;
  if (captcha) {
    const posted = await apiPost(query, variables, { buildId, extensions });
    return posted?.tobeparsed ? decryptTobeparsed(posted.tobeparsed, key) : posted;
  }
  const url = `${API_URL}?variables=${encodeURIComponent(JSON.stringify(variables))}&extensions=${encodeURIComponent(JSON.stringify(extensions))}`;
  const res = await sessionFetch(url, {
    headers: apiHeaders(buildId)
  });
  const raw = await res.text();
  if (!res.ok) {
    const err = new Error(`API ${res.status}`);
    err.rawBody = raw;
    throw err;
  }
  const json5 = JSON.parse(raw);
  const messages = json5.errors?.map((e) => e.message || e.extensions?.code).filter(Boolean) || [];
  if (messages.includes("PersistedQueryNotFound") || messages.some((m) => /Context creation failed/i.test(m))) {
    const posted = await apiPost(query, variables, { buildId, extensions });
    return posted?.tobeparsed ? decryptTobeparsed(posted.tobeparsed, key) : posted;
  }
  if (messages.includes("NEED_CAPTCHA")) {
    if (!postFallback) {
      try {
        const postHash = sha256Hex(query);
        const postExtensions = {
          persistedQuery: { version: 1, sha256Hash: postHash },
          k: CONTENT_LANE,
          aaReq: makeAaReq(key, epoch, buildId, postHash, CONTENT_LANE)
        };
        const posted = await apiPost(query, variables, { buildId, extensions: postExtensions });
        return posted?.tobeparsed ? decryptTobeparsed(posted.tobeparsed, key) : posted;
      } catch (err2) {
        if (err2.code !== "NEED_CAPTCHA") throw err2;
      }
    }
    if (captchaRetry < 5) {
      await sleep(1500 + captchaRetry * 1200);
      return apiEpisode(query, variables, { force: true, captchaRetry: captchaRetry + 1, postFallback: true });
    }
    const err = new Error("MKissa requested captcha");
    err.code = "NEED_CAPTCHA";
    err.rawBody = raw;
    throw err;
  }
  if (messages.some((m) => /^AA_CRYPTO_/.test(m))) {
    if (!force) return apiEpisode(query, variables, { force: true, captchaRetry, captcha, postFallback });
    const err = new Error(messages.join(" \xB7 "));
    err.rawBody = raw;
    throw err;
  }
  if (json5.data?.tobeparsed) {
    return decryptTobeparsed(json5.data.tobeparsed, key);
  }
  if (messages.length) {
    const err = new Error(messages.join(" \xB7 "));
    err.rawBody = raw;
    throw err;
  }
  return json5.data;
}
__name(apiEpisode, "apiEpisode");
__name4(apiEpisode, "apiEpisode");
async function searchMkissa(query, mode = "sub") {
  const gql = `query($search:SearchInput $limit:Int $page:Int $translationType:VaildTranslationTypeEnumType $countryOrigin:VaildCountryOriginEnumType){shows(search:$search limit:$limit page:$page translationType:$translationType countryOrigin:$countryOrigin){edges{_id name englishName nativeName slugTime availableEpisodes availableEpisodesDetail aniListId __typename}}}`;
  const data = await apiPost(gql, {
    search: { allowAdult: false, allowUnknown: false, query },
    limit: 40,
    page: 1,
    translationType: mode,
    countryOrigin: "ALL"
  });
  return data?.shows?.edges ?? [];
}
__name(searchMkissa, "searchMkissa");
__name4(searchMkissa, "searchMkissa");
async function getEpisodeSources(showId, epNum, audio = "sub", captcha = null) {
  const query = await discoverEpisodeQuery();
  const data = await apiEpisode(query, { showId, translationType: audio, episodeString: String(epNum) }, { captcha });
  return data?.episode ?? null;
}
__name(getEpisodeSources, "getEpisodeSources");
__name4(getEpisodeSources, "getEpisodeSources");
function slugifyTitle(value) {
  return String(value || "").normalize("NFKD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
}
__name(slugifyTitle, "slugifyTitle");
__name4(slugifyTitle, "slugifyTitle");
async function warmWatchPage(showId, show, epNum, audio) {
  const slug = show?.slugTime || slugifyTitle(show?.englishName || show?.name || show?.nativeName);
  if (!slug || !showId) return;
  const page = `${REFERER}/anime/${slug}-${showId}/${audio}/${epNum}`;
  try {
    await fetchWithTimeout(page, {
      headers: browserHeaders({
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Referer": `${REFERER}/`,
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "same-origin",
        "Sec-Fetch-User": "?1",
        "Upgrade-Insecure-Requests": "1"
      })
    }, FETCH_TIMEOUT_MS);
  } catch {
  }
}
__name(warmWatchPage, "warmWatchPage");
__name4(warmWatchPage, "warmWatchPage");
async function fetchAniZip(anilistId) {
  const res = await fetch(`${ANIZIP}?anilist_id=${anilistId}`);
  if (!res.ok) return null;
  return res.json();
}
__name(fetchAniZip, "fetchAniZip");
__name4(fetchAniZip, "fetchAniZip");
function normalize(s) {
  return (s || "").toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");
}
__name(normalize, "normalize");
__name4(normalize, "normalize");
function extractYear(title) {
  if (!title) return null;
  const m = title.match(/\b(19\d{2}|20\d{2})\b/);
  return m ? parseInt(m[1]) : null;
}
__name(extractYear, "extractYear");
__name4(extractYear, "extractYear");
function findBestMatch(results, titles, targetYear, targetId) {
  const normalizedTitles = titles.map(normalize).filter(Boolean);
  let bestShow = null;
  let maxScore = -Infinity;
  for (const r of results) {
    if (targetId && r.aniListId && String(r.aniListId) === String(targetId)) return r;
    const names = [r.name, r.englishName, r.nativeName].map(normalize).filter(Boolean);
    let nameScore = 0;
    let isExact = false;
    for (const n of names) {
      if (normalizedTitles.includes(n)) {
        nameScore = 100;
        isExact = true;
        break;
      }
    }
    if (!isExact) {
      let maxFuzzy = 0;
      for (const rName of names) {
        for (const t of normalizedTitles) {
          if (t.includes(rName) || rName.includes(t)) {
            const score = Math.min(rName.length, t.length);
            const lengthPenalty = Math.abs(rName.length - t.length) * 0.1;
            maxFuzzy = Math.max(maxFuzzy, score - lengthPenalty);
          }
        }
      }
      nameScore = maxFuzzy;
    }
    let yearScore = 0;
    const rYear = extractYear(r.name) || extractYear(r.englishName) || extractYear(r.nativeName);
    if (targetYear && rYear) yearScore = rYear === targetYear ? 50 : -200;
    const totalScore = nameScore + yearScore;
    if (totalScore > maxScore) {
      maxScore = totalScore;
      bestShow = r;
    }
  }
  return bestShow || results[0];
}
__name(findBestMatch, "findBestMatch");
__name4(findBestMatch, "findBestMatch");
async function fetchAniListMedia(anilistId) {
  try {
    const q = "query ($id: Int) { Media (id: $id, type: ANIME) { seasonYear startDate { year } title { romaji english native } } }";
    const res = await fetch("https://graphql.anilist.co", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json", "User-Agent": UA4, "Origin": "https://anilist.co" },
      body: JSON.stringify({ query: q, variables: { id: Number(anilistId) } })
    });
    if (!res.ok) return null;
    const json5 = await res.json();
    return json5.data?.Media ?? null;
  } catch {
    return null;
  }
}
__name(fetchAniListMedia, "fetchAniListMedia");
__name4(fetchAniListMedia, "fetchAniListMedia");
async function resolveMkissaId(anilistId, ctx = {}) {
  const [anizipRes, alMedia] = await Promise.all([
    ctx.anizip ? Promise.resolve(ctx.anizip) : fetchAniZip(anilistId).catch(() => ({})),
    ctx.media ? Promise.resolve({ title: ctx.media.title, seasonYear: ctx.media.seasonYear, startDate: ctx.media.startDate }) : fetchAniListMedia(anilistId).catch(() => null)
  ]);
  const anizip = anizipRes || {};
  let titlesToTry = [];
  if (anizip.titles) {
    titlesToTry = [
      anizip.titles.en,
      anizip.titles.ja,
      anizip.titles["x-jat"],
      ...Object.values(anizip.titles)
    ].filter(Boolean);
  }
  if (alMedia?.title) {
    const alTitles = [alMedia.title.english, alMedia.title.romaji, alMedia.title.native].filter(Boolean);
    titlesToTry = [.../* @__PURE__ */ new Set([...alTitles, ...titlesToTry])];
  }
  if (!titlesToTry.length && anizip.mappings) {
    const apId = anizip.mappings.animeplanet_id;
    if (apId) titlesToTry = [apId.split(/[-_]/).map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(" ")];
  }
  if (!titlesToTry.length) throw new Error(`Could not resolve titles for AniList ID: ${anilistId}`);
  const targetYear = alMedia?.seasonYear || alMedia?.startDate?.year || null;
  let allResults = [];
  for (const title of titlesToTry.slice(0, 3)) {
    allResults.push(...await searchMkissa(title, "sub"));
  }
  const seen = /* @__PURE__ */ new Set();
  allResults = allResults.filter((r) => {
    if (seen.has(r._id)) return false;
    seen.add(r._id);
    return true;
  });
  if (!allResults.length) throw new Error(`No MKissa match for "${titlesToTry[0]}"`);
  const match = findBestMatch(allResults, titlesToTry, targetYear, anilistId);
  return { showId: match._id, show: match, anizip };
}
__name(resolveMkissaId, "resolveMkissaId");
__name4(resolveMkissaId, "resolveMkissaId");
function hexToBytes(hex) {
  const clean = hex.replace(/[^0-9a-f]/gi, "");
  const out = new Uint8Array(clean.length / 2);
  for (let i = 0; i < out.length; i++) out[i] = parseInt(clean.slice(i * 2, i * 2 + 2), 16);
  return out;
}
__name(hexToBytes, "hexToBytes");
__name4(hexToBytes, "hexToBytes");
async function aesDecrypt(hex) {
  const decipher = crypto.createDecipheriv("aes-128-cbc", Buffer.from("kiemtienmua911ca"), Buffer.from("1234567890oiuytr"));
  return Buffer.concat([decipher.update(Buffer.from(hexToBytes(hex))), decipher.final()]).toString("utf8");
}
__name(aesDecrypt, "aesDecrypt");
__name4(aesDecrypt, "aesDecrypt");
async function extractMp4(id) {
  try {
    const r = await fetchWithTimeout(`https://www.mp4upload.com/embed-${id}.html`, {
      headers: { "User-Agent": UA4, Referer: "https://mp4upload.com/" }
    });
    if (!r.ok) return null;
    const h = await r.text();
    const m = h.match(/player\.src\s*\(\s*\{[^}]*\bsrc\s*:\s*"([^"]+)"/) || h.match(/"file"\s*:\s*"(https?:[^"]+\.mp4[^"]*)"/) || h.match(/\bsrc\s*:\s*"(https?:[^"]+\.mp4[^"]*)"/);
    return m?.[1]?.replace(/\\/g, "") || null;
  } catch {
    return null;
  }
}
__name(extractMp4, "extractMp4");
__name4(extractMp4, "extractMp4");
async function extractUns(url) {
  try {
    const parsed = new URL(url);
    const id = parsed.hash.replace(/^#/, "").split("&")[0];
    if (!id) return null;
    const base = `${parsed.protocol}//${parsed.host}`;
    const r = await fetchWithTimeout(`${base}/api/v1/video?id=${encodeURIComponent(id)}&w=1280&h=720&r=`, {
      headers: { "User-Agent": UA4, Referer: `${base}/#${id}`, Origin: base }
    });
    if (!r.ok) return null;
    const hex = (await r.text()).trim();
    if (!hex || !/^[0-9a-f]+$/i.test(hex)) return null;
    const data = JSON.parse(await aesDecrypt(hex));
    return data?.source || data?.cf || null;
  } catch {
    return null;
  }
}
__name(extractUns, "extractUns");
__name4(extractUns, "extractUns");
async function extractOk(id) {
  try {
    const r = await fetchWithTimeout(`https://ok.ru/videoembed/${id}`, {
      headers: { "User-Agent": UA4, Referer: "https://ok.ru/" }
    });
    if (!r.ok) return null;
    const h = await r.text();
    const m = h.match(/ondemandHls\\&quot;:\\&quot;(https?:\/\/.*?)\\&quot;/);
    return m?.[1]?.replace(/\\u0026/g, "&") || null;
  } catch {
    return null;
  }
}
__name(extractOk, "extractOk");
__name4(extractOk, "extractOk");
async function extractStreamSB(id) {
  try {
    const baseHeaders = {
      "User-Agent": UA4,
      "Referer": `${REFERER}/`,
      "watchsb": "streamsb",
      "Accept": "application/json, text/plain, */*",
      "Accept-Language": "en-US,en;q=0.9"
    };
    const r1 = await fetchWithTimeout(`https://streamsb.net/api/v1/video?id=${id}`, { headers: baseHeaders });
    const sid = (r1.headers.get("set-cookie") || "").match(/sid=([^;]+)/)?.[1] ?? "";
    const html2 = await r1.text();
    const m = html2.match(/window\.location\.replace\('([^']+)'\)/);
    if (!m) return null;
    const r2 = await fetchWithTimeout(m[1], { headers: { ...baseHeaders, Cookie: `sid=${sid}`, Referer: `https://streamsb.net/e/${id}.html` } });
    if (!r2.ok) return null;
    const ct = r2.headers.get("content-type") ?? "";
    if (!ct.includes("json")) return null;
    const data = await r2.json();
    return data?.stream_data?.file ?? data?.data?.file ?? null;
  } catch {
    return null;
  }
}
__name(extractStreamSB, "extractStreamSB");
__name4(extractStreamSB, "extractStreamSB");
async function extractStreamlare(id) {
  try {
    const r = await fetchWithTimeout("https://streamlare.com/api/video/stream/get", {
      method: "POST",
      headers: { "Content-Type": "application/json", "User-Agent": UA4, "Referer": "https://streamlare.com/", "Origin": "https://streamlare.com", "Accept": "application/json, */*" },
      body: JSON.stringify({ id })
    });
    if (!r.ok) return null;
    const data = await r.json();
    return data?.data?.file ?? null;
  } catch {
    return null;
  }
}
__name(extractStreamlare, "extractStreamlare");
__name4(extractStreamlare, "extractStreamlare");
async function extractClock(url) {
  try {
    const parsed = new URL(url);
    const clockUrl = parsed.pathname.includes("/clock.json") ? url : url.replace("/clock", "/clock.json");
    const r = await fetchWithTimeout(clockUrl, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36",
        "Referer": "https://allanime.day/player.html",
        "Accept": "*/*",
        "Accept-Language": "en-US,en;q=0.9",
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "same-origin"
      }
    });
    if (!r.ok) return null;
    const data = await r.json();
    const links = Array.isArray(data?.links) ? data.links : [];
    const best = links.find((item) => item?.hls && item?.link) || links.find((item) => item?.link);
    return best?.link || null;
  } catch {
    return null;
  }
}
__name(extractClock, "extractClock");
__name4(extractClock, "extractClock");
function embedMediaType(url) {
  if (!url) return null;
  if (url.includes(".m3u8")) return "hls";
  if (url.includes(".mp4")) return "mp4";
  return "direct";
}
__name(embedMediaType, "embedMediaType");
__name4(embedMediaType, "embedMediaType");
async function extractSource(src) {
  let url = src.sourceUrl;
  if (url && url.startsWith("--")) url = decodeHexUrl(url.slice(2));
  if (url && url.startsWith("/apivtwo/clock")) url = "https://allanime.day" + url.replace("/clock", "/clock.json");
  if (url && /^https?:\/\/allanime\.day\/apivtwo\/clock(?:\.json)?/i.test(url)) url = url.replace("/clock?", "/clock.json?");
  let extractedUrl = null;
  try {
    const host = new URL(url).hostname.replace(/^www\./, "");
    if (host === "allanime.day" && /\/apivtwo\/clock(?:\.json)?/i.test(new URL(url).pathname)) {
      extractedUrl = await extractClock(url);
    } else if (src.type === "player") extractedUrl = url;
    else if (host === "mp4upload.com") {
      const m = url.match(/embed-([a-zA-Z0-9]+)\.html/i);
      if (m?.[1]) extractedUrl = await extractMp4(m[1]);
    } else if (/uns\.bio$/i.test(host)) {
      extractedUrl = await extractUns(url);
    } else if (host === "ok.ru") {
      const m = url.match(/\/(?:videoembed\/)?(\d+)(?:[/?#]|$)/i);
      if (m?.[1]) extractedUrl = await extractOk(m[1]);
    } else if (/streamsb\./i.test(host)) {
      const m = url.match(/\/(?:e\/|embed-)([a-zA-Z0-9]+)(?:\.html)?/i);
      if (m?.[1]) extractedUrl = await extractStreamSB(m[1]);
    } else if (/streamlare\./i.test(host)) {
      const m = url.match(/\/e\/([a-zA-Z0-9]+)/i);
      if (m?.[1]) extractedUrl = await extractStreamlare(m[1]);
    }
  } catch {
  }
  return {
    name: src.sourceName || "",
    url,
    extractedUrl,
    extractedType: embedMediaType(extractedUrl),
    type: src.type,
    priority: src.priority,
    headers: {
      "Referer": REFERER,
      "User-Agent": UA4
    },
    downloads: src.downloads || null
  };
}
__name(extractSource, "extractSource");
__name4(extractSource, "extractSource");
async function fetchAniListFull(anilistId) {
  const q = `
  query ($id: Int) {
    Media(id: $id, type: ANIME) {
      id
      idMal
      title { romaji english native }
      synonyms
      format
      episodes
      seasonYear
      startDate { year }
      type
      relations {
        edges { relationType(version: 2) node { id type format title { romaji english native } } }
      }
    }
  }`;
  const res = await fetch("https://graphql.anilist.co", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json", "User-Agent": UA4, "Origin": "https://anilist.co" },
    body: JSON.stringify({ query: q, variables: { id: Number(anilistId) } })
  });
  if (!res.ok) throw new Error("AniList fetch failed");
  const json5 = await res.json();
  return json5.data?.Media;
}
__name(fetchAniListFull, "fetchAniListFull");
__name4(fetchAniListFull, "fetchAniListFull");
async function fetchKitsuId(malId) {
  if (!malId) return null;
  try {
    const res = await fetch(`https://kitsu.io/api/edge/mappings?filter[externalSite]=myanimelist/anime&filter[externalId]=${malId}`);
    const json5 = await res.json();
    const mapping = json5.data?.[0];
    if (mapping?.relationships?.item?.links?.related) {
      const itemRes = await fetch(mapping.relationships.item.links.related);
      const itemJson = await itemRes.json();
      return itemJson.data?.id ? Number(itemJson.data.id) : null;
    }
  } catch {
  }
  return null;
}
__name(fetchKitsuId, "fetchKitsuId");
__name4(fetchKitsuId, "fetchKitsuId");
async function fetchTMDB(titles, year, format) {
  const tmdbType = format === "MOVIE" || format === "OVA" || format === "SPECIAL" ? "movie" : "tv";
  let result = null;
  for (const title of titles) {
    if (!title) continue;
    try {
      const searchUrl = `https://api.themoviedb.org/3/search/${tmdbType}?query=${encodeURIComponent(title)}&first_air_date_year=${year}&year=${year}`;
      const res = await fetch(searchUrl, { headers: { Authorization: `Bearer ${TMDB_TOKEN}`, Accept: "application/json" } });
      const json5 = await res.json();
      if (json5.results?.length) {
        result = json5.results[0];
        break;
      }
    } catch {
    }
  }
  if (!result) return { themoviedbId: null, imdbId: null, thetvdbId: null };
  try {
    const extUrl = `https://api.themoviedb.org/3/${tmdbType}/${result.id}/external_ids`;
    const extRes = await fetch(extUrl, { headers: { Authorization: `Bearer ${TMDB_TOKEN}`, Accept: "application/json" } });
    const externalIds = await extRes.json();
    return {
      themoviedbId: result.id,
      imdbId: externalIds.imdb_id || null,
      thetvdbId: externalIds.tvdb_id || null
    };
  } catch {
    return { themoviedbId: result.id, imdbId: null, thetvdbId: null };
  }
}
__name(fetchTMDB, "fetchTMDB");
__name4(fetchTMDB, "fetchTMDB");
async function handleMap(anilistId) {
  const al = await fetchAniListFull(anilistId);
  if (!al) throw new Error("AniList entry not found");
  const year = al.seasonYear || al.startDate?.year;
  const titlesToSearch = [al.title.english, al.title.romaji, al.title.native].filter(Boolean);
  const [kitsuId, tmdbData] = await Promise.all([
    fetchKitsuId(al.idMal),
    fetchTMDB(titlesToSearch, year, al.format)
  ]);
  return {
    mappings: {
      id: Number(anilistId),
      title: al.title.english || al.title.romaji,
      type: al.type,
      format: al.format,
      episodes: al.episodes,
      malId: al.idMal,
      aniId: Number(anilistId),
      anidbId: null,
      animePlanetId: null,
      kitsuId,
      imdbId: tmdbData.imdbId,
      themoviedbId: tmdbData.themoviedbId,
      thetvdbId: tmdbData.thetvdbId,
      livechartId: null,
      annId: null,
      synonyms: al.synonyms || [],
      franchise: al.relations?.edges?.map((e) => ({
        relation: e.relationType,
        id: e.node.id,
        title: e.node.title.romaji || e.node.title.english,
        type: e.node.type,
        format: e.node.format
      })) || []
    }
  };
}
__name(handleMap, "handleMap");
__name4(handleMap, "handleMap");
async function handleEpisodes(anilistId, ctx = {}) {
  const { showId, show, anizip } = await resolveMkissaId(anilistId, ctx);
  const epDetail = show.availableEpisodesDetail || {};
  const subEps = (epDetail.sub || []).map(Number).sort((a, b) => a - b);
  const dubEps = (epDetail.dub || []).map(Number).sort((a, b) => a - b);
  const buildList = /* @__PURE__ */ __name((nums, audio) => nums.map((n) => {
    const meta = anizip.episodes?.[String(n)] ?? {};
    return {
      id: `watch/mkissa/${anilistId}/${audio}/mkissa-${n}`,
      number: n,
      title: meta.title?.en || meta.title?.["x-jat"] || null,
      duration: meta.runtime ?? meta.length ?? 0,
      audio,
      filler: meta.filler ?? false,
      uncensored: false,
      description: meta.overview || meta.summary || null,
      image: meta.image || anizip.images?.cover || null,
      airDate: meta.airdate || meta.aired || null
    };
  }), "buildList");
  return {
    meta: {
      id: showId,
      title: show.englishName || show.name
    },
    episodes: {
      sub: buildList(subEps, "sub"),
      dub: buildList(dubEps, "dub"),
      raw: []
    }
  };
}
__name(handleEpisodes, "handleEpisodes");
__name4(handleEpisodes, "handleEpisodes");
async function handleEpisodesRoute(anilistId) {
  const { showId, show, anizip } = await resolveMkissaId(anilistId);
  const epDetail = show.availableEpisodesDetail || {};
  const subEps = (epDetail.sub || []).map(Number).sort((a, b) => a - b);
  const dubEps = (epDetail.dub || []).map(Number).sort((a, b) => a - b);
  const buildEpList = /* @__PURE__ */ __name((nums, audio) => nums.map((n) => {
    const meta = anizip.episodes?.[String(n)] ?? {};
    return {
      id: `watch/mkissa/${anilistId}/${audio}/mkissa-${n}`,
      number: n,
      title: meta.title?.en || meta.title?.["x-jat"] || `Episode ${n}`,
      duration: meta.runtime ?? meta.length ?? 0,
      audio,
      filler: meta.filler ?? false,
      uncensored: false,
      description: meta.overview || meta.summary || "",
      image: meta.image || anizip.images?.cover || "",
      airDate: meta.airdate || meta.aired || ""
    };
  }), "buildEpList");
  return {
    anilistId: Number(anilistId),
    mkissaId: showId,
    title: show.englishName || show.name,
    sub: buildEpList(subEps, "sub"),
    dub: buildEpList(dubEps, "dub")
  };
}
__name(handleEpisodesRoute, "handleEpisodesRoute");
__name4(handleEpisodesRoute, "handleEpisodesRoute");
function readCaptcha(request2, url) {
  const token = url.searchParams.get("captchaToken") || url.searchParams.get("turnstileToken") || request2.headers.get("x-captcha-token") || request2.headers.get("cf-turnstile-response");
  const provider = url.searchParams.get("captchaProvider") || request2.headers.get("x-captcha-provider") || "turnstile1";
  return token ? { token, provider } : null;
}
__name(readCaptcha, "readCaptcha");
__name4(readCaptcha, "readCaptcha");
function html(body, status = 200) {
  return new Response(body, {
    status,
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "no-store"
    }
  });
}
__name(html, "html");
__name4(html, "html");
function handleCaptchaPage(url) {
  const next = url.searchParams.get("next") || "";
  const safeNext = next.startsWith("/watch/mkissa/") ? next : "";
  const endpoint = `${API.replace(/\/$/, "")}/captcha/turnstile`;
  return html(`<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>MKissa Security Check</title>
<style>
body{margin:0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#101114;color:#f5f5f5;display:grid;place-items:center;min-height:100vh}
main{width:min(720px,calc(100vw - 32px))}
h1{font-size:20px;font-weight:650;margin:0 0 14px}
#captcha-root{min-height:160px}
iframe{width:100%;min-height:180px;border:0;border-radius:8px;background:white}
pre{white-space:pre-wrap;word-break:break-word;background:#17191f;border:1px solid #2a2d36;border-radius:8px;padding:14px;max-height:48vh;overflow:auto}
button{border:0;border-radius:6px;padding:10px 14px;background:#f5f5f5;color:#111;font-weight:650;cursor:pointer}
</style>
</head>
<body>
<main>
<h1>MKissa Security Check</h1>
<div id="captcha-root"></div>
<pre id="out">Waiting for captcha...</pre>
</main>
<script>
const next=${JSON.stringify(safeNext)};
const endpoint=${JSON.stringify(endpoint)};
const out=document.getElementById("out");
const root=document.getElementById("captcha-root");
function show(value){out.textContent=typeof value==="string"?value:JSON.stringify(value,null,2)}
function run(token,provider){
  if(!next){show({error:"Missing next watch path"});return}
  const u=new URL(next,location.origin);
  u.searchParams.set("captchaToken",token);
  u.searchParams.set("captchaProvider",provider||"turnstile");
  show("Captcha solved. Retrying watch request...");
  fetch(u).then(r=>r.text().then(t=>{try{show(JSON.parse(t))}catch{show(t)}})).catch(e=>show({error:String(e)}));
}
window.addEventListener("message",event=>{
  if(event.origin!==new URL(endpoint).origin)return;
  const data=event.data;
  if(!data||data.type!=="sitea-captcha-ready")return;
  if(data.error){show({error:data.error});return}
  if(!data.token){show({error:"Captcha token missing"});return}
  run(data.token,data.provider);
});
const iframe=document.createElement("iframe");
iframe.src=endpoint;
iframe.title="Security check";
iframe.loading="eager";
iframe.referrerPolicy="strict-origin-when-cross-origin";
iframe.onerror=()=>show({error:"Failed to load captcha frame"});
root.appendChild(iframe);
<\/script>
</body>
</html>`);
}
__name(handleCaptchaPage, "handleCaptchaPage");
__name4(handleCaptchaPage, "handleCaptchaPage");
async function handleWatch(anilistId, audio, epNum, captcha = null) {
  const cacheKey = `${anilistId}:${audio}:${epNum}`;
  const cached = watchMemoryCache.get(cacheKey);
  if (cached && Date.now() < cached.expiresAt && !captcha) return cached.data;
  const { showId, show, anizip } = await resolveMkissaId(anilistId);
  if (!captcha) await warmWatchPage(showId, show, epNum, audio);
  let episode;
  try {
    episode = await getEpisodeSources(showId, epNum, audio, captcha);
  } catch (err) {
    if (err.code === "NEED_CAPTCHA" && cached?.data) return cached.data;
    throw err;
  }
  if (!episode) throw new Error("Episode not found");
  const sources = await Promise.all((episode.sourceUrls || []).map(extractSource));
  sources.sort((a, b) => b.priority - a.priority);
  const epMeta = anizip?.episodes?.[String(epNum)] ?? {};
  const data = {
    anilistId: Number(anilistId),
    mkissaId: showId,
    episode: Number(epNum),
    audio,
    intro: epMeta.intro ?? null,
    outro: epMeta.outro ?? null,
    sources
  };
  watchMemoryCache.set(cacheKey, { data, expiresAt: Date.now() + WATCH_MEMORY_TTL });
  return data;
}
__name(handleWatch, "handleWatch");
__name4(handleWatch, "handleWatch");
function json(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=300"
    }
  });
}
__name(json, "json");
__name4(json, "json");
function matchRoute(pathname) {
  let m = pathname.match(/^\/episodes\/(\d+)\/?$/);
  if (m) return { handler: "episodes", anilistId: m[1] };
  m = pathname.match(/^\/watch\/mkissa\/(\d+)\/(sub|dub)\/mkissa-(\d+)\/?$/);
  if (m) return { handler: "watch", anilistId: m[1], audio: m[2], ep: m[3] };
  m = pathname.match(/^\/map\/(\d+)\/?$/);
  if (m) return { handler: "map", anilistId: m[1] };
  m = pathname.match(/^\/captcha\/mkissa\/?$/);
  if (m) return { handler: "captcha" };
  return null;
}
__name(matchRoute, "matchRoute");
__name4(matchRoute, "matchRoute");
var mkissaDefault = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const route = matchRoute(url.pathname);
    if (!route) {
      return json({
        error: "Not found",
        routes: [
          "GET /episodes/:anilistId",
          "GET /watch/mkissa/:anilistId/:audio/mkissa-:ep",
          "GET /captcha/mkissa?next=/watch/mkissa/:anilistId/:audio/mkissa-:ep",
          "GET /map/:anilistId"
        ]
      }, 404);
    }
    try {
      if (route.handler === "captcha") return handleCaptchaPage(url);
      if (route.handler === "map") return json(await handleMap(route.anilistId));
      if (route.handler === "episodes") return json(await handleEpisodesRoute(route.anilistId));
      if (route.handler === "watch") return json(await handleWatch(route.anilistId, route.audio, route.ep, readCaptcha(request2, url)));
    } catch (err) {
      const status = err.code === "NEED_CAPTCHA" ? 403 : 500;
      const solveUrl = route.handler === "watch" ? `/captcha/mkissa?next=${encodeURIComponent(url.pathname)}` : null;
      return json({ error: err.message, code: err.code ?? null, captcha: err.code === "NEED_CAPTCHA" ? { endpoint: `${API.replace(/\/$/, "")}/captcha/turnstile`, provider: "turnstile", tokenQuery: "captchaToken", tokenHeader: "x-captcha-token", solveUrl } : null, "Raw-ERROR": err.rawBody ?? null, stack: err.stack }, status);
    }
  }
};
var mkissa_default = mkissaDefault;

// extractors/byse.js
import { webcrypto as crypto2 } from "node:crypto";
var DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
var BLOCKS = 512;
var MASK = BLOCKS - 1;
var ROUNDS = 2;
var MUL_A = 2654435761;
var MUL_B = 2246822519;
function b64u(value) {
  return Buffer.from(value).toString("base64url");
}
__name(b64u, "b64u");
function b64uDec(value) {
  return Buffer.from(value, "base64url");
}
__name(b64uDec, "b64uDec");
function rot(value, shift) {
  return (value << shift | value >>> 32 - shift) >>> 0;
}
__name(rot, "rot");
function mul(value, factor) {
  return Math.imul(value, factor) >>> 0;
}
__name(mul, "mul");
function mix(state) {
  state[0] = state[0] + state[1] >>> 0;
  state[3] = rot(state[3] ^ state[0], 16);
  state[2] = state[2] + state[3] >>> 0;
  state[1] = rot(state[1] ^ state[2], 12);
  state[0] = state[0] + state[1] >>> 0;
  state[3] = rot(state[3] ^ state[0], 8);
  state[2] = state[2] + state[3] >>> 0;
  state[1] = rot(state[1] ^ state[2], 7);
}
__name(mix, "mix");
function hash(bytes) {
  const state = new Uint32Array([1779033703, 3144134277, 1013904242, 2773480762]);
  for (let i = 0; i < bytes.length; i++) {
    state[0] = state[0] + bytes[i] >>> 0;
    state[0] = rot(state[0], 7);
    mix(state);
  }
  for (let i = 0; i < 8; i++) mix(state);
  const table = new Uint32Array(BLOCKS);
  for (let i = 0; i < BLOCKS; i++) {
    mix(state);
    table[i] = (state[0] ^ state[2]) >>> 0;
  }
  for (let i = 0; i < ROUNDS; i++) {
    for (let index = 0; index < BLOCKS; index++) {
      const tableIndex = table[index] & MASK;
      let value = table[index] + table[tableIndex] >>> 0;
      value = rot(value, 13);
      value = (value ^ mul(table[index + 1 & MASK], MUL_A)) >>> 0;
      table[index] = value;
      state[0] = (state[0] ^ value) >>> 0;
      mix(state);
    }
  }
  const out = new Uint32Array(8);
  const width = BLOCKS / 8;
  for (let i = 0; i < 8; i++) {
    mix(state);
    let value = state[0];
    const offset = i * width;
    for (let index = 0; index < width; index++) {
      const tableValue = table[offset + index];
      value = value + tableValue >>> 0;
      value = rot(value, 5);
      value = (value ^ mul(tableValue, MUL_B)) >>> 0;
    }
    out[i] = (value ^ state[2]) >>> 0;
  }
  return out;
}
__name(hash, "hash");
function latin1Bytes(value) {
  const out = new Uint8Array(value.length);
  for (let i = 0; i < value.length; i++) out[i] = value.charCodeAt(i) & 255;
  return out;
}
__name(latin1Bytes, "latin1Bytes");
function leadingZeros(value) {
  let total = 0;
  for (let i = 0; i < value.length; i++) {
    const item = value[i];
    if (item === 0) {
      total += 32;
      continue;
    }
    return total + Math.clz32(item);
  }
  return total;
}
__name(leadingZeros, "leadingZeros");
function solvePoW(nonce, difficulty) {
  const prefix = `${nonce}:`;
  for (let counter = 0; ; counter++) {
    if (leadingZeros(hash(latin1Bytes(prefix + counter))) >= difficulty) return String(counter);
  }
}
__name(solvePoW, "solvePoW");
function canExtractByse(url) {
  return /(?:bysesayeveum\.com|gn1r5n\.org)\/e\//i.test(String(url));
}
__name(canExtractByse, "canExtractByse");
async function extractByse(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT, referer } = {}) {
  const code = String(embedUrl).match(/\/e\/([a-z0-9]+)/i)?.[1];
  if (!code) throw new Error(`Cannot extract Byse code from ${embedUrl}`);
  const embedOrigin = new URL(embedUrl).origin;
  const parentUrl = referer || embedUrl;
  const parentHost = new URL(parentUrl).hostname;
  const embedHeaders = {
    "X-Embed-Origin": parentHost,
    "X-Embed-Referer": parentUrl,
    "X-Embed-Parent": embedUrl
  };
  const detailsResponse = await fetchImpl(`${embedOrigin}/api/videos/${code}/embed/details`, {
    headers: { "User-Agent": userAgent, "Referer": embedUrl, ...embedHeaders }
  });
  if (!detailsResponse.ok) throw new Error(`Byse details HTTP ${detailsResponse.status}`);
  const details = await detailsResponse.json();
  const frameUrl = details.embed_frame_url || embedUrl;
  const frameBase = new URL(frameUrl).origin;
  const challengeResponse = await fetchImpl(`${frameBase}/api/videos/access/challenge`, {
    method: "POST",
    headers: { "Content-Length": "0", "Origin": frameBase, "Referer": frameUrl, "User-Agent": userAgent }
  });
  if (!challengeResponse.ok) throw new Error(`Byse challenge HTTP ${challengeResponse.status}`);
  const challenge = await challengeResponse.json();
  const keyPair = await crypto2.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign"]);
  const publicKey = await crypto2.subtle.exportKey("jwk", keyPair.publicKey);
  const signature = await crypto2.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, keyPair.privateKey, new TextEncoder().encode(challenge.nonce));
  const attestResponse = await fetchImpl(`${frameBase}/api/videos/access/attest`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Origin": frameBase, "Referer": frameUrl, "User-Agent": userAgent },
    body: JSON.stringify({ nonce: challenge.nonce, challenge_id: challenge.challenge_id, public_key: publicKey, signature: b64u(signature) })
  });
  if (!attestResponse.ok) throw new Error(`Byse attest HTTP ${attestResponse.status}`);
  const attest = await attestResponse.json();
  const cookie = `byse_viewer_id=${attest.viewer_id}; byse_device_id=${attest.device_id}`;
  const fingerprint = { token: attest.token, viewer_id: attest.viewer_id, device_id: attest.device_id, confidence: attest.confidence };
  const captchaResponse = await fetchImpl(`${frameBase}/api/videos/${code}/embed/captcha`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Origin": frameBase, "Referer": frameUrl, "User-Agent": userAgent, "Cookie": cookie, ...embedHeaders },
    body: JSON.stringify({ fingerprint })
  });
  if (!captchaResponse.ok) throw new Error(`Byse captcha HTTP ${captchaResponse.status}`);
  const captcha = await captchaResponse.json();
  const verifyResponse = await fetchImpl(`${frameBase}/api/videos/${code}/embed/captcha/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Origin": frameBase, "Referer": frameUrl, "User-Agent": userAgent, "Cookie": cookie, ...embedHeaders },
    body: JSON.stringify({ pow_token: captcha.pow_token, solution: solvePoW(captcha.pow_nonce, captcha.pow_difficulty), fingerprint })
  });
  if (!verifyResponse.ok) throw new Error(`Byse verify HTTP ${verifyResponse.status}`);
  const verification = await verifyResponse.json();
  const playbackResponse = await fetchImpl(`${frameBase}/api/videos/${code}/embed/playback`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Origin": frameBase, "Referer": frameUrl, "User-Agent": userAgent, "Cookie": cookie, "X-Captcha-Token": verification.token, ...embedHeaders },
    body: JSON.stringify({ fingerprint })
  });
  if (!playbackResponse.ok) throw new Error(`Byse playback HTTP ${playbackResponse.status}`);
  const playbackData = await playbackResponse.json();
  const playback = playbackData.playback;
  const keyBytes = Buffer.concat(playback.key_parts.filter((item) => b64uDec(item).length === 16).map(b64uDec));
  const aesKey = await crypto2.subtle.importKey("raw", keyBytes, { name: "AES-GCM" }, false, ["decrypt"]);
  const decrypted = await crypto2.subtle.decrypt({ name: "AES-GCM", iv: b64uDec(playback.iv) }, aesKey, b64uDec(playback.payload));
  return JSON.parse(new TextDecoder().decode(decrypted)).sources.map((item) => item.url);
}
__name(extractByse, "extractByse");

// extractors/babastream.js
import crypto3 from "node:crypto";
var DEFAULT_USER_AGENT2 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
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
    strings.push(value.replace(/\\([\\'"bnfrtv])/g, (_, escaped) => ({ b: "\b", n: "\n", f: "\f", r: "\r", t: "	", v: "\v" })[escaped] ?? escaped));
    index++;
  }
  return [...new Set(strings)];
}
__name(getScriptStrings, "getScriptStrings");
function getTemplateSuffixes(script) {
  return [...script.matchAll(/`[^`]*\$\{[^}]+\}([^`]+)`/g)].map((match) => match[1]);
}
__name(getTemplateSuffixes, "getTemplateSuffixes");
function parseConfig(html2) {
  for (const match of html2.matchAll(/\b(?:var|let|const)\s+\w+\s*=\s*(\{[^;]+\})\s*;/g)) {
    try {
      const config = JSON.parse(match[1]);
      if (typeof config.sid === "string" && typeof config.pk === "string") return config;
    } catch {
    }
  }
  return null;
}
__name(parseConfig, "parseConfig");
function getApiRoutes(html2) {
  const routes = [...html2.matchAll(/fetch\(\s*["']([^"']+)["']/g)].map((match) => match[1]);
  return {
    resolve: routes.find((route) => /resolve/i.test(route)) ?? null,
    verify: routes.find((route) => /verify/i.test(route)) ?? null
  };
}
__name(getApiRoutes, "getApiRoutes");
function getCryptoOptions(html2, key) {
  const algorithm = html2.match(/importKey\([^,]+,[^,]+,\s*\{\s*name\s*:\s*["'](AES-[A-Z]+)["']/i)?.[1];
  const ivLength = Number(html2.match(/getRandomValues\(new Uint8Array\((\d+)\)\)/)?.[1]);
  const tagLength = Number(html2.match(/tagLength\s*:\s*(\d+)/)?.[1]) || 128;
  if (!algorithm || !Number.isInteger(ivLength) || ivLength < 1 || !key.length) return null;
  return { cipher: `aes-${key.length * 8}-${algorithm.slice(4).toLowerCase()}`, ivLength, tagLength: tagLength / 8 };
}
__name(getCryptoOptions, "getCryptoOptions");
function encrypt(value, key, options) {
  const iv = crypto3.randomBytes(options.ivLength);
  const cipher = crypto3.createCipheriv(options.cipher, key, iv, { authTagLength: options.tagLength });
  return Buffer.concat([iv, cipher.update(value, "utf8"), cipher.final(), cipher.getAuthTag()]).toString("base64");
}
__name(encrypt, "encrypt");
function decrypt(value, key, options) {
  const raw = Buffer.from(value, "base64");
  const tagStart = raw.length - options.tagLength;
  if (tagStart <= options.ivLength) throw new Error("BabaStream encrypted payload is invalid");
  const decipher = crypto3.createDecipheriv(options.cipher, key, raw.subarray(0, options.ivLength), { authTagLength: options.tagLength });
  decipher.setAuthTag(raw.subarray(tagStart));
  return Buffer.concat([decipher.update(raw.subarray(options.ivLength, tagStart)), decipher.final()]).toString("utf8");
}
__name(decrypt, "decrypt");
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
__name(seededHex, "seededHex");
function solveProof(salt, target) {
  for (let nonce = 0; ; nonce++) {
    const hash2 = crypto3.createHash("sha256").update(`${salt}${nonce}`).digest("hex");
    if (hash2.startsWith(target)) return nonce;
  }
}
__name(solveProof, "solveProof");
function solveProofs(proofs) {
  return proofs.map(({ index, salt, target }) => ({ index, nonce: solveProof(salt, target) }));
}
__name(solveProofs, "solveProofs");
async function solveChallenge(payload) {
  if (Array.isArray(payload.challenges)) {
    const proofs2 = payload.challenges.map((challenge2, index) => {
      if (challenge2?.protocol !== "sha256-pow") throw new Error(`Unsupported BabaStream challenge protocol: ${challenge2?.protocol}`);
      return { index, salt: challenge2.payload.salt, target: challenge2.payload.target };
    });
    return (await solveProofs(proofs2)).map(({ nonce }) => ({ nonce }));
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
__name(solveChallenge, "solveChallenge");
async function fetchText2(fetchImpl, url, headers) {
  const response = await fetchImpl(url, { headers });
  if (!response.ok) throw new Error(`BabaStream HTTP ${response.status}: ${url}`);
  return response.text();
}
__name(fetchText2, "fetchText");
async function fetchJson(fetchImpl, url, options) {
  const response = await fetchImpl(url, options);
  if (!response.ok) throw new Error(`BabaStream HTTP ${response.status}: ${url}`);
  return response.json();
}
__name(fetchJson, "fetchJson");
function canExtractBabaStream(url) {
  return /babastream\.[^/]+\/embed\//i.test(String(url));
}
__name(canExtractBabaStream, "canExtractBabaStream");
async function extractBabaStreamDetails(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT2, referer } = {}) {
  const pageUrl = new URL(String(embedUrl));
  const pageHeaders = {
    "User-Agent": userAgent,
    "Accept": "text/html,*/*",
    "Referer": referer ?? `${pageUrl.origin}/`
  };
  const html2 = await fetchText2(fetchImpl, pageUrl, pageHeaders);
  const config = parseConfig(html2);
  if (!config?.cap) throw new Error(`BabaStream config not found: ${embedUrl}`);
  const key = Buffer.from(config.pk, "base64");
  const cryptoOptions = getCryptoOptions(html2, key);
  const routes = getApiRoutes(html2);
  if (!cryptoOptions || !routes.resolve || !routes.verify) throw new Error(`BabaStream client routes not found: ${embedUrl}`);
  const encryptedRequest = /* @__PURE__ */ __name(async (route, body) => {
    const response = await fetchJson(fetchImpl, new URL(route, pageUrl), {
      method: "POST",
      headers: { "Content-Type": "application/json", "User-Agent": userAgent, "Referer": pageUrl.href },
      body: JSON.stringify({ s: config.sid, d: encrypt(JSON.stringify(body), key, cryptoOptions) })
    });
    if (!response?.d) throw new Error("BabaStream response is missing encrypted data");
    return JSON.parse(decrypt(response.d, key, cryptoOptions));
  }, "encryptedRequest");
  let resolved2 = await encryptedRequest(routes.resolve, { ts: Date.now() });
  if (resolved2?.t === "error" && /verify/i.test(resolved2.m ?? "")) {
    const capScripts = [...html2.matchAll(/<script[^>]+src=["']([^"']+)["']/gi)].map((match) => new URL(match[1], pageUrl).href);
    const widget = (await Promise.all(capScripts.map(async (url) => {
      try {
        return await fetchText2(fetchImpl, url, { "User-Agent": userAgent, "Referer": pageUrl.href });
      } catch {
        return null;
      }
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
      body: JSON.stringify({ token: challenge.token, solutions })
    });
    if (!redeemed?.success || !redeemed.token) throw new Error("BabaStream challenge verification failed");
    const verified = await encryptedRequest(routes.verify, { ts: Date.now(), token: redeemed.token, mode: "invisible" });
    if (verified?.t !== "ok") throw new Error("BabaStream cap verification failed");
    resolved2 = await encryptedRequest(routes.resolve, { ts: Date.now() });
  }
  if (!resolved2?.u) throw new Error(resolved2?.m || "BabaStream did not return a stream");
  return {
    url: resolved2.u,
    type: /\.m3u8(?:$|[?&])/i.test(resolved2.u) ? "hls" : resolved2.t === "embed" ? "embed" : "mp4",
    origin: pageUrl.origin
  };
}
__name(extractBabaStreamDetails, "extractBabaStreamDetails");
async function extractBabaStream(embedUrl, options = {}) {
  const source = await extractBabaStreamDetails(embedUrl, options);
  return [source];
}
__name(extractBabaStream, "extractBabaStream");

// extractors/datasv.js
var DEFAULT_USER_AGENT3 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
function canExtractDataSv(url) {
  return /play\.echovideo\.ru\/embed-20\//i.test(String(url));
}
__name(canExtractDataSv, "canExtractDataSv");
async function extractDataSv(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT3 } = {}) {
  const url = new URL(String(embedUrl));
  const id = url.pathname.match(/^\/embed-20\/([^/]+)$/i)?.[1];
  if (!id) throw new Error(`Cannot extract DATASV id from ${embedUrl}`);
  const endpoint = new URL("/embed-20/getSources", url.origin);
  endpoint.searchParams.set("id", id);
  const response = await fetchImpl(endpoint, {
    headers: {
      "User-Agent": userAgent,
      "Referer": embedUrl,
      "X-Requested-With": "XMLHttpRequest"
    }
  });
  if (!response.ok) throw new Error(`DATASV sources HTTP ${response.status}`);
  const data = await response.json();
  const sources = [];
  for (const [quality, urls] of Object.entries(data?.sources ?? {})) {
    for (const source of Array.isArray(urls) ? urls : [urls]) {
      if (typeof source === "string" && source) sources.push({ url: source, type: "mp4", quality });
    }
  }
  const available = await Promise.all(sources.map(async (source) => {
    try {
      const check = await fetchImpl(source.url, {
        method: "HEAD",
        headers: { "User-Agent": userAgent, "Referer": `${url.origin}/` }
      });
      return check.ok ? source : null;
    } catch {
      return null;
    }
  }));
  const valid = available.filter(Boolean);
  if (!valid.length) throw new Error("DATASV response has no available sources");
  return valid;
}
__name(extractDataSv, "extractDataSv");

// extractors/megaplay.js
import crypto4 from "node:crypto";
var DEFAULT_USER_AGENT4 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
function decodeScriptString(value) {
  return value.replace(/\\u([\dA-Fa-f]{4})|\\x([\dA-Fa-f]{2})|\\([\\'"bnfrtv0])/g, (_, unicode, hex, escaped) => {
    if (unicode) return String.fromCharCode(Number.parseInt(unicode, 16));
    if (hex) return String.fromCharCode(Number.parseInt(hex, 16));
    return { b: "\b", n: "\n", f: "\f", r: "\r", t: "	", v: "\v", 0: "\0" }[escaped] ?? escaped;
  });
}
__name(decodeScriptString, "decodeScriptString");
function getScriptStrings2(script) {
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
    if (char === "'" || char === '"') {
      const quote = char;
      let value = "";
      index++;
      while (index < script.length && script[index] !== quote) {
        if (script[index] === "\\" && index + 1 < script.length) value += script[index++];
        value += script[index++];
      }
      strings.push(decodeScriptString(value));
      index++;
      continue;
    }
    if (char === "`") {
      index++;
      while (index < script.length && script[index] !== "`") index += script[index] === "\\" ? 2 : 1;
      index++;
      continue;
    }
    if (!/\s/.test(char)) previous = char;
    index++;
  }
  return [...new Set(strings)];
}
__name(getScriptStrings2, "getScriptStrings");
function getMegaPlayRoutes(script) {
  const routes = getScriptStrings2(script).filter((value) => /^stream\/getSources[\w/-]*$/i.test(value)).sort((left, right) => left.length - right.length);
  const legacy = routes[0] ?? null;
  const modern = routes.find((route) => route !== legacy && route.startsWith(legacy)) ?? null;
  return { legacy, modern };
}
__name(getMegaPlayRoutes, "getMegaPlayRoutes");
function decryptMegaPlaySource(value, script) {
  if (!value) return null;
  const encrypted = Buffer.from(value, "base64url");
  if (!encrypted.length || encrypted.length % 16) return null;
  const values = getScriptStrings2(script).filter((item) => Buffer.byteLength(item) > 0 && Buffer.byteLength(item) <= 32);
  const ivs = values.filter((item) => Buffer.byteLength(item) === 16);
  for (const keyValue of values) {
    const key = Buffer.alloc(32);
    Buffer.from(keyValue).copy(key);
    for (const ivValue of ivs) {
      try {
        const decipher = crypto4.createDecipheriv("aes-256-cbc", key, Buffer.from(ivValue));
        const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]);
        const data = JSON.parse(decrypted.toString("utf8"));
        const source = data?.file ?? data?.url;
        if (typeof source === "string" && source) return source;
      } catch {
      }
    }
  }
  return null;
}
__name(decryptMegaPlaySource, "decryptMegaPlaySource");
function buildSourceUrl(origin, path, fileId) {
  const endpoint = new URL(path, origin);
  endpoint.searchParams.append("id", fileId);
  endpoint.searchParams.append("id", fileId);
  return endpoint;
}
__name(buildSourceUrl, "buildSourceUrl");
async function fetchText3(fetchImpl, url, headers) {
  const response = await fetchImpl(url, { headers });
  if (!response.ok) throw new Error(`MegaPlay HTTP ${response.status}: ${url}`);
  return response.text();
}
__name(fetchText3, "fetchText");
async function fetchJson2(fetchImpl, url, headers) {
  const response = await fetchImpl(url, { headers });
  if (!response.ok) throw new Error(`MegaPlay HTTP ${response.status}: ${url}`);
  return response.json();
}
__name(fetchJson2, "fetchJson");
function canExtractMegaPlay(url) {
  return /megaplay\.[^/]+\/stream\//i.test(String(url));
}
__name(canExtractMegaPlay, "canExtractMegaPlay");
async function extractMegaPlayDetails(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT4, referer } = {}) {
  const pageUrl = new URL(String(embedUrl));
  const pageHeaders = {
    "User-Agent": userAgent,
    "Accept": "text/html,*/*",
    "Referer": referer ?? `${pageUrl.origin}/`
  };
  const pageHtml = await fetchText3(fetchImpl, pageUrl, pageHeaders);
  const fileId = pageHtml.match(/data-id=["']([^"']+)["']/i)?.[1];
  if (!fileId) throw new Error(`MegaPlay file id not found: ${embedUrl}`);
  const scriptUrls = [...pageHtml.matchAll(/<script[^>]+src=["']([^"']+)["']/gi)].map((match) => new URL(match[1], pageUrl).href);
  const scripts = await Promise.all(scriptUrls.map(async (url) => {
    try {
      return await fetchText3(fetchImpl, url, { "User-Agent": userAgent, "Referer": pageUrl.href });
    } catch {
      return null;
    }
  }));
  const script = scripts.find((value) => /getSources/i.test(value) && /AES-CBC/i.test(value));
  if (!script) throw new Error(`MegaPlay client script not found: ${embedUrl}`);
  const { legacy, modern } = getMegaPlayRoutes(script);
  if (!legacy && !modern) throw new Error(`MegaPlay source routes not found: ${embedUrl}`);
  const sourceHeaders = {
    "User-Agent": userAgent,
    "Accept": "application/json,*/*",
    "Referer": pageUrl.href,
    "X-Requested-With": "XMLHttpRequest"
  };
  const [modernData, legacyData] = await Promise.all([
    modern ? fetchJson2(fetchImpl, buildSourceUrl(pageUrl.origin, modern, fileId), sourceHeaders).catch(() => null) : null,
    legacy ? fetchJson2(fetchImpl, buildSourceUrl(pageUrl.origin, legacy, fileId), sourceHeaders).catch(() => null) : null
  ]);
  const legacyUrl = legacyData?.sources?.file ?? decryptMegaPlaySource(legacyData?.enc, script);
  const sources = [
    modernData?.sources?.file ? { url: modernData.sources.file, variant: "modern" } : null,
    legacyUrl ? { url: legacyUrl, variant: "legacy" } : null
  ].filter((source, index, all) => source && all.findIndex((candidate) => candidate?.url === source.url) === index);
  if (!sources.length) throw new Error(`MegaPlay response has no sources: ${embedUrl}`);
  const metadata = modernData ?? legacyData ?? {};
  return {
    origin: pageUrl.origin,
    sources,
    tracks: Array.isArray(metadata.tracks) ? metadata.tracks : [],
    intro: metadata.intro ?? null,
    outro: metadata.outro ?? null
  };
}
__name(extractMegaPlayDetails, "extractMegaPlayDetails");
async function extractMegaPlay(embedUrl, options = {}) {
  const details = await extractMegaPlayDetails(embedUrl, options);
  return details.sources.map((source) => source.url);
}
__name(extractMegaPlay, "extractMegaPlay");

// extractors/nova.js
import crypto5 from "node:crypto";
var DEFAULT_USER_AGENT5 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
var KEY = Buffer.from("6b69656d7469656e6d75613931316361", "hex");
var IV = Buffer.from("313233343536373839306f6975797472", "hex");
function canExtractNova(url) {
  return /upn\.one/i.test(String(url));
}
__name(canExtractNova, "canExtractNova");
async function extractNova(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT5 } = {}) {
  const id = String(embedUrl).match(/upn\.one\/#([A-Za-z0-9]+)/i)?.[1];
  if (!id) throw new Error(`Cannot extract Nova id from ${embedUrl}`);
  const response = await fetchImpl(`https://nova.upn.one/api/v1/video?id=${id}&w=1920&h=1080&r=`, {
    headers: { "User-Agent": userAgent, "Referer": "https://nova.upn.one/" }
  });
  if (!response.ok) throw new Error(`Nova fetch HTTP ${response.status}`);
  const hex = (await response.text()).trim();
  const decipher = crypto5.createDecipheriv("aes-128-cbc", KEY, IV);
  const decrypted = Buffer.concat([decipher.update(Buffer.from(hex, "hex")), decipher.final()]);
  const data = JSON.parse(decrypted.toString("utf8"));
  const url = data.cf ?? data.source;
  if (!url) throw new Error("Nova response missing m3u8 url");
  return [url];
}
__name(extractNova, "extractNova");

// extractors/vidplay.js
var DEFAULT_USER_AGENT6 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
function canExtractVidplay(url) {
  return /play\.echovideo\.ru\/embed-[01]\//i.test(String(url));
}
__name(canExtractVidplay, "canExtractVidplay");
async function extractVidplay(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT6 } = {}) {
  const url = new URL(String(embedUrl));
  const match = url.pathname.match(/^\/(embed-[01])\/([^/]+)$/i);
  const type = match?.[1];
  const id = match?.[2];
  if (!id) throw new Error(`Cannot extract Vidplay id from ${embedUrl}`);
  const endpoint = new URL(`/${type}/getSources`, url.origin);
  endpoint.searchParams.set("id", id);
  const response = await fetchImpl(endpoint, {
    headers: {
      "User-Agent": userAgent,
      "Referer": embedUrl,
      "X-Requested-With": "XMLHttpRequest"
    }
  });
  if (!response.ok) throw new Error(`Vidplay sources HTTP ${response.status}`);
  const data = await response.json();
  const sources = Array.isArray(data?.sources) ? data.sources.map((item) => typeof item === "string" ? item : item?.file ?? item?.url).filter(Boolean) : typeof data?.sources === "string" ? [data.sources] : [];
  if (!sources.length) throw new Error("Vidplay response has no sources");
  return sources;
}
__name(extractVidplay, "extractVidplay");

// extractors/vidmoly.js
var DEFAULT_USER_AGENT7 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
function canExtractVidmoly(url) {
  return /vidmoly\.(net|biz|to)/i.test(String(url));
}
__name(canExtractVidmoly, "canExtractVidmoly");
async function extractVidmoly(embedUrl, { fetchImpl = fetch, userAgent = DEFAULT_USER_AGENT7, referer } = {}) {
  const url = String(embedUrl).startsWith("//") ? `https:${embedUrl}` : String(embedUrl);
  const response = await fetchImpl(url, {
    headers: { "User-Agent": userAgent, "Referer": referer ?? "https://animenosub.to/" },
    redirect: "follow"
  });
  if (!response.ok) throw new Error(`Vidmoly fetch HTTP ${response.status}`);
  const html2 = await response.text();
  const match = html2.match(/sources:\s*\[\s*\{\s*file:\s*['"]([^'"]+\.m3u8[^'"]*)['"]/);
  if (!match) throw new Error("Vidmoly m3u8 not found in embed HTML");
  return [match[1]];
}
__name(extractVidmoly, "extractVidmoly");

// extractors/flixcloud.js
import { webcrypto as crypto6 } from "node:crypto";
var encoder = new TextEncoder();
var decoder = new TextDecoder();
async function sha256hex(value) {
  const bytes = await crypto6.subtle.digest("SHA-256", typeof value === "string" ? encoder.encode(value) : value);
  return Array.from(new Uint8Array(bytes)).map((item) => item.toString(16).padStart(2, "0")).join("");
}
__name(sha256hex, "sha256hex");
function b64toU8(value) {
  const binary = atob(value);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}
__name(b64toU8, "b64toU8");
async function deriveFields(seed) {
  let first = seed;
  for (let i = 0; i < 3; i++) first = await sha256hex(first + i);
  let second = first;
  for (let i = 0; i < 3; i++) second = await sha256hex(second + i);
  return {
    keyField: "kf_" + first.substring(8, 16),
    ivField: "ivf_" + first.substring(16, 24),
    containerName: "cd_" + first.substring(24, 32),
    arrayName: "ad_" + first.substring(32, 40),
    objectName: "od_" + first.substring(40, 48),
    tokenField: first.substring(48, 64) + "_" + first.substring(56, 64),
    keyFrag2Field: second.substring(0, 16) + "_" + second.substring(16, 24)
  };
}
__name(deriveFields, "deriveFields");
function extractSsrObj(html2) {
  const match = html2.match(/\{type:"data",data:(\{)/);
  if (!match) throw new Error("SSR data block not found");
  let depth = 0;
  const start = html2.indexOf("{", match.index + match[0].length - 1);
  for (let i = start; i < html2.length; i++) {
    if (html2[i] === "{") depth++;
    else if (html2[i] === "}" && --depth === 0) return html2.slice(start, i + 1);
  }
  throw new Error("SSR brace matching failed");
}
__name(extractSsrObj, "extractSsrObj");
function parseJsLiteral(source) {
  let index = 0;
  function whitespace() {
    while (index < source.length && /\s/.test(source[index])) index++;
  }
  __name(whitespace, "whitespace");
  function doubleString() {
    let out = "";
    index++;
    while (index < source.length && source[index] !== '"') {
      if (source[index] === "\\") {
        index++;
        out += { n: "\n", t: "	", r: "\r", '"': '"', "\\": "\\" }[source[index]] ?? source[index];
        index++;
      } else out += source[index++];
    }
    index++;
    return out;
  }
  __name(doubleString, "doubleString");
  function singleString() {
    let out = "";
    index++;
    while (index < source.length && source[index] !== "'") {
      if (source[index] === "\\") {
        index++;
        out += source[index] === "'" ? "'" : { n: "\n", t: "	", r: "\r", "\\": "\\" }[source[index]] ?? source[index];
        index++;
      } else out += source[index++];
    }
    index++;
    return out;
  }
  __name(singleString, "singleString");
  function key() {
    whitespace();
    if (source[index] === '"') return doubleString();
    if (source[index] === "'") return singleString();
    const match = source.slice(index).match(/^[a-zA-Z_$][a-zA-Z0-9_$]*/);
    if (!match) throw new Error(`Bad key at pos ${index}: ${source.slice(index, index + 20)}`);
    index += match[0].length;
    return match[0];
  }
  __name(key, "key");
  function object() {
    const out = {};
    index++;
    whitespace();
    while (index < source.length && source[index] !== "}") {
      if (source[index] === ",") {
        index++;
        whitespace();
        continue;
      }
      const property = key();
      whitespace();
      index++;
      out[property] = value();
      whitespace();
    }
    index++;
    return out;
  }
  __name(object, "object");
  function array() {
    const out = [];
    index++;
    whitespace();
    while (index < source.length && source[index] !== "]") {
      if (source[index] === ",") {
        index++;
        whitespace();
        continue;
      }
      out.push(value());
      whitespace();
    }
    index++;
    return out;
  }
  __name(array, "array");
  function value() {
    whitespace();
    if (source[index] === "{") return object();
    if (source[index] === "[") return array();
    if (source[index] === '"') return doubleString();
    if (source[index] === "'") return singleString();
    if (source.startsWith("true", index)) {
      index += 4;
      return true;
    }
    if (source.startsWith("false", index)) {
      index += 5;
      return false;
    }
    if (source.startsWith("null", index)) {
      index += 4;
      return null;
    }
    if (source.startsWith("undefined", index)) {
      index += 9;
      return null;
    }
    if (source.startsWith("!0", index)) {
      index += 2;
      return true;
    }
    if (source.startsWith("!1", index)) {
      index += 2;
      return false;
    }
    const match = source.slice(index).match(/^-?[\d.]+([eE][+-]?\d+)?/);
    if (match) {
      index += match[0].length;
      return parseFloat(match[0]);
    }
    throw new Error(`JS parse error at pos ${index}: ...${source.slice(index, index + 20)}`);
  }
  __name(value, "value");
  return value();
}
__name(parseJsLiteral, "parseJsLiteral");
function parseWasmDecrypt(bytes) {
  let position = 8;
  while (position < bytes.length) {
    const section = bytes[position++];
    let size = 0;
    let shift = 0;
    let next2;
    do {
      next2 = bytes[position++];
      size |= (next2 & 127) << shift;
      shift += 7;
    } while (next2 & 128);
    if (section === 10) {
      position++;
      let bodySize = 0;
      let bodyShift = 0;
      do {
        next2 = bytes[position++];
        bodySize |= (next2 & 127) << bodyShift;
        bodyShift += 7;
      } while (next2 & 128);
      position += bodySize;
      break;
    }
    position += size;
  }
  let functionSize = 0;
  let functionShift = 0;
  let next;
  do {
    next = bytes[position++];
    functionSize |= (next & 127) << functionShift;
    functionShift += 7;
  } while (next & 128);
  const body = bytes.slice(position, position + functionSize);
  function leb(array, index) {
    let value = 0;
    let shift = 0;
    let next2;
    do {
      next2 = array[index++];
      value |= (next2 & 127) << shift;
      shift += 7;
    } while (next2 & 128);
    return [value, index];
  }
  __name(leb, "leb");
  const xorEnd = [32, 2, 32, 5, 106, 45, 0, 0, 115, 33, 6];
  let transformStart = -1;
  outer: for (let i = 0; i < body.length - xorEnd.length; i++) {
    for (let j = 0; j < xorEnd.length; j++) if (body[i + j] !== xorEnd[j]) continue outer;
    transformStart = i + xorEnd.length;
    break;
  }
  if (transformStart < 0) throw new Error("WASM: transform start not found");
  let transformEnd = -1;
  let step = 36;
  for (let i = transformStart; i < body.length - 4; i++) {
    if (body[i] === 32 && body[i + 1] === 5 && body[i + 2] === 65) {
      const [value, nextIndex] = leb(body, i + 3);
      if (body[nextIndex] === 108) {
        transformEnd = i;
        step = value;
        break;
      }
    }
  }
  if (transformEnd < 0) throw new Error("WASM: keystream not found");
  const code = body.slice(transformStart, transformEnd);
  function transform(inputByte) {
    let local = inputByte & 255;
    const stack = [];
    let index = 0;
    while (index < code.length) {
      const opcode = code[index++];
      if (opcode === 32) {
        const [localIndex, nextIndex] = leb(code, index);
        index = nextIndex;
        stack.push(localIndex === 6 ? local : 0);
      } else if (opcode === 33) {
        const [localIndex, nextIndex] = leb(code, index);
        index = nextIndex;
        const value = stack.pop();
        if (localIndex === 6) local = value & 255;
      } else if (opcode === 65) {
        const [value, nextIndex] = leb(code, index);
        index = nextIndex;
        stack.push(value);
      } else if (opcode === 106) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left + right & 255);
      } else if (opcode === 107) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left - right + 256 & 255);
      } else if (opcode === 113) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left & right & 255);
      } else if (opcode === 114) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left | right & 255);
      } else if (opcode === 115) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left ^ right & 255);
      } else if (opcode === 116) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left << (right & 7) & 255);
      } else if (opcode === 118) {
        const right = stack.pop();
        const left = stack.pop();
        stack.push(left >>> (right & 7) & 255);
      }
    }
    return local;
  }
  __name(transform, "transform");
  return { step, transform };
}
__name(parseWasmDecrypt, "parseWasmDecrypt");
function runDecrypt(wasmBytes, fragment, keyFragment, token, seed) {
  const { step, transform } = parseWasmDecrypt(wasmBytes);
  const out = new Uint8Array(fragment.length);
  for (let i = 0; i < fragment.length; i++) {
    const value = fragment[i] ^ keyFragment[i] ^ token[i] & 255;
    out[i] = transform(value) ^ i * step + seed & 255;
  }
  return out;
}
__name(runDecrypt, "runDecrypt");
async function extractFlixcloud(embedHtml, { fetchImpl = fetch, apiBase = "https://flixcloud.cc", headers = {}, referer } = {}) {
  const data = parseJsLiteral(extractSsrObj(embedHtml));
  const seed = data.obfuscation_seed;
  if (!seed) {
    const error = new Error("obfuscation_seed missing");
    error.debug = { topKeys: Object.keys(data).slice(0, 20) };
    throw error;
  }
  const fields = await deriveFields(seed);
  const cryptoData = data.obfuscated_crypto_data;
  if (!cryptoData) {
    const error = new Error("obfuscated_crypto_data missing");
    error.debug = { fields, topKeys: Object.keys(data).slice(0, 20) };
    throw error;
  }
  const container = cryptoData[fields.containerName];
  if (!container) {
    const error = new Error(`containerName "${fields.containerName}" not in ocd`);
    error.debug = { fields, ocdKeys: Object.keys(cryptoData).slice(0, 10) };
    throw error;
  }
  const array = container[fields.arrayName];
  if (!array) {
    const error = new Error(`arrayName "${fields.arrayName}" not in container`);
    error.debug = { fields, containerKeys: Object.keys(container).slice(0, 10) };
    throw error;
  }
  const object = array[0][fields.objectName];
  if (!object) {
    const error = new Error(`objectName "${fields.objectName}" not in arr[0]`);
    error.debug = { fields, arr0Keys: Object.keys(array[0]).slice(0, 10) };
    throw error;
  }
  const fragment = b64toU8(object[fields.keyField]);
  const iv = b64toU8(object[fields.ivField]);
  const keyFragmentRaw = data[fields.keyFrag2Field];
  if (!keyFragmentRaw) {
    const error = new Error(`kf2 field "${fields.keyFrag2Field}" not in data`);
    error.debug = { fields, topKeys: Object.keys(data).slice(0, 20) };
    throw error;
  }
  const keyFragment = b64toU8(keyFragmentRaw);
  const token = data[fields.tokenField];
  if (!token) {
    const error = new Error(`tokenField "${fields.tokenField}" missing`);
    error.debug = { fields, topKeys: Object.keys(data).slice(0, 20) };
    throw error;
  }
  const tokenResponse = await fetchImpl(`${apiBase}/api/m3u8/${token}`, { headers: { ...headers, ...referer ? { Referer: referer } : {} } });
  if (!tokenResponse.ok) {
    const error = new Error(`Token API ${tokenResponse.status}`);
    error.rawBody = await tokenResponse.text().catch(() => null);
    throw error;
  }
  const tokenData = await tokenResponse.json();
  const videoKey = (await sha256hex(token + "vid")).substring(0, 10);
  const tokenKey = (await sha256hex(token + "key")).substring(0, 10);
  const videoBytes = b64toU8(tokenData[videoKey]);
  const tokenBytes = b64toU8(tokenData[tokenKey]);
  if (!videoBytes.length || !tokenBytes.length) {
    const error = new Error(`Token fields missing. vidKey="${videoKey}" keyKey="${tokenKey}"`);
    error.debug = { tokKeys: Object.keys(tokenData).slice(0, 10) };
    throw error;
  }
  const seedNumber = parseInt(seed.substring(0, 8), 16);
  const wasmPayload = b64toU8(data.w_payload ?? "");
  if (!wasmPayload.length) throw new Error("w_payload missing from embed data");
  let wasmOut;
  try {
    wasmOut = runDecrypt(wasmPayload, fragment, keyFragment, tokenBytes, seedNumber);
  } catch (error) {
    error.wasmHex = Array.from(wasmPayload).map((item) => item.toString(16).padStart(2, "0")).join("");
    throw error;
  }
  const material = await crypto6.subtle.importKey("raw", wasmOut, { name: "PBKDF2" }, false, ["deriveBits"]);
  const derived = new Uint8Array(await crypto6.subtle.deriveBits({ name: "PBKDF2", salt: encoder.encode(seed), iterations: 1e3, hash: "SHA-256" }, material, 256));
  for (let i = 0; i < 32; i++) derived[i] ^= seed.charCodeAt(i % seed.length);
  const aesKeyBytes = new Uint8Array(await crypto6.subtle.digest("SHA-256", derived));
  const aesKey = await crypto6.subtle.importKey("raw", aesKeyBytes, { name: "AES-CBC" }, false, ["decrypt"]);
  let plain;
  try {
    plain = await crypto6.subtle.decrypt({ name: "AES-CBC", iv }, aesKey, videoBytes);
  } catch (error) {
    error.debug = {
      seedInt: "0x" + seedNumber.toString(16),
      frag1Len: fragment.length,
      kf2Len: keyFragment.length,
      T_bytesLen: tokenBytes.length,
      ivLen: iv.length,
      v_bytesLen: videoBytes.length,
      wPayloadLen: wasmPayload.length,
      wasmOutHex: Array.from(wasmOut).map((item) => item.toString(16).padStart(2, "0")).join("")
    };
    throw error;
  }
  const url = decoder.decode(plain).trim().replace(/\0+$/, "");
  if (!url.startsWith("http")) throw new Error(`Unexpected decrypted value: ${url.substring(0, 60)}`);
  return {
    url,
    subtitles: data.subtitles ?? [],
    thumbnails_vtt: data.thumbnails_vtt ?? null,
    video_title: data.video_title ?? null,
    intro_chapter: data.intro_chapter ?? null,
    outro_chapter: data.outro_chapter ?? null,
    video_id: data.video_id ?? null
  };
}
__name(extractFlixcloud, "extractFlixcloud");

// extractors/index.js
var videoExtractors = [
  { name: "babastream", matches: canExtractBabaStream, extract: extractBabaStream },
  { name: "byse", matches: canExtractByse, extract: extractByse },
  { name: "datasv", matches: canExtractDataSv, extract: extractDataSv },
  { name: "megaplay", matches: canExtractMegaPlay, extract: extractMegaPlay },
  { name: "vidplay", matches: canExtractVidplay, extract: extractVidplay },
  { name: "vidmoly", matches: canExtractVidmoly, extract: extractVidmoly },
  { name: "nova", matches: canExtractNova, extract: extractNova }
];
function findVideoExtractor(url) {
  return videoExtractors.find((extractor) => extractor.matches(url)) ?? null;
}
__name(findVideoExtractor, "findVideoExtractor");

// core/smartcache.js
function readEnv(name) {
  try {
    return typeof process !== "undefined" ? process.env?.[name] : void 0;
  } catch {
    return void 0;
  }
}
__name(readEnv, "readEnv");
function envBool(name, fallback) {
  const raw = readEnv(name);
  if (raw === void 0 || raw === "") return fallback;
  return ["true", "1", "yes", "on"].includes(String(raw).toLowerCase());
}
__name(envBool, "envBool");
function envInt(name, fallback) {
  const raw = readEnv(name);
  const n = Number(raw);
  return raw !== void 0 && raw !== "" && Number.isFinite(n) && n > 0 ? n : fallback;
}
__name(envInt, "envInt");
var _CACHE_ENABLED = envBool("CACHE_ENABLED", false);
var IS_LOCAL_NODE = (() => {
  try {
    return typeof process !== "undefined" && typeof process.versions?.node === "string" && !process.env.VERCEL;
  } catch {
    return false;
  }
})();
var UPSTASH_REDIS_REST_URL = readEnv("UPSTASH_REDIS_REST_URL") ?? "";
var UPSTASH_REDIS_REST_TOKEN = readEnv("UPSTASH_REDIS_REST_TOKEN") ?? "";
var REDIS_ENABLED = Boolean(UPSTASH_REDIS_REST_URL && UPSTASH_REDIS_REST_TOKEN);
var REDIS_TTL_MS = envInt("DEFAULT_REDIS_TTL", 900) * 1e3;
function encodeEntry(entry) {
  return JSON.stringify(entry, (_, value) => value === Infinity ? "__Infinity__" : value);
}
__name(encodeEntry, "encodeEntry");
function decodeEntry(raw) {
  return JSON.parse(raw, (_, value) => value === "__Infinity__" ? Infinity : value);
}
__name(decodeEntry, "decodeEntry");
async function redisCommand(command) {
  if (!REDIS_ENABLED || typeof fetch !== "function") return null;
  const res = await fetch(UPSTASH_REDIS_REST_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${UPSTASH_REDIS_REST_TOKEN}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(command)
  }).catch(() => null);
  if (!res?.ok) return null;
  const json5 = await res.json().catch(() => null);
  return json5?.result ?? null;
}
__name(redisCommand, "redisCommand");
async function redisWrite(key, entry) {
  if (!REDIS_ENABLED) return;
  const value = encodeEntry(entry);
  const ttlMs = Number.isFinite(entry.ttl) && entry.ttl > 0 ? entry.ttl : REDIS_TTL_MS;
  await redisCommand(["SET", key, value, "PX", Math.ceil(ttlMs)]);
}
__name(redisWrite, "redisWrite");
var diskRead = /* @__PURE__ */ __name(() => null, "diskRead");
var diskWrite = /* @__PURE__ */ __name(() => {
}, "diskWrite");
var diskDel = /* @__PURE__ */ __name(() => {
}, "diskDel");
if (IS_LOCAL_NODE) {
  const { readFileSync, mkdirSync, existsSync } = await import("node:fs");
  const { writeFile, unlink } = await import("node:fs/promises");
  const { join, dirname } = await import("node:path");
  const { fileURLToPath } = await import("node:url");
  const __dir = dirname(fileURLToPath(import.meta.url));
  const CACHE_DIR = join(__dir, ".cache");
  try {
    mkdirSync(CACHE_DIR, { recursive: true });
  } catch {
  }
  const keyToPath = /* @__PURE__ */ __name((key) => join(CACHE_DIR, key.replace(/[^a-zA-Z0-9_-]/g, "_") + ".json"), "keyToPath");
  diskRead = /* @__PURE__ */ __name((key) => {
    try {
      const p = keyToPath(key);
      if (!existsSync(p)) return null;
      return decodeEntry(readFileSync(p, "utf8"));
    } catch {
      return null;
    }
  }, "diskRead");
  diskWrite = /* @__PURE__ */ __name((key, entry) => {
    writeFile(keyToPath(key), encodeEntry(entry)).catch(() => {
    });
  }, "diskWrite");
  diskDel = /* @__PURE__ */ __name((key) => {
    unlink(keyToPath(key)).catch(() => {
    });
  }, "diskDel");
}
var MAX_MEM = 800;
var mem = /* @__PURE__ */ new Map();
function evict() {
  if (mem.size <= MAX_MEM) return;
  const drop = mem.size - MAX_MEM;
  let n = 0;
  for (const k of mem.keys()) {
    if (n++ >= drop) break;
    mem.delete(k);
  }
}
__name(evict, "evict");
function get(key) {
  if (!_CACHE_ENABLED) return null;
  let e = mem.get(key);
  if (e) return e;
  e = diskRead(key);
  if (!e) return null;
  mem.set(key, e);
  evict();
  return e;
}
__name(get, "get");
async function getAsync(key) {
  if (!_CACHE_ENABLED) return null;
  let e = get(key);
  if (e) return e;
  const raw = await redisCommand(["GET", key]);
  if (!raw) return null;
  try {
    e = typeof raw === "string" ? decodeEntry(raw) : raw;
    if (!isFresh(e)) {
      await delAsync(key);
      return null;
    }
    mem.set(key, e);
    evict();
    diskWrite(key, e);
    return e;
  } catch {
    return null;
  }
}
__name(getAsync, "getAsync");
function setLocal(key, data, ttlMs, refreshAfterMs) {
  const now = Date.now();
  const entry = {
    data,
    cachedAt: now,
    ttl: ttlMs,
    refreshAfter: refreshAfterMs ?? ttlMs,
    expiresAt: now + ttlMs
  };
  mem.delete(key);
  mem.set(key, entry);
  evict();
  diskWrite(key, entry);
  return entry;
}
__name(setLocal, "setLocal");
function set(key, data, ttlMs, refreshAfterMs) {
  if (!_CACHE_ENABLED) return { data, cachedAt: Date.now(), ttl: ttlMs, refreshAfter: refreshAfterMs ?? ttlMs, expiresAt: Date.now() + ttlMs };
  const entry = setLocal(key, data, ttlMs, refreshAfterMs);
  redisWrite(key, entry).catch(() => {
  });
  return entry;
}
__name(set, "set");
async function setAsync(key, data, ttlMs, refreshAfterMs) {
  if (!_CACHE_ENABLED) return { data, cachedAt: Date.now(), ttl: ttlMs, refreshAfter: refreshAfterMs ?? ttlMs, expiresAt: Date.now() + ttlMs };
  const entry = setLocal(key, data, ttlMs, refreshAfterMs);
  await redisWrite(key, entry);
  return entry;
}
__name(setAsync, "setAsync");
function isFresh(entry) {
  return entry !== null && entry !== void 0 && Date.now() < entry.expiresAt;
}
__name(isFresh, "isFresh");
function needsRefresh(entry) {
  return !entry || Date.now() - entry.cachedAt > entry.refreshAfter;
}
__name(needsRefresh, "needsRefresh");
function delLocal(key) {
  mem.delete(key);
  diskDel(key);
}
__name(delLocal, "delLocal");
async function delAsync(key) {
  delLocal(key);
  await redisCommand(["DEL", key]);
}
__name(delAsync, "delAsync");
function delByPrefix(prefix) {
  for (const k of [...mem.keys()]) {
    if (k.startsWith(prefix)) mem.delete(k);
  }
}
__name(delByPrefix, "delByPrefix");
async function delByPrefixAsync(prefix) {
  delByPrefix(prefix);
  const keys = await redisCommand(["KEYS", `${prefix}*`]);
  if (Array.isArray(keys) && keys.length) {
    await redisCommand(["DEL", ...keys]);
  }
}
__name(delByPrefixAsync, "delByPrefixAsync");
var MIN = 6e4;
var HOUR = 60 * MIN;
var DAY = 24 * HOUR;
function episodeTTL(status) {
  switch (status) {
    case "FINISHED":
      return [7 * DAY, Infinity];
    case "RELEASING":
      return [2 * HOUR, 15 * MIN];
    case "HIATUS":
      return [6 * HOUR, 60 * MIN];
    case "NOT_YET_RELEASED":
      return [30 * MIN, 15 * MIN];
    default:
      return [HOUR, 15 * MIN];
  }
}
__name(episodeTTL, "episodeTTL");
function jikanPageTTL(isLastPage, status) {
  if (!isLastPage || status === "FINISHED") return [7 * DAY, Infinity];
  switch (status) {
    case "RELEASING":
      return [2 * HOUR, 15 * MIN];
    case "HIATUS":
      return [6 * HOUR, 60 * MIN];
    case "NOT_YET_RELEASED":
      return [30 * MIN, 15 * MIN];
    default:
      return [2 * HOUR, 15 * MIN];
  }
}
__name(jikanPageTTL, "jikanPageTTL");
function mapTTL(status) {
  return status === "FINISHED" ? 30 * DAY : 12 * HOUR;
}
__name(mapTTL, "mapTTL");
var WATCH_TTL = 3 * HOUR;
var SHOW_IDENTITY_TTL = 24 * HOUR;
var THIRTY_DAYS = 30 * DAY;

// core/new-provider-utils.js
var UA3 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var RELATION_FRAGMENT = `edges{relationType(version:2) node{id type episodes relations{edges{relationType(version:2) node{id type episodes relations{edges{relationType(version:2) node{id type episodes relations{edges{relationType(version:2) node{id type episodes}}}}}}}}}}}`;
async function fetchHtml(url, headers = {}) {
  const res = await fetch(url, {
    headers: {
      "User-Agent": UA3,
      Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9",
      ...headers
    }
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} fetching ${url}`);
  return res.text();
}
__name(fetchHtml, "fetchHtml");
function decodeEntities(s = "") {
  return s.replace(/&#(\d+);/g, (_, n) => String.fromCharCode(Number(n))).replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCharCode(parseInt(n, 16))).replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").trim();
}
__name(decodeEntities, "decodeEntities");
function stripTags(html2 = "") {
  return decodeEntities(html2.replace(/<[^>]*>/g, " ").replace(/\s+/g, " "));
}
__name(stripTags, "stripTags");
function attr(tag, name) {
  const m = tag.match(new RegExp(`${name}=["']([^"']*)["']`, "i"));
  return m ? decodeEntities(m[1]) : "";
}
__name(attr, "attr");
function norm(s = "") {
  return s.toLowerCase().replace(/[^a-z0-9]/g, "");
}
__name(norm, "norm");
function diceCoeff(a, b) {
  const na = norm(a);
  const nb = norm(b);
  if (na === nb) return 1;
  if (na.length < 2 || nb.length < 2) return 0;
  const bigrams = /* @__PURE__ */ new Map();
  for (let i = 0; i < na.length - 1; i++) {
    const bg2 = na.slice(i, i + 2);
    bigrams.set(bg2, (bigrams.get(bg2) ?? 0) + 1);
  }
  let hits = 0;
  for (let i = 0; i < nb.length - 1; i++) {
    const bg2 = nb.slice(i, i + 2);
    const count = bigrams.get(bg2) ?? 0;
    if (count > 0) {
      hits++;
      bigrams.set(bg2, count - 1);
    }
  }
  return 2 * hits / (na.length + nb.length - 2);
}
__name(diceCoeff, "diceCoeff");
function titleScore(query, candidate, slug) {
  const base = Math.max(diceCoeff(query, candidate), diceCoeff(query, slug.replace(/-/g, " ")));
  const queryFirstNum = norm(query).match(/\d+/)?.[0] ?? "";
  const slugFirstNum = slug.match(/\d+/)?.[0] ?? "";
  if (queryFirstNum && slugFirstNum && queryFirstNum !== slugFirstNum) return base * 0.65;
  if (queryFirstNum && !slugFirstNum) return base * 0.65;
  if (!queryFirstNum && slugFirstNum) {
    const n = parseInt(slugFirstNum);
    if (n > 1 && n < 1900) return base * (1 - 0.06 * (n - 1));
  }
  const isMovieQuery = /\b(movie|film|the movie)\b/i.test(query);
  const isMovieMatch = /\b(movie|film)\b/i.test(candidate) || /movie|film/.test(slug);
  if (isMovieQuery && !isMovieMatch) return base * 0.4;
  const qLen = norm(query).length;
  const sLen = norm(slug.replace(/-/g, " ")).length;
  return sLen > qLen * 1.6 + 4 ? base * 0.8 : base;
}
__name(titleScore, "titleScore");
function buildSearchQueries(title) {
  const queries = /* @__PURE__ */ new Set([title]);
  const words = title.trim().split(/\s+/);
  if (words.length > 4) queries.add(words.slice(0, 4).join(" "));
  if (words.length > 3) queries.add(words.slice(0, 3).join(" "));
  const stripped = title.replace(/\bseason\s*\d+\b/gi, "").replace(/\bpart\s*\d+\b/gi, "").replace(/\b\d+rd\b|\b\d+th\b|\b\d+st\b|\b\d+nd\b/gi, "").replace(/\s+/g, " ").trim();
  if (stripped && stripped !== title) queries.add(stripped);
  return [...queries].filter((q) => q.length >= 3);
}
__name(buildSearchQueries, "buildSearchQueries");
async function findTopSlugs(titles, searchFn2, n = 6) {
  const allCandidates = /* @__PURE__ */ new Map();
  const searchQueries4 = /* @__PURE__ */ new Set();
  for (const title of titles.slice(0, 4)) {
    for (const q of buildSearchQueries(title)) searchQueries4.add(q);
  }
  await Promise.all([...searchQueries4].map(async (q) => {
    try {
      const results = await searchFn2(q);
      for (const r of results) if (!allCandidates.has(r.slug)) allCandidates.set(r.slug, r.text);
    } catch {
    }
  }));
  const scored = [];
  for (const [slug, text] of allCandidates) {
    let best = 0;
    for (const title of titles.slice(0, 2)) best = Math.max(best, titleScore(title, text, slug));
    if (best >= 0.5) scored.push({ slug, title: text, score: best });
  }
  return scored.sort((a, b) => b.score - a.score).slice(0, n);
}
__name(findTopSlugs, "findTopSlugs");
async function anilistQuery(query, variables) {
  const res = await fetch("https://graphql.anilist.co", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ query, variables })
  });
  if (!res.ok) throw new Error(`AniList HTTP ${res.status}`);
  const json5 = await res.json();
  if (json5.errors?.length) throw new Error(`AniList: ${json5.errors[0].message}`);
  return json5.data;
}
__name(anilistQuery, "anilistQuery");
function computePrequelOffset(relations, depth = 0) {
  if (!relations || depth > 5) return 0;
  const prequelEdge = relations.edges?.find(
    (e) => e.relationType === "PREQUEL" && e.node.type === "ANIME" && (e.node.episodes ?? 0) >= 5
  );
  if (!prequelEdge) return 0;
  return (prequelEdge.node.episodes ?? 0) + computePrequelOffset(prequelEdge.node.relations, depth + 1);
}
__name(computePrequelOffset, "computePrequelOffset");
async function getPrequelOffset(anilistId) {
  const key = `np-offset:${anilistId}`;
  const entry = get(key);
  if (isFresh(entry)) return entry.data;
  const data = await anilistQuery(
    `query($id:Int){Media(id:$id,type:ANIME){relations{${RELATION_FRAGMENT}}}}`,
    { id: Number(anilistId) }
  );
  const offset = computePrequelOffset(data?.Media?.relations);
  set(key, offset, SHOW_IDENTITY_TTL);
  return offset;
}
__name(getPrequelOffset, "getPrequelOffset");
function buildTitles(media, anizip) {
  return [
    media?.title?.english,
    media?.title?.romaji,
    media?.title?.native,
    ...media?.synonyms ?? [],
    anizip?.titles?.en,
    anizip?.titles?.["x-jat"],
    anizip?.titles?.ja
  ].filter(Boolean);
}
__name(buildTitles, "buildTitles");
function expectedCount(media, anizip, jikanEps) {
  const counts = [
    media?.episodes,
    ...Object.keys(anizip?.episodes ?? {}).map(Number).filter(Number.isFinite),
    ...(jikanEps ?? []).map((e) => e.mal_id).filter(Number.isFinite)
  ].filter((n) => Number.isFinite(n) && n > 0);
  return counts.length ? Math.max(...counts) : null;
}
__name(expectedCount, "expectedCount");
function episodeMeta(n, ctx) {
  const az = ctx.anizip?.episodes?.[String(n)] ?? {};
  const jk = (ctx.jikanEps ?? []).find((e) => Number(e.mal_id) === Number(n));
  const runtime = az.runtime ?? az.length ?? null;
  return {
    title: jk?.title ?? az.title?.en ?? az.title?.["x-jat"] ?? null,
    duration: runtime ? runtime * 60 : null,
    filler: jk?.filler ?? az.filler ?? false,
    uncensored: false,
    description: az.overview ?? az.summary ?? null,
    image: az.image ?? ctx.anizip?.images?.cover ?? null,
    airDate: jk?.aired ?? az.airdate ?? az.aired ?? null
  };
}
__name(episodeMeta, "episodeMeta");
function selectSeries(candidates, scrapeSeries5, expected, status, offset, options = {}) {
  return Promise.all(candidates.map(async (candidate) => {
    const episodes = await scrapeSeries5(candidate.slug);
    const max = Math.max(0, ...episodes.map((e) => e.number));
    const localHits = expected ? episodes.filter((e) => e.number >= 1 && e.number <= expected).length : episodes.length;
    const offsetHits = expected && offset ? episodes.filter((e) => e.number > offset && e.number <= offset + expected).length : 0;
    const mode = offsetHits > localHits ? "offset" : "local";
    const hits = Math.max(localHits, offsetHits);
    let countScore = 1;
    if (expected && expected >= 6) {
      const needed = status === "FINISHED" ? Math.ceil(expected * 0.9) : Math.max(1, expected - 3);
      countScore = hits >= needed ? 1 : hits / needed;
    }
    return { ...candidate, episodes, max, mode, score: candidate.score * 0.7 + countScore * 0.3 };
  })).then((results) => {
    const minScore = options.minScore ?? 0.65;
    const viable = results.filter((r) => r.episodes.length && r.score >= minScore).sort((a, b) => b.score - a.score);
    if (!viable.length) return null;
    return viable[0];
  });
}
__name(selectSeries, "selectSeries");
function json2(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=300"
    }
  });
}
__name(json2, "json");

// providers/reanime.js
var __name5 = /* @__PURE__ */ __name((fn, _) => fn, "__name");
var BASE = "https://reanime.to";
var FLIX = "https://flixcloud.cc";
var ANIZIP2 = "https://api.ani.zip/mappings";
var UA5 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var H = { "User-Agent": UA5, Accept: "application/json, */*" };
async function searchReanime(query) {
  const data = await fetch(`${BASE}/api/v1/search?${new URLSearchParams({ q: query, limit: 10 })}`, { headers: H }).then(async (r) => {
    const _raw = await r.text();
    if (!r.ok) {
      const _e = new Error(`reanime search ${r.status}`);
      _e.rawBody = _raw;
      throw _e;
    }
    try {
      return JSON.parse(_raw);
    } catch (_pe) {
      _pe.rawBody = _raw;
      throw _pe;
    }
  });
  return Array.isArray(data?.results) ? data.results : [];
}
__name(searchReanime, "searchReanime");
__name5(searchReanime, "searchReanime");
async function fetchAnimeDetail(animeId) {
  const res = await fetch(`${BASE}/api/v1/anime/${animeId}`, { headers: H });
  if (!res.ok) return null;
  return res.json().catch(() => null);
}
__name(fetchAnimeDetail, "fetchAnimeDetail");
__name5(fetchAnimeDetail, "fetchAnimeDetail");
function extractAnilistIdFromCover(coverImage) {
  const urls = [coverImage?.extra_large, coverImage?.large, coverImage?.medium].filter(Boolean);
  for (const url of urls) {
    const m = url.match(/anilist\.co\/.*\/bx(\d+)-/);
    if (m) return Number(m[1]);
  }
  return null;
}
__name(extractAnilistIdFromCover, "extractAnilistIdFromCover");
__name5(extractAnilistIdFromCover, "extractAnilistIdFromCover");
async function resolveSeries(anilistId, ctx = {}) {
  const cacheKey = `np:reanime:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const malId = media?.idMal ?? null;
  const queries = buildTitles(media, ctx.anizip).slice(0, 5);
  const candidates = /* @__PURE__ */ new Map();
  await Promise.all(queries.map(async (q) => {
    for (const r of await searchReanime(q).catch(() => [])) {
      if (r?.anime_id && !candidates.has(r.anime_id)) candidates.set(r.anime_id, r);
    }
  }));
  for (const [id, r] of candidates) {
    const coverId = extractAnilistIdFromCover(r.cover_image);
    if (coverId && coverId === Number(anilistId)) {
      const data = {
        animeId: id,
        title: r.title?.english || r.title?.romaji || id,
        anilistId: Number(anilistId),
        malId: null,
        subbed: Number.isFinite(r.subbed) ? r.subbed : null,
        dubbed: Number.isFinite(r.dubbed) ? r.dubbed : null,
        episodesCount: Number.isFinite(r.episodes) ? r.episodes : null,
        matchType: "cover_image",
        matchScore: 1
      };
      set(cacheKey, data, SHOW_IDENTITY_TTL);
      return data;
    }
  }
  const needsDetail = [...candidates.keys()].filter(
    (id) => extractAnilistIdFromCover(candidates.get(id)?.cover_image) === null
  );
  const details = await Promise.all(
    needsDetail.map(async (id) => ({ id, detail: await fetchAnimeDetail(id).catch(() => null) }))
  );
  for (const { id, detail } of details) {
    if (detail?.anilist_id && Number(detail.anilist_id) === Number(anilistId)) {
      const data = {
        animeId: id,
        title: detail.title?.english || detail.title?.romaji || candidates.get(id)?.title?.english || id,
        anilistId: Number(anilistId),
        malId: detail.mal_id || null,
        subbed: Number.isFinite(detail.subbed) ? detail.subbed : null,
        dubbed: Number.isFinite(detail.dubbed) ? detail.dubbed : null,
        episodesCount: Number.isFinite(detail.episodes) ? detail.episodes : null,
        matchType: "anilist",
        matchScore: 1
      };
      set(cacheKey, data, SHOW_IDENTITY_TTL);
      return data;
    }
  }
  if (malId) {
    for (const { id, detail } of details) {
      const detailMal = detail?.mal_id;
      if (detailMal && Number(detailMal) === Number(malId)) {
        const data = {
          animeId: id,
          title: detail.title?.english || detail.title?.romaji || id,
          anilistId: Number(anilistId),
          malId: Number(detailMal),
          subbed: Number.isFinite(detail.subbed) ? detail.subbed : null,
          dubbed: Number.isFinite(detail.dubbed) ? detail.dubbed : null,
          episodesCount: Number.isFinite(detail.episodes) ? detail.episodes : null,
          matchType: "mal",
          matchScore: 0.9
        };
        set(cacheKey, data, SHOW_IDENTITY_TTL);
        return data;
      }
    }
  }
  throw new Error(`No confirmed reanime match for AniList ${anilistId}`);
}
__name(resolveSeries, "resolveSeries");
__name5(resolveSeries, "resolveSeries");
async function fetchEpisodesList(animeId, limit = 2e3) {
  const data = await fetch(`${BASE}/api/v1/anime/${animeId}/episodes?${new URLSearchParams({ limit })}`, { headers: H }).then(async (r) => {
    const _raw = await r.text();
    if (!r.ok) {
      const _e = new Error(`reanime episodes ${r.status}`);
      _e.rawBody = _raw;
      throw _e;
    }
    try {
      return JSON.parse(_raw);
    } catch (_pe) {
      _pe.rawBody = _raw;
      throw _pe;
    }
  });
  return Array.isArray(data?.data) ? data.data : [];
}
__name(fetchEpisodesList, "fetchEpisodesList");
__name5(fetchEpisodesList, "fetchEpisodesList");
async function fetchAnizip(anilistId) {
  return fetch(`${ANIZIP2}?anilist_id=${anilistId}`).then((r) => r.json()).catch(() => null);
}
__name(fetchAnizip, "fetchAnizip");
__name5(fetchAnizip, "fetchAnizip");
function mergeEpisode(anilistId, ep, meta, audio) {
  const number = ep.episode_number;
  return {
    id: `watch/reanime/${anilistId}/${audio}/reanime-${number}`,
    number,
    title: meta?.title?.en || meta?.title?.["x-jat"] || ep.title || `Episode ${number}`,
    titleJapanese: meta?.title?.ja || ep.title_japanese || null,
    titleRomanji: meta?.title?.["x-jat"] || ep.title_romanji || null,
    image: meta?.image || ep.thumbnail || null,
    airDate: meta?.airdate || ep.aired || null,
    duration: meta?.runtime ? meta.runtime * 60 : ep.duration ? ep.duration * 60 : null,
    score: null,
    filler: ep.is_filler ?? meta?.filler ?? false,
    recap: ep.is_recap ?? false,
    description: meta?.overview || ep.description || null,
    audio
  };
}
__name(mergeEpisode, "mergeEpisode");
__name5(mergeEpisode, "mergeEpisode");
function json3(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" } });
}
__name(json3, "json3");
__name5(json3, "json");
async function handleEpisodes3(anilistId, url) {
  const series = await resolveSeries(anilistId);
  const [reanimeEps, anizip] = await Promise.all([
    fetchEpisodesList(series.animeId),
    fetchAnizip(anilistId)
  ]);
  if (!reanimeEps.length) return json3({ error: `No reanime episodes found for AniList ID ${anilistId} (slug ${series.animeId})` }, 404);
  const episodes = reanimeEps.map((ep) => {
    const meta = anizip?.episodes?.[String(ep.episode_number)] ?? null;
    return mergeEpisode(anilistId, ep, meta, "sub");
  }).sort((a, b) => a.number - b.number);
  return json3({
    anime: series.title,
    anilistId: Number(anilistId),
    malId: series.malId,
    animeId: series.animeId,
    episodes,
    pagination: { currentPage: 1, lastPage: 1, hasNextPage: false }
  });
}
__name(handleEpisodes3, "handleEpisodes3");
__name5(handleEpisodes3, "handleEpisodes");
async function resolveStream3(anilistId, audio, ep) {
  const series = await resolveSeries(anilistId);
  const title2 = series.title;
  const slug = series.animeId;
  const order = { "HD-2": 0, "HD-1": 1 };
  const byPrio = /* @__PURE__ */ __name((arr) => arr.slice().sort((a, b) => (order[a.serverName] ?? 9) - (order[b.serverName] ?? 9)), "byPrio");
  const [watchRes, flixRes] = await Promise.allSettled([
    fetch(`${BASE}/api/watch/${slug}/${ep}`, { headers: H }).then(async (r) => {
      const _raw = await r.text();
      if (!r.ok) {
        const _e = new Error(`watch ${r.status}`);
        _e.rawBody = _raw;
        throw _e;
      }
      try {
        return JSON.parse(_raw);
      } catch (_pe) {
        _pe.rawBody = _raw;
        throw _pe;
      }
    }),
    fetch(`${BASE}/api/flix/${anilistId}/${ep}`, { headers: H }).then(async (r) => {
      const _raw = await r.text();
      if (!r.ok) {
        const _e = new Error(`flix ${r.status}`);
        _e.rawBody = _raw;
        throw _e;
      }
      try {
        return JSON.parse(_raw);
      } catch (_pe) {
        _pe.rawBody = _raw;
        throw _pe;
      }
    })
  ]);
  const watchData = watchRes.status === "fulfilled" ? watchRes.value : null;
  const flixData = flixRes.status === "fulfilled" ? flixRes.value : null;
  const links = [...watchData?.episode_links ?? []];
  if (flixData?.success && flixData?.servers) {
    const seen = new Set(links.map((s) => s["$id"]));
    for (const s of flixData.servers) {
      if (!seen.has(s["$id"])) links.push(s);
    }
  }
  const audioTypes = audio === "sub" ? ["sub", "s-sub"] : ["dub", "s-dub"];
  const servers = byPrio(links.filter((s) => audioTypes.includes(s.dataType)));
  if (!servers.length) throw Object.assign(new Error(`No ${audio} servers for "${title2}" ep ${ep}`), { status: 404 });
  const seenServers = /* @__PURE__ */ new Set();
  const uniqueServers = servers.filter((s) => {
    const key = `${s.serverName}:${s.dataType}:${s.dataLink}`;
    if (seenServers.has(key)) return false;
    seenServers.add(key);
    return true;
  });
  const decrypted = await Promise.all(uniqueServers.map(async (server, index) => {
    try {
      const embedRes = await fetch(server.dataLink, { headers: { ...H, Referer: `${BASE}/` } });
      if (!embedRes.ok) throw new Error(`Embed fetch failed: ${embedRes.status}`);
      const stream = await extractFlixcloud(await embedRes.text(), { apiBase: FLIX, headers: H, referer: `${BASE}/` });
      return { server, stream, index };
    } catch (error) {
      return { server, error: error.message, index };
    }
  }));
  const streams = decrypted.filter((item) => item.stream?.url);
  if (!streams.length) {
    const error = decrypted.find((item) => item.error)?.error || "No decrypted streams";
    throw Object.assign(new Error(error), { status: 502 });
  }
  return { title: title2, slug, watchData, stream: streams[0].stream, server: streams[0].server.serverName, servers: uniqueServers, streams, failedServers: decrypted.filter((item) => item.error) };
}
__name(resolveStream3, "resolveStream3");
__name5(resolveStream3, "resolveStream");
async function handleWatch3(anilistId, audio, epNum, origin) {
  if (audio !== "sub" && audio !== "dub") return json3({ error: "audio must be sub or dub" }, 400);
  const ep = parseInt(epNum);
  if (isNaN(ep)) return json3({ error: `Invalid episode: ${epNum}` }, 400);
  let resolved2;
  try {
    resolved2 = await resolveStream3(anilistId, audio, ep);
  } catch (e) {
    return json3({ error: e.message, "Raw-ERROR": e.rawBody ?? null, stack: e.stack }, e.status ?? 500);
  }
  const { title: title2, slug, watchData, stream, server, servers, streams, failedServers } = resolved2;
  const seenStreamUrls = /* @__PURE__ */ new Set();
  const cleanStreams = streams.map(({ server: source, stream: item, index }) => ({
    server: source.serverName,
    audio: source.dataType,
    index,
    url: item.url,
    type: "hls",
    embed: source.dataLink,
    subtitles: item.subtitles ?? [],
    thumbnails_vtt: item.thumbnails_vtt ?? null,
    video_title: item.video_title ?? null,
    intro: item.intro_chapter ?? null,
    outro: item.outro_chapter ?? null
  })).filter((item) => {
    if (seenStreamUrls.has(item.url)) return false;
    seenStreamUrls.add(item.url);
    return true;
  });
  const embeds = servers.map((s) => ({ name: s.serverName, type: s.dataType, url: s.dataLink }));
  return json3({
    anime: title2,
    slug,
    ep,
    audio,
    server,
    stream_url: stream.url,
    streams: cleanStreams,
    subtitles: stream.subtitles,
    thumbnails_vtt: stream.thumbnails_vtt,
    video_title: stream.video_title,
    intro: stream.intro_chapter,
    outro: stream.outro_chapter,
    intro_start: watchData?.intro_start ?? null,
    intro_end: watchData?.intro_end ?? null,
    outro_start: watchData?.outro_start ?? null,
    outro_end: watchData?.outro_end ?? null,
    embeds,
    allServers: servers.map((s) => ({ name: s.serverName, type: s.dataType, embed: s.dataLink })),
    failedServers: failedServers.map((s) => ({ name: s.server.serverName, type: s.server.dataType, error: s.error }))
  });
}
__name(handleWatch3, "handleWatch3");
__name5(handleWatch3, "handleWatch");
async function handleStream3(anilistId, audio, epNum) {
  if (audio !== "sub" && audio !== "dub") return json3({ error: "audio must be sub or dub" }, 400);
  const ep = parseInt(epNum);
  if (isNaN(ep)) return json3({ error: `Invalid episode: ${epNum}` }, 400);
  let resolved2;
  try {
    resolved2 = await resolveStream3(anilistId, audio, ep);
  } catch (e) {
    return json3({ error: e.message, "Raw-ERROR": e.rawBody ?? null, stack: e.stack }, e.status ?? 500);
  }
  return new Response(null, {
    status: 302,
    headers: {
      "Location": resolved2.stream.url,
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "no-store"
    }
  });
}
__name(handleStream3, "handleStream3");
__name5(handleStream3, "handleStream");
async function handleProxy3(url) {
  const target = url.searchParams.get("url");
  const referer = url.searchParams.get("referer") ?? `${FLIX}/`;
  if (!target) return json3({ error: "Missing required ?url= param" }, 400);
  let targetUrl;
  try {
    targetUrl = new URL(target);
  } catch {
    return json3({ error: "Invalid url param" }, 400);
  }
  const upstream = await fetch(target, {
    headers: {
      "User-Agent": UA5,
      "Accept": "*/*",
      "Accept-Language": "en-US,en;q=0.9",
      "Referer": referer,
      "Sec-Fetch-Dest": "empty",
      "Sec-Fetch-Mode": "cors",
      "Sec-Fetch-Site": "cross-site"
    }
  });
  const ct = upstream.headers.get("Content-Type") ?? "";
  const isM3U8 = ct.includes("mpegurl") || ct.includes("x-mpegurl") || targetUrl.pathname.endsWith(".m3u8") || targetUrl.pathname.endsWith(".m3u");
  const corsHeaders = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "*" };
  if (!upstream.ok) {
    return new Response(await upstream.text(), { status: upstream.status, headers: { "Content-Type": ct || "text/plain", ...corsHeaders } });
  }
  if (isM3U8) {
    const text = await upstream.text();
    const rewritten = rewriteM3U8(text, target, url.origin);
    return new Response(rewritten, { status: 200, headers: { "Content-Type": "application/vnd.apple.mpegurl", ...corsHeaders } });
  }
  return new Response(upstream.body, { status: upstream.status, headers: { "Content-Type": ct || "application/octet-stream", ...corsHeaders } });
}
__name(handleProxy3, "handleProxy3");
__name5(handleProxy3, "handleProxy");
var reanime_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    const path = url.pathname;
    if (request2.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET,OPTIONS", "Access-Control-Allow-Headers": "*" } });
    }
    try {
      let m;
      if (path === "/healthz") return json3({ status: "ok", provider: "reanime" });
      if (path === "/proxy") return await handleProxy3(url);
      m = path.match(/^\/episodes\/(\d+)$/);
      if (m) return await handleEpisodes3(m[1], url);
      m = path.match(/^\/watch\/(\d+)\/(sub|dub)\/(\d+)$/);
      if (m) return await handleWatch3(m[1], m[2], m[3], url.origin);
      m = path.match(/^\/stream\/(\d+)\/(sub|dub)\/(\d+)$/);
      if (m) return await handleStream3(m[1], m[2], m[3]);
      return json3({ error: "Not found", routes: ["GET /episodes/:anilistId", "GET /watch/:anilistId/sub|dub/:ep", "GET /stream/:anilistId/sub|dub/:ep", "GET /proxy?url=&referer="] }, 404);
    } catch (err) {
      return json3({ error: err.message, "Raw-ERROR": err.rawBody ?? null, ...err.debug ? { debug: err.debug } : {}, stack: err.stack }, 500);
    }
  }
};
async function getEpisodes3(anilistId, ctx = {}) {
  const series = await resolveSeries(anilistId, ctx);
  const anizip = ctx.anizip !== void 0 ? ctx.anizip : await fetchAnizip(anilistId);
  const reanimeEps = await fetchEpisodesList(series.animeId);
  if (!reanimeEps.length) throw new Error(`No reanime episodes found for AniList ${anilistId} (slug ${series.animeId})`);
  const hasSub = series.subbed == null || series.subbed > 0;
  const dubCount = series.dubbed ?? 0;
  const sub = [], dub = [];
  for (const ep of reanimeEps) {
    const meta = anizip?.episodes?.[String(ep.episode_number)] ?? null;
    if (hasSub) sub.push(mergeEpisode(anilistId, ep, meta, "sub"));
    if (dubCount > 0 && ep.episode_number <= dubCount) dub.push(mergeEpisode(anilistId, ep, meta, "dub"));
  }
  sub.sort((a, b) => a.number - b.number);
  dub.sort((a, b) => a.number - b.number);
  return {
    meta: { title: series.title, malId: series.malId, animeId: series.animeId },
    episodes: { sub, dub }
  };
}
__name(getEpisodes3, "getEpisodes3");
__name5(getEpisodes3, "getEpisodes");
var reanime_default2 = reanime_default;

// providers/anikoto.js
var ANIKOTO = "https://anikototv.to";
var MAPPER = "https://mapper.nekostream.site/api/mal";
var ANIZIP3 = "https://api.ani.zip/mappings";
var SPOOF_REF = "https://hianimes.re/";
var UA6 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var LANG_MAP = {
  en: "en",
  english: "en",
  ja: "ja",
  japanese: "ja",
  fr: "fr",
  french: "fr",
  de: "de",
  german: "de",
  es: "es",
  spanish: "es",
  pt: "pt",
  portuguese: "pt"
};
function normalize2(s) {
  return (s || "").toLowerCase().replace(/[^a-z0-9]/g, "");
}
__name(normalize2, "normalize");
async function httpGet(url, headers = {}) {
  const res = await fetch(url, { headers: { "User-Agent": UA6, Accept: "text/html,*/*", ...headers } });
  if (!res.ok) {
    const _raw = await res.text().catch(() => null);
    const _e = new Error(`HTTP ${res.status} fetching ${url}`);
    _e.rawBody = _raw;
    throw _e;
  }
  return res.text();
}
__name(httpGet, "httpGet");
async function getJSON(url, headers = {}) {
  const res = await fetch(url, { headers: { "User-Agent": UA6, Accept: "application/json,*/*", ...headers } });
  if (!res.ok) {
    const _raw = await res.text().catch(() => null);
    const _e = new Error(`HTTP ${res.status} fetching ${url}`);
    _e.rawBody = _raw;
    throw _e;
  }
  return res.json();
}
__name(getJSON, "getJSON");
var MODIFIERS = [
  "ova",
  "movie",
  "special",
  "specials",
  "tales",
  "journal",
  "part",
  "season",
  "kanwa",
  "spin-off",
  "theatre"
];
function scoreCandidate(cand, primaryEn, primaryRom, synonyms) {
  let score = 0;
  const candNameNorm = normalize2(cand.name);
  const candJpNorm = normalize2(cand.jp);
  const candSlugNorm = normalize2(cand.slug);
  const normEn = normalize2(primaryEn);
  const normRom = normalize2(primaryRom);
  if (normEn && candNameNorm === normEn) score += 1e3;
  if (normRom && candNameNorm === normRom) score += 900;
  if (normRom && candJpNorm === normRom) score += 800;
  const targetText = `${primaryEn || ""} ${primaryRom || ""} ${(synonyms || []).join(" ")}`.toLowerCase();
  for (const mod of MODIFIERS) {
    const candHasMod = candNameNorm.includes(mod) || candSlugNorm.includes(mod);
    const targetHasMod = targetText.includes(mod);
    if (candHasMod && !targetHasMod) {
      score -= 300;
    }
  }
  for (const t of [primaryEn, primaryRom, ...synonyms || []]) {
    const normT = normalize2(t);
    if (!normT || normT.length < 3) continue;
    if (candNameNorm === normT) score += 200;
    else if (candNameNorm.startsWith(normT) || normT.startsWith(candNameNorm)) score += 80;
    else if (candNameNorm.includes(normT) || normT.includes(candNameNorm)) score += 40;
    if (candJpNorm && candJpNorm === normT) score += 100;
  }
  const lengthDiff = Math.abs(candNameNorm.length - (normEn || normRom || "").length);
  score -= lengthDiff * 2;
  return score;
}
__name(scoreCandidate, "scoreCandidate");
async function searchAnikoto(query) {
  const searchHtml = await httpGet(`${ANIKOTO}/filter?keyword=${encodeURIComponent(query)}`, { Referer: `${ANIKOTO}/` });
  const candidates = [];
  const re = /<a\s+class="name d-title"\s+href="https:\/\/anikototv\.to\/watch\/([^"/]+)(?:\/ep-\d+)?"[^>]*data-jp="([^"]*)"[^>]*>([\s\S]*?)<\/a>/g;
  let m;
  while ((m = re.exec(searchHtml)) !== null) {
    const slug = m[1];
    const jp = m[2].trim();
    const name = m[3].replace(/<[^>]*>/g, "").trim();
    candidates.push({ slug, name, jp });
  }
  if (!candidates.length) {
    const reFallback = /<a\s+href="https:\/\/anikototv\.to\/watch\/([^"/]+)(?:\/ep-\d+)?"[^>]*>([\s\S]*?)<\/a>/g;
    while ((m = reFallback.exec(searchHtml)) !== null) {
      candidates.push({ slug: m[1], name: m[1], jp: "" });
    }
  }
  const seen = /* @__PURE__ */ new Set();
  return candidates.filter((c) => {
    if (seen.has(c.slug)) return false;
    seen.add(c.slug);
    return true;
  });
}
__name(searchAnikoto, "searchAnikoto");
async function findAnikotoShow(media) {
  const primaryEn = media.title?.english;
  const primaryRom = media.title?.romaji;
  const synonyms = media.synonyms || [];
  const keywords = [...new Set([primaryEn, primaryRom, ...synonyms].filter(Boolean))];
  const allCandidatesMap = /* @__PURE__ */ new Map();
  for (const k of keywords.slice(0, 5)) {
    const res = await searchAnikoto(k).catch(() => []);
    for (const c of res) {
      allCandidatesMap.set(c.slug, c);
    }
  }
  const candidates = Array.from(allCandidatesMap.values());
  if (!candidates.length) {
    throw new Error(`No results found on Anikoto for: ${primaryEn || primaryRom}`);
  }
  const scored = candidates.map((c) => ({
    ...c,
    score: scoreCandidate(c, primaryEn, primaryRom, synonyms)
  })).sort((a, b) => b.score - a.score);
  const chosen = scored[0];
  const watchHtml = await httpGet(`${ANIKOTO}/watch/${chosen.slug}`, { Referer: `${ANIKOTO}/` });
  const showIdMatch = watchHtml.match(/data-id="(\d+)"/);
  if (!showIdMatch) throw new Error(`Could not find show ID for slug: ${chosen.slug}`);
  return { slug: chosen.slug, showId: showIdMatch[1], title: chosen.name };
}
__name(findAnikotoShow, "findAnikotoShow");
function mapTrack(t, source) {
  const label = t.label ?? "";
  const langKey = label.toLowerCase().split(" ")[0];
  return {
    url: t.file,
    label: label || "English",
    srclang: LANG_MAP[langKey] ?? "en",
    default: t.default ?? false,
    source
  };
}
__name(mapTrack, "mapTrack");
async function extractEmbedSource(embedUrl) {
  try {
    return await extractMegaPlayDetails(embedUrl, { userAgent: UA6, referer: SPOOF_REF });
  } catch (e) {
    return null;
  }
}
__name(extractEmbedSource, "extractEmbedSource");
async function getEpisodes(anilistId, ctx = {}) {
  const media = ctx.media || await getMedia(anilistId);
  if (!media) throw new Error(`Could not resolve media for AniList ID: ${anilistId}`);
  const [show, anizipRes] = await Promise.all([
    findAnikotoShow(media),
    ctx.anizip ? Promise.resolve(ctx.anizip) : getJSON(`${ANIZIP3}?anilist_id=${anilistId}`).catch(() => null)
  ]);
  const listJson = await getJSON(`${ANIKOTO}/ajax/episode/list/${show.showId}`, {
    "X-Requested-With": "XMLHttpRequest",
    Referer: `${ANIKOTO}/watch/${show.slug}`
  });
  const html2 = listJson.result || "";
  const sub = [];
  const dub = [];
  let firstMal = media.idMal || null;
  const re = /<a\s+[^>]*data-id="([^"]*)"[^>]*>([\s\S]*?)<\/a>/g;
  let m;
  while ((m = re.exec(html2)) !== null) {
    const tag = m[0];
    const inner = m[2];
    const getAttr = /* @__PURE__ */ __name((attr2) => {
      const x = tag.match(new RegExp(`data-${attr2}="([^"]*)"`));
      return x ? x[1] : "";
    }, "getAttr");
    const numStr = getAttr("num");
    if (!numStr) continue;
    const num = parseInt(numStr);
    const hasSub = getAttr("sub") === "1";
    const hasDub = getAttr("dub") === "1";
    const malAttr = getAttr("mal");
    if (!firstMal && malAttr) firstMal = parseInt(malAttr);
    const titleMatch = inner.match(/<span class="d-title"[^>]*>([\s\S]*?)<\/span>/);
    const parsedTitle = titleMatch ? titleMatch[1].replace(/<[^>]*>/g, "").trim() : "";
    const epTitle = parsedTitle || `Episode ${num}`;
    const azEp = anizipRes?.episodes?.[String(num)] ?? {};
    const img = azEp.image || null;
    const desc = azEp.overview || azEp.summary || null;
    const airDate = azEp.airDate || azEp.airdate || null;
    const base = {
      number: num,
      title: epTitle,
      duration: null,
      filler: false,
      uncensored: false,
      description: desc,
      image: img,
      airDate
    };
    if (hasSub) {
      sub.push({
        id: `watch/anikoto/${anilistId}/sub/anikoto-${num}`,
        ...base,
        audio: "sub"
      });
    }
    if (hasDub) {
      dub.push({
        id: `watch/anikoto/${anilistId}/dub/anikoto-${num}`,
        ...base,
        audio: "dub"
      });
    }
  }
  sub.sort((a, b) => a.number - b.number);
  dub.sort((a, b) => a.number - b.number);
  return {
    meta: {
      title: show.title,
      slug: show.slug,
      malId: firstMal,
      source: "anikoto"
    },
    episodes: { sub, dub }
  };
}
__name(getEpisodes, "getEpisodes");
async function handleWatch2(anilistId, audio, epNum, ctx = {}) {
  if (audio !== "sub" && audio !== "dub") {
    return jsonResponse({ error: "audio must be sub or dub" }, 400);
  }
  const media = ctx.media || await getMedia(anilistId);
  if (!media) {
    return jsonResponse({ error: `Could not resolve media for AniList ID: ${anilistId}` }, 400);
  }
  const show = await findAnikotoShow(media);
  const listJson = await getJSON(`${ANIKOTO}/ajax/episode/list/${show.showId}`, {
    "X-Requested-With": "XMLHttpRequest",
    Referer: `${ANIKOTO}/watch/${show.slug}`
  });
  const html2 = listJson.result || "";
  let targetEp = null;
  const re = /<a\s+[^>]*data-id="([^"]*)"[^>]*>/g;
  let m;
  while ((m = re.exec(html2)) !== null) {
    const tag = m[0];
    const getAttr = /* @__PURE__ */ __name((attr2) => {
      const x = tag.match(new RegExp(`data-${attr2}="([^"]*)"`));
      return x ? x[1] : "";
    }, "getAttr");
    if (parseInt(getAttr("num")) === epNum) {
      targetEp = {
        ids: getAttr("ids"),
        mal: getAttr("mal"),
        slug: getAttr("slug"),
        timestamp: getAttr("timestamp")
      };
      break;
    }
  }
  if (!targetEp?.ids) {
    return jsonResponse({ error: `Episode ${epNum} not found for show: ${show.title}` }, 404);
  }
  const malIdNum = media.idMal || (targetEp.mal ? parseInt(targetEp.mal) : null);
  const [serverDataRes, mapperRes] = await Promise.allSettled([
    getJSON(`${ANIKOTO}/ajax/server/list?servers=${encodeURIComponent(targetEp.ids)}`, {
      "X-Requested-With": "XMLHttpRequest",
      Referer: `${ANIKOTO}/`
    }),
    targetEp.mal && targetEp.slug && targetEp.timestamp ? getJSON(`${MAPPER}/${targetEp.mal}/${targetEp.slug}/${targetEp.timestamp}`, { Referer: `${ANIKOTO}/` }) : Promise.resolve(null)
  ]);
  const serverData = serverDataRes.status === "fulfilled" ? serverDataRes.value : null;
  const mapperData = mapperRes.status === "fulfilled" ? mapperRes.value : null;
  const serverHtml = serverData?.result || "";
  const serverItems = [];
  const downloadItems = [];
  const typeRe = /<div class="type" data-type="([^"]+)">([\s\S]*?)<\/ul>\s*<\/div>/g;
  let typeM;
  while ((typeM = typeRe.exec(serverHtml)) !== null) {
    const typeName = typeM[1];
    for (const li of typeM[2].matchAll(/<li\s+([^>]*data-link-id[^>]*)>([\s\S]*?)<\/li>/g)) {
      const linkId = li[1].match(/data-link-id="([^"]+)"/)?.[1];
      const name = li[2].replace(/<[^>]+>/g, "").trim();
      if (!linkId) continue;
      if (typeName === "dl" || name.toLowerCase().includes("download") || name.toLowerCase().includes("kiwi")) {
        downloadItems.push({ linkId, name });
      } else if (typeName === audio) {
        serverItems.push({ linkId, name });
      }
    }
  }
  if (mapperData) {
    for (const [sKey, sObj] of Object.entries(mapperData)) {
      if (sKey === "status") continue;
      const cleanName = sKey.replace(/[-_]+$/, "").trim();
      if (sObj?.[audio]?.url) {
        serverItems.push({ linkId: sObj[audio].url, name: cleanName });
      }
      if (sObj?.[audio]?.download) {
        for (const [dLabel, dUrl] of Object.entries(sObj[audio].download)) {
          if (dUrl && typeof dUrl === "string") {
            downloadItems.push({ url: dUrl, name: cleanName });
          }
        }
      }
    }
  }
  const streams = [];
  const subtitles = [];
  const downloads = [];
  const serverSeen = /* @__PURE__ */ new Set();
  const subSeen = /* @__PURE__ */ new Set();
  const dlSeen = /* @__PURE__ */ new Set();
  for (const item of serverItems) {
    if (serverSeen.has(item.name)) continue;
    serverSeen.add(item.name);
    const resolved2 = item.linkId.startsWith("http") ? { result: { url: item.linkId } } : await getJSON(`${ANIKOTO}/ajax/server?get=${encodeURIComponent(item.linkId)}`, {
      "X-Requested-With": "XMLHttpRequest",
      Referer: `${ANIKOTO}/`
    }).catch(() => null);
    const embedUrl = resolved2?.result?.url;
    if (!embedUrl) continue;
    let serverIntro = { start: 0, end: 0 };
    let serverOutro = { start: 0, end: 0 };
    if (resolved2?.result?.skip_data?.intro?.length === 2) {
      const [s, e] = resolved2.result.skip_data.intro;
      if (s || e) serverIntro = { start: Number(s) || 0, end: Number(e) || 0 };
    }
    if (resolved2?.result?.skip_data?.outro?.length === 2) {
      const [s, e] = resolved2.result.skip_data.outro;
      if (s || e) serverOutro = { start: Number(s) || 0, end: Number(e) || 0 };
    }
    const hlsSources = [];
    if (embedUrl.includes("#aHR0c")) {
      const b64 = embedUrl.split("#")[1];
      try {
        const decodedUrl = atob(b64);
        if (decodedUrl.includes(".m3u8")) {
          hlsSources.push({ url: decodedUrl, variant: null });
        }
      } catch (e) {
      }
    }
    const extracted = await extractEmbedSource(embedUrl);
    const itemSubs = [];
    if (extracted?.sources?.length) {
      for (const source of extracted.sources) {
        if (!hlsSources.some((item2) => item2.url === source.url)) hlsSources.push(source);
      }
      for (const t of extracted.tracks ?? []) {
        const mapped = mapTrack(t, item.name);
        itemSubs.push(mapped);
        if (!subSeen.has(mapped.url)) {
          subSeen.add(mapped.url);
          subtitles.push(mapped);
        }
      }
      if (extracted.intro?.start || extracted.intro?.end) {
        serverIntro = { start: Number(extracted.intro.start) || 0, end: Number(extracted.intro.end) || 0 };
      }
      if (extracted.outro?.start || extracted.outro?.end) {
        serverOutro = { start: Number(extracted.outro.start) || 0, end: Number(extracted.outro.end) || 0 };
      }
    }
    if (hlsSources.length) {
      for (const source of hlsSources) {
        const streamObj = {
          url: source.url,
          type: "hls",
          server: item.name,
          embedUrl,
          referer: extracted?.origin ? `${extracted.origin}/` : `${new URL(embedUrl).origin}/`,
          subtitles: itemSubs,
          priority: streams.length ? 4 : 5,
          isActive: streams.length === 0
        };
        if (source.variant) streamObj.variant = source.variant;
        if (serverIntro.start || serverIntro.end) streamObj.intro = serverIntro;
        if (serverOutro.start || serverOutro.end) streamObj.outro = serverOutro;
        streams.push(streamObj);
      }
      streams.push({
        url: embedUrl,
        type: "embed",
        server: item.name,
        referer: `${new URL(embedUrl).origin}/`,
        priority: 4,
        isActive: false
      });
    } else {
      const streamObj = {
        url: embedUrl,
        type: "embed",
        server: item.name,
        referer: `${new URL(embedUrl).origin}/`,
        priority: 4,
        isActive: streams.length === 0
      };
      if (serverIntro.start || serverIntro.end) streamObj.intro = serverIntro;
      if (serverOutro.start || serverOutro.end) streamObj.outro = serverOutro;
      streams.push(streamObj);
    }
  }
  for (const dl of downloadItems) {
    let dlUrl = dl.url;
    if (!dlUrl && dl.linkId) {
      const resolved2 = await getJSON(`${ANIKOTO}/ajax/server?get=${encodeURIComponent(dl.linkId)}`, {
        "X-Requested-With": "XMLHttpRequest",
        Referer: `${ANIKOTO}/`
      }).catch(() => null);
      dlUrl = resolved2?.result?.url;
    }
    if (dlUrl && !dlSeen.has(dlUrl)) {
      dlSeen.add(dlUrl);
      downloads.push({
        url: dlUrl,
        label: dl.name
      });
    }
  }
  return jsonResponse({
    anilistId: parseInt(anilistId),
    malId: malIdNum,
    episode: epNum,
    audio,
    streams,
    subtitles,
    downloads,
    headers: {
      "User-Agent": UA6,
      "Referer": streams[0]?.referer || "https://anikototv.to/"
    }
  });
}
__name(handleWatch2, "handleWatch");
function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}
__name(jsonResponse, "jsonResponse");
var anikoto_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    const path = url.pathname;
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    try {
      let m = path.match(/^\/watch\/anikoto\/(\d+)\/(sub|dub)\/anikoto-(\d+)\/?$/);
      if (m) return await handleWatch2(m[1], m[2], parseInt(m[3]));
      m = path.match(/^\/episodes\/anikoto\/(\d+)\/?$/);
      if (m) {
        const data = await getEpisodes(parseInt(m[1]));
        return jsonResponse(data);
      }
      return jsonResponse({ error: "Not found" }, 404);
    } catch (err) {
      return jsonResponse({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// providers/animegg.js
var BASE2 = "https://www.animegg.org";
async function search(query) {
  const html2 = await fetchHtml(`${BASE2}/search/?q=${encodeURIComponent(query)}`);
  const results = [];
  for (const m of html2.matchAll(/<a\b[^>]*class=["'][^"']*\bmse\b[^"']*["'][^>]*>[\s\S]*?<\/a>/gi)) {
    const tag = m[0].match(/<a\b[^>]*>/i)?.[0] ?? "";
    const href = attr(tag, "href");
    const slug = href.match(/^\/series\/([^/?#]+)/)?.[1];
    if (!slug) continue;
    const strong = m[0].match(/<strong[^>]*>([\s\S]*?)<\/strong>/i)?.[1];
    results.push({ slug, text: strong ? stripTags(strong) : slug.replace(/-/g, " ") });
  }
  return results;
}
__name(search, "search");
async function scrapeSeries(slug) {
  const html2 = await fetchHtml(`${BASE2}/series/${slug}`);
  const episodes = [];
  for (const m of html2.matchAll(/<li\b[^>]*>([\s\S]*?)<\/li>/gi)) {
    const block = m[1];
    if (!/\banm_det_pop\b/.test(block)) continue;
    const link = block.match(/<a\b[^>]*class=["'][^"']*anm_det_pop[^"']*["'][^>]*>/i)?.[0] ?? "";
    const href = attr(link, "href").replace(/#.*$/, "").replace(/^\//, "");
    const strong = stripTags(block.match(/<strong[^>]*>([\s\S]*?)<\/strong>/i)?.[1] ?? "");
    const rangeMatch = strong.match(/(\d+)-(\d+)\s*$/);
    const numMatch = rangeMatch || strong.match(/(\d+)\s*$/);
    if (!numMatch || !href) continue;
    const number = parseInt(numMatch[1]);
    const title = stripTags(block.match(/<i\b[^>]*class=["'][^"']*anititle[^"']*["'][^>]*>([\s\S]*?)<\/i>/i)?.[1] ?? "") || strong;
    const audio = [];
    if (/\bbtn-subbed\b/.test(block)) audio.push("sub");
    if (/\bbtn-dubbed\b/.test(block)) audio.push("dub");
    episodes.push({ number, title, epSlug: href, hasSub: audio.includes("sub"), hasDub: audio.includes("dub") });
  }
  episodes.sort((a, b) => a.number - b.number);
  const seen = /* @__PURE__ */ new Set();
  return episodes.filter((e) => seen.has(e.number) ? false : (seen.add(e.number), true));
}
__name(scrapeSeries, "scrapeSeries");
async function scrapeEmbed(embedId) {
  const html2 = await fetchHtml(`${BASE2}/embed/${embedId}`, { Referer: BASE2 });
  const m = html2.match(/var\s+videoSources\s*=\s*(\[[\s\S]*?\]);/);
  if (!m) return [];
  let parsed = [];
  try {
    const asJson = m[1].replace(/([{,]\s*)([a-zA-Z_][a-zA-Z0-9_]*)\s*:/g, '$1"$2":').replace(/:\s*'([^']*)'/g, ': "$1"');
    parsed = JSON.parse(asJson);
  } catch {
    return [];
  }
  return parsed.map((s) => {
    let backup = null;
    if (s.bk) {
      try {
        backup = decodeURIComponent(atob(s.bk));
      } catch {
        backup = null;
      }
    }
    return {
      quality: s.label || "unknown",
      url: s.file ? s.file.startsWith("http") ? s.file : `${BASE2}${s.file}` : "",
      backup
    };
  }).filter((s) => s.url);
}
__name(scrapeEmbed, "scrapeEmbed");
async function scrapeEpisodeWatch(epSlug, audio) {
  const html2 = await fetchHtml(`${BASE2}/${epSlug}`, { Referer: BASE2 });
  const title = stripTags(html2.match(/<div\b[^>]*class=["'][^"']*info[^"']*["'][^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)<\/a>/i)?.[1] ?? "");
  const tabs = [];
  for (const m of html2.matchAll(/<a\b[^>]*data-toggle=["']tab["'][^>]*>/gi)) {
    const tag = m[0];
    const embedId = attr(tag, "data-id");
    const server = attr(tag, "data-mirror") || "AnimeGG";
    const version = attr(tag, "data-version") || "subbed";
    if (!embedId) continue;
    const normalized = version.startsWith("dub") ? "dub" : "sub";
    if (audio === "all" || normalized === audio) {
      tabs.push({ embedId, embedUrl: `${BASE2}/embed/${embedId}`, server, normalized });
    }
  }
  const results = await Promise.allSettled(tabs.map(async (tab, i) => {
    const sources = await scrapeEmbed(tab.embedId);
    const streams = sources.map((s, j) => ({
      url: s.url,
      type: s.url.includes(".m3u8") ? "hls" : "mp4",
      quality: s.quality,
      backup: s.backup,
      audio: tab.normalized,
      server: tab.server,
      embed: tab.embedUrl,
      referer: `${new URL(tab.embedUrl).origin}/`,
      priority: tabs.length - i,
      isActive: i === 0 && j === 0
    }));
    streams.push({
      url: tab.embedUrl,
      type: "embed",
      audio: tab.normalized,
      server: `${tab.server}-embed`,
      referer: `${new URL(tab.embedUrl).origin}/`,
      priority: 1,
      isActive: false
    });
    return streams;
  }));
  return { title, streams: results.flatMap((r) => r.status === "fulfilled" ? r.value : []) };
}
__name(scrapeEpisodeWatch, "scrapeEpisodeWatch");
async function searchFn(query) {
  const r1 = await search(query);
  const compact = query.split(/\s+/)[0].replace(/[^a-zA-Z0-9]/g, "");
  if (compact.length >= 4 && compact.toLowerCase() !== query.toLowerCase()) {
    try {
      const r2 = await search(compact);
      const seen = new Set(r1.map((r) => r.slug));
      r2.forEach((r) => {
        if (!seen.has(r.slug)) r1.push(r);
      });
    } catch {
    }
  }
  return r1;
}
__name(searchFn, "searchFn");
async function resolveSeries2(anilistId, ctx = {}) {
  const cacheKey = `np:animegg:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const candidates = await findTopSlugs(titles, searchFn);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const offset = await getPrequelOffset(anilistId).catch(() => 0);
  const isSingleMovie = String(media?.format ?? "").toUpperCase() === "MOVIE" || expected === 1;
  const selected = await selectSeries(candidates, scrapeSeries, expected, media?.status, offset, {
    minScore: isSingleMovie ? 0.9 : 0.65
  });
  if (!selected) throw new Error(`AnimeGG match not found for AniList ${anilistId}`);
  const data = { slug: selected.slug, title: selected.title, mode: selected.mode, offset, score: selected.score };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries2, "resolveSeries");
function buildEpisodeLists(anilistId, series, providerEpisodes, ctx, expected) {
  const sub = [], dub = [];
  for (const src of providerEpisodes) {
    const number = series.mode === "offset" ? src.number - series.offset : src.number;
    if (number < 1) continue;
    if (expected && number > expected) continue;
    const meta = episodeMeta(number, ctx);
    const base = {
      number,
      title: meta.title ?? src.title ?? `Episode ${number}`,
      duration: meta.duration,
      filler: meta.filler,
      uncensored: meta.uncensored,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate,
      sourceNumber: src.number
    };
    if (src.hasSub) sub.push({ ...base, id: `watch/animegg/${anilistId}/sub/animegg-${number}`, audio: "sub" });
    if (src.hasDub) dub.push({ ...base, id: `watch/animegg/${anilistId}/dub/animegg-${number}`, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists, "buildEpisodeLists");
async function getEpisodes2(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const series = await resolveSeries2(anilistId, localCtx);
  const episodes = await scrapeSeries(series.slug);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  return {
    meta: {
      id: series.slug,
      title: series.title,
      source: "animegg",
      matchScore: Number(series.score.toFixed(3)),
      numbering: series.mode,
      episodeOffset: series.mode === "offset" ? series.offset : 0
    },
    episodes: buildEpisodeLists(anilistId, series, episodes, localCtx, expected)
  };
}
__name(getEpisodes2, "getEpisodes");
async function handleWatch4(anilistId, audio, epNum, ctx = {}) {
  const series = await resolveSeries2(anilistId, ctx);
  const providerEp = series.mode === "offset" ? Number(epNum) + series.offset : Number(epNum);
  const episodes = await scrapeSeries(series.slug);
  const ep = episodes.find((e) => e.number === providerEp);
  if (!ep) return json2({ error: `AnimeGG episode ${providerEp} not found` }, 404);
  const watch = await scrapeEpisodeWatch(ep.epSlug, audio);
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), providerEpisode: providerEp, audio, title: watch.title, streams: watch.streams });
}
__name(handleWatch4, "handleWatch");
var animegg_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET,OPTIONS", "Access-Control-Allow-Headers": "*" } });
    }
    try {
      const m = url.pathname.match(/^\/watch\/animegg\/(\d+)\/(sub|dub)\/animegg-(\d+)\/?$/);
      if (m) return await handleWatch4(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, "Raw-ERROR": err.rawBody ?? null, stack: err.stack }, 500);
    }
  }
};

// providers/anineko.js
var BASE3 = "https://anineko.to";
async function search2(query) {
  const html2 = await fetchHtml(`${BASE3}/browser?keyword=${encodeURIComponent(query)}`);
  const results = [];
  for (const m of html2.matchAll(/<a\b[^>]*class=["'][^"']*nv-anime-thumb[^"']*["'][^>]*>[\s\S]*?<\/a>/gi)) {
    const tag = m[0].match(/<a\b[^>]*>/i)?.[0] ?? "";
    const href = attr(tag, "href");
    const slug = href.match(/\/watch\/([^/?#]+)/)?.[1];
    if (!slug) continue;
    const titleMatch = m[0].match(/<(?:h3|[^>]+class=["'][^"']*nv-anime-title[^"']*["'][^>]*)>([\s\S]*?)<\/(?:h3|[^>]+)>/i);
    results.push({ slug, text: titleMatch ? stripTags(titleMatch[1]) : slug.replace(/-/g, " ") });
  }
  return results;
}
__name(search2, "search");
async function scrapeSeries2(slug) {
  const html2 = await fetchHtml(`${BASE3}/watch/${slug}`);
  const episodes = [];
  for (const m of html2.matchAll(/<article\b[^>]*class=["'][^"']*nv-info-episode-item[^"']*["'][^>]*>([\s\S]*?)<\/article>/gi)) {
    const block = m[1];
    const link = block.match(/<a\b[^>]*class=["'][^"']*nv-info-episode-main[^"']*["'][^>]*>/i)?.[0] ?? "";
    const href = attr(link, "href");
    const num = Number(href.match(/\/ep-(\d+)/)?.[1]);
    if (!Number.isFinite(num)) continue;
    const title = stripTags(block.match(/<a\b[^>]*class=["'][^"']*nv-info-episode-main[^"']*["'][^>]*>[\s\S]*?<span[^>]*>([\s\S]*?)<\/span>/i)?.[1] ?? "");
    const badges = [...block.matchAll(/<span\b[^>]*>([\s\S]*?)<\/span>/gi)].map((b) => stripTags(b[1]).toLowerCase());
    episodes.push({
      number: num,
      title: title || `Episode ${num}`,
      epSlug: `ep-${num}`,
      hasSub: badges.includes("sub"),
      hasDub: badges.includes("dub")
    });
  }
  episodes.sort((a, b) => a.number - b.number);
  const seen = /* @__PURE__ */ new Set();
  return episodes.filter((e) => seen.has(e.number) ? false : (seen.add(e.number), true));
}
__name(scrapeSeries2, "scrapeSeries");
async function extractHls(embedUrl) {
  const html2 = await fetchHtml(embedUrl, { Referer: `${BASE3}/` }).catch(() => "");
  const patterns = [
    /const\s+src\s*=\s*["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i,
    /file\s*:\s*["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i,
    /["'](https?:\/\/[^"']+\/master\.m3u8[^"']*)["']/i,
    /["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i
  ];
  for (const pattern of patterns) {
    const m = html2.match(pattern);
    if (m) return decodeEntities(m[1]);
  }
  return null;
}
__name(extractHls, "extractHls");
async function scrapeEpisodeWatch2(seriesSlug, epSlug, audio) {
  const html2 = await fetchHtml(`${BASE3}/watch/${seriesSlug}/${epSlug}`, { Referer: `${BASE3}/watch/${seriesSlug}` });
  const byAudio = { sub: [], dub: [] };
  for (const panel of html2.matchAll(/<div\b[^>]*class=["'][^"']*nv-server-grid[^"']*["'][^>]*data-id=["']([^"']+)["'][^>]*>([\s\S]*?)(?=<div\b[^>]*class=["'][^"']*nv-server-grid|$)/gi)) {
    const rawAudio = panel[1].toLowerCase();
    const panelAudio = rawAudio.includes("dub") ? "dub" : "sub";
    for (const btn of panel[2].matchAll(/data-video=["']([^"']+)["']/gi)) byAudio[panelAudio].push(decodeEntities(btn[1]));
  }
  const audios = audio === "all" ? ["sub", "dub"] : [audio];
  const streams = [];
  await Promise.all(audios.map(async (aud) => {
    const embeds = byAudio[aud] ?? [];
    const resolved2 = await Promise.all(embeds.map(async (embed, i) => {
      const hls = await extractHls(embed);
      return {
        url: hls ?? embed,
        type: hls ? "hls" : "embed",
        embed,
        audio: aud,
        server: "AniNeko",
        priority: embeds.length - i,
        referer: `${new URL(embed).origin}/`,
        isActive: i === 0
      };
    }));
    streams.push(...resolved2);
  }));
  return streams;
}
__name(scrapeEpisodeWatch2, "scrapeEpisodeWatch");
async function resolveSeries3(anilistId, ctx = {}) {
  const cacheKey = `np:anineko:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const candidates = await findTopSlugs(titles, search2);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const offset = await getPrequelOffset(anilistId).catch(() => 0);
  const selected = await selectSeries(candidates, scrapeSeries2, expected, media?.status, offset);
  if (!selected) throw new Error(`AniNeko match not found for AniList ${anilistId}`);
  const data = { slug: selected.slug, title: selected.title, mode: selected.mode, offset, score: selected.score };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries3, "resolveSeries");
function buildEpisodeLists2(anilistId, series, providerEpisodes, ctx, expected) {
  const sub = [], dub = [];
  for (const src of providerEpisodes) {
    const number = series.mode === "offset" ? src.number - series.offset : src.number;
    if (number < 1) continue;
    if (expected && number > expected) continue;
    const meta = episodeMeta(number, ctx);
    const base = {
      number,
      title: meta.title ?? src.title ?? `Episode ${number}`,
      duration: meta.duration,
      filler: meta.filler,
      uncensored: meta.uncensored,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate,
      sourceNumber: src.number
    };
    if (src.hasSub) sub.push({ id: `watch/anineko/${anilistId}/sub/anineko-${number}`, ...base, audio: "sub" });
    if (src.hasDub) dub.push({ id: `watch/anineko/${anilistId}/dub/anineko-${number}`, ...base, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists2, "buildEpisodeLists");
async function getEpisodes4(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const series = await resolveSeries3(anilistId, localCtx);
  const episodes = await scrapeSeries2(series.slug);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  return {
    meta: {
      id: series.slug,
      title: series.title,
      source: "anineko",
      matchScore: Number(series.score.toFixed(3)),
      numbering: series.mode,
      episodeOffset: series.mode === "offset" ? series.offset : 0
    },
    episodes: buildEpisodeLists2(anilistId, series, episodes, localCtx, expected)
  };
}
__name(getEpisodes4, "getEpisodes");
async function handleWatch5(anilistId, audio, epNum, ctx = {}) {
  const series = await resolveSeries3(anilistId, ctx);
  const providerEp = series.mode === "offset" ? Number(epNum) + series.offset : Number(epNum);
  const streams = await scrapeEpisodeWatch2(series.slug, `ep-${providerEp}`, audio);
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), providerEpisode: providerEp, audio, streams });
}
__name(handleWatch5, "handleWatch");
var anineko_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET,OPTIONS", "Access-Control-Allow-Headers": "*" } });
    }
    try {
      const m = url.pathname.match(/^\/watch\/anineko\/(\d+)\/(sub|dub)\/anineko-(\d+)\/?$/);
      if (m) return await handleWatch5(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, "Raw-ERROR": err.rawBody ?? null, stack: err.stack }, 500);
    }
  }
};

// providers/anidbapp.js
var BASE4 = "https://anidb.app";
var UA7 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";
var NAV_HEADERS = {
  "User-Agent": UA7,
  "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
  "Accept-Language": "en-US,en;q=0.9"
};
var XHR_HEADERS = {
  "User-Agent": UA7,
  "Accept": "application/json, text/html, */*;q=0.8",
  "Accept-Language": "en-US,en;q=0.9",
  "X-Requested-With": "XMLHttpRequest"
};
async function curlFetch(url, headersObj = {}) {
  const res = await fetch(url, {
    headers: { ...headersObj, "User-Agent": UA7 }
  });
  const body = await res.text();
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status} fetching ${url}`);
    err.rawBody = body;
    throw err;
  }
  return body;
}
__name(curlFetch, "curlFetch");
async function fetchAnidbHtml(url, referer) {
  const headers = referer ? { ...NAV_HEADERS, Referer: referer } : NAV_HEADERS;
  return curlFetch(url, headers);
}
__name(fetchAnidbHtml, "fetchAnidbHtml");
async function fetchXhr(url, referer) {
  const headers = referer ? { ...XHR_HEADERS, Referer: referer } : XHR_HEADERS;
  return curlFetch(url, headers);
}
__name(fetchXhr, "fetchXhr");
async function fetchJson3(url, referer) {
  const text = await fetchXhr(url, referer);
  return JSON.parse(text);
}
__name(fetchJson3, "fetchJson");
async function search3(query) {
  const html2 = await fetchXhr(`${BASE4}/search/suggestions?q=${encodeURIComponent(query)}`, `${BASE4}/home`).catch(() => "");
  const results = [];
  for (const m of html2.matchAll(/<a\b[^>]*data-search-item\b[^>]*>[\s\S]*?<\/a>/gi)) {
    const tag = m[0].match(/<a\b[^>]*>/i)?.[0] ?? "";
    const href = attr(tag, "href");
    const path = href.startsWith("http") ? new URL(href).pathname : href;
    const slug = path.match(/^\/anime\/([^/?#]+)/)?.[1];
    if (!slug) continue;
    const title = stripTags(m[0].match(/<p\b[^>]*class=["'][^"']*text-sm[^"']*["'][^>]*>([\s\S]*?)<\/p>/i)?.[1] ?? "");
    const meta = stripTags(m[0].match(/<p\b[^>]*class=["'][^"']*text-xs[^"']*["'][^>]*>([\s\S]*?)<\/p>/i)?.[1] ?? "");
    const siteId = Number(slug.match(/-(\d+)$/)?.[1]);
    results.push({ slug, title: title || slug.replace(/-/g, " "), meta, siteId });
  }
  if (results.length) return results;
  const browseHtml = await fetchAnidbHtml(`${BASE4}/browse?q=${encodeURIComponent(query)}`, `${BASE4}/home`).catch(() => "");
  const seen = /* @__PURE__ */ new Set();
  for (const m of browseHtml.matchAll(/<a\b[^>]*href=["'](?:https:\/\/anidb\.app)?\/anime\/([^"']+)["'][^>]*class=["'][^"']*\banime-card\b[^"']*["'][^>]*>[\s\S]*?<\/a>/gi)) {
    const slug = m[1];
    if (seen.has(slug)) continue;
    seen.add(slug);
    const title = stripTags(m[0].match(/title=["']([^"']+)["']/i)?.[1] ?? "") || stripTags(m[0].match(/alt=["']([^"']+)["']/i)?.[1] ?? "") || slug.replace(/-/g, " ");
    const siteId = Number(slug.match(/-(\d+)$/)?.[1]);
    results.push({ slug, title, meta: "", siteId });
  }
  return results;
}
__name(search3, "search");
function parseExternalIds(html2) {
  return {
    anilistId: Number(html2.match(/https:\/\/anilist\.co\/anime\/(\d+)/i)?.[1]) || null,
    malId: Number(html2.match(/https:\/\/myanimelist\.net\/anime\/(\d+)/i)?.[1]) || null,
    anidbId: Number(html2.match(/https:\/\/anidb\.net\/anime\/(\d+)/i)?.[1]) || null,
    kitsuId: Number(html2.match(/https:\/\/kitsu\.app\/anime\/(\d+)/i)?.[1]) || null
  };
}
__name(parseExternalIds, "parseExternalIds");
function parsePageTitle(html2) {
  return stripTags(html2.match(/<h1\b[^>]*>([\s\S]*?)<\/h1>/i)?.[1] ?? "");
}
__name(parsePageTitle, "parsePageTitle");
function searchQueries(media, anizip) {
  const titles = buildTitles(media, anizip);
  const out = /* @__PURE__ */ new Set();
  for (const title of titles.slice(0, 5)) {
    out.add(title);
    const words = title.trim().split(/\s+/);
    if (words.length > 4) out.add(words.slice(0, 4).join(" "));
  }
  return [...out].filter((q) => q.length >= 2);
}
__name(searchQueries, "searchQueries");
async function resolveSeries4(anilistId, ctx = {}) {
  const cacheKey = `np:anidbapp:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const queries = searchQueries(media, ctx.anizip);
  const candidates = /* @__PURE__ */ new Map();
  await Promise.all(queries.map(async (q) => {
    for (const r of await search3(q).catch(() => [])) {
      if (!candidates.has(r.slug)) candidates.set(r.slug, r);
    }
  }));
  for (const candidate of candidates.values()) {
    const html2 = await fetchAnidbHtml(`${BASE4}/anime/${candidate.slug}`, `${BASE4}/home`).catch(() => "");
    if (!html2) continue;
    const ids = parseExternalIds(html2);
    if (ids.anilistId !== Number(anilistId)) continue;
    const data = {
      slug: candidate.slug,
      siteId: candidate.siteId || Number(candidate.slug.match(/-(\d+)$/)?.[1]),
      title: parsePageTitle(html2) || candidate.title,
      matchType: "anilist",
      matchScore: 1,
      ...ids
    };
    set(cacheKey, data, SHOW_IDENTITY_TTL);
    return data;
  }
  const malId = media?.idMal ?? null;
  if (malId) {
    for (const candidate of candidates.values()) {
      const html2 = await fetchAnidbHtml(`${BASE4}/anime/${candidate.slug}`, `${BASE4}/home`).catch(() => "");
      if (!html2) continue;
      const ids = parseExternalIds(html2);
      if (ids.anilistId || ids.malId !== Number(malId)) continue;
      const data = {
        slug: candidate.slug,
        siteId: candidate.siteId || Number(candidate.slug.match(/-(\d+)$/)?.[1]),
        title: parsePageTitle(html2) || candidate.title,
        matchType: "mal",
        matchScore: 0.9,
        ...ids
      };
      set(cacheKey, data, SHOW_IDENTITY_TTL);
      return data;
    }
  }
  throw new Error(`AniDB.app match not found for AniList ${anilistId}`);
}
__name(resolveSeries4, "resolveSeries");
async function fetchProviderEpisodes(siteId) {
  const data = await fetchJson3(`${BASE4}/api/frontend/anime/${siteId}/episodes`, `${BASE4}/anime/${siteId}`);
  return Array.isArray(data.episodes) ? data.episodes : [];
}
__name(fetchProviderEpisodes, "fetchProviderEpisodes");
function inferOffset(providerEpisodes, expected) {
  const nums = providerEpisodes.map((e) => Number(e.number)).filter((n) => Number.isFinite(n) && n > 0);
  if (!nums.length || !expected) return 0;
  const min = Math.min(...nums);
  const max = Math.max(...nums);
  if (min > expected) return min - 1;
  if (min > 1 && max - min + 1 >= expected) return min - 1;
  return 0;
}
__name(inferOffset, "inferOffset");
async function fetchLanguages(episodeId, seriesSlug) {
  const data = await fetchJson3(`${BASE4}/api/frontend/episode/${episodeId}/languages`, `${BASE4}/anime/${seriesSlug}`).catch(() => null);
  return Array.isArray(data?.languages) ? data.languages : [];
}
__name(fetchLanguages, "fetchLanguages");
function hasLanguage(languages, audio) {
  return Boolean(languageForAudio(languages, audio)?.embed_url);
}
__name(hasLanguage, "hasLanguage");
function buildEpisodeLists3(anilistId, providerEpisodes, ctx, expected, offset, availability) {
  const sub = [];
  const dub = [];
  for (const src of providerEpisodes) {
    const sourceNumber = Number(src.number);
    const number = sourceNumber - offset;
    if (!Number.isFinite(number) || number < 1) continue;
    if (expected && number > expected) continue;
    const meta = episodeMeta(number, ctx);
    const base = {
      number,
      title: meta.title ?? `Episode ${number}`,
      duration: meta.duration,
      filler: src.filler ?? meta.filler,
      uncensored: meta.uncensored,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate,
      sourceNumber,
      sourceId: src.id
    };
    if (availability.hasSub) sub.push({ ...base, id: `watch/anidbapp/${anilistId}/sub/anidbapp-${number}`, audio: "sub" });
    if (availability.hasDub) dub.push({ ...base, id: `watch/anidbapp/${anilistId}/dub/anidbapp-${number}`, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists3, "buildEpisodeLists");
function languageForAudio(languages, audio) {
  const preferred = audio === "sub" ? ["jpn", "ja", "japanese"] : ["eng", "en", "english"];
  return languages.find((l) => preferred.includes(String(l.code ?? "").toLowerCase())) ?? languages.find((l) => preferred.includes(String(l.name ?? "").toLowerCase())) ?? null;
}
__name(languageForAudio, "languageForAudio");
function extractHls2(html2) {
  const patterns = [
    /file\s*:\s*["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i,
    /sources\s*:\s*\[\s*\{[^}]*file\s*:\s*["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i,
    /["'](https?:\/\/[^"']+\/master\.m3u8[^"']*)["']/i,
    /["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i
  ];
  for (const pattern of patterns) {
    const m = html2.match(pattern);
    if (m?.[1]) return decodeEntities(m[1]);
  }
  return null;
}
__name(extractHls2, "extractHls");
async function streamsForEmbed(embedUrl, audio, language) {
  const html2 = await fetchAnidbHtml(embedUrl, { Referer: `${BASE4}/` }).catch(() => "");
  const hls = html2 ? extractHls2(html2) : null;
  const streams = [];
  if (hls) {
    streams.push({
      url: hls,
      type: "hls",
      audio,
      language: language.code,
      server: "AniDB.app",
      embed: embedUrl,
      referer: `${new URL(embedUrl).origin}/`,
      priority: 5,
      isActive: true
    });
  }
  streams.push({
    url: embedUrl,
    type: "embed",
    audio,
    language: language.code,
    server: "AniDB.app-embed",
    referer: `${BASE4}/`,
    priority: 4,
    isActive: !hls
  });
  return streams;
}
__name(streamsForEmbed, "streamsForEmbed");
async function getEpisodes5(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const series = await resolveSeries4(anilistId, localCtx);
  const episodes = await fetchProviderEpisodes(series.siteId);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const offset = inferOffset(episodes, expected);
  const sampleLanguages = episodes[0]?.id ? await fetchLanguages(episodes[0].id, series.slug) : [];
  const availability = {
    hasSub: hasLanguage(sampleLanguages, "sub") || !sampleLanguages.length,
    hasDub: hasLanguage(sampleLanguages, "dub")
  };
  return {
    meta: {
      id: series.slug,
      siteId: series.siteId,
      title: series.title,
      source: "anidbapp",
      matchScore: series.matchScore,
      matchType: series.matchType,
      anilistId: series.anilistId,
      malId: series.malId,
      numbering: offset ? "offset" : "local",
      episodeOffset: offset
    },
    episodes: buildEpisodeLists3(anilistId, episodes, localCtx, expected, offset, availability)
  };
}
__name(getEpisodes5, "getEpisodes");
async function handleWatch6(anilistId, audio, epNum, ctx = {}) {
  const series = await resolveSeries4(anilistId, ctx);
  const episodes = await fetchProviderEpisodes(series.siteId);
  const media = ctx.media ?? await getMedia(anilistId).catch(() => null);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const offset = inferOffset(episodes, expected);
  const providerEp = Number(epNum) + offset;
  const episode = episodes.find((e) => Number(e.number) === providerEp);
  if (!episode) return json2({ error: `AniDB.app episode ${epNum} not found` }, 404);
  const languages = await fetchLanguages(episode.id, series.slug);
  const language = languageForAudio(languages, audio);
  if (!language?.embed_url) {
    return json2({ anilistId: Number(anilistId), episode: Number(epNum), providerEpisode: providerEp, audio, streams: [] });
  }
  const embedUrl = decodeEntities(language.embed_url);
  const streams = await streamsForEmbed(embedUrl, audio, language);
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), providerEpisode: providerEp, audio, language: language.code, streams });
}
__name(handleWatch6, "handleWatch");
var anidbapp_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET,OPTIONS", "Access-Control-Allow-Headers": "*" } });
    }
    try {
      const m = url.pathname.match(/^\/watch\/anidbapp\/(\d+)\/(sub|dub)\/anidbapp-(\d+)\/?$/);
      if (m) return await handleWatch6(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, "Raw-ERROR": err.rawBody ?? null, stack: err.stack }, 500);
    }
  }
};

// providers/2dhive.js
async function getMalId(anilistId, ctx) {
  const idMal = ctx?.media?.idMal ?? (await getMedia(anilistId)).idMal;
  if (!idMal) throw new Error(`2dhive: no MAL ID found for AniList ${anilistId}`);
  return idMal;
}
__name(getMalId, "getMalId");
var BASE5 = "https://2dhive.com";
var UA8 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
async function fetchPage(url) {
  const res = await fetch(url, { headers: { "User-Agent": UA8 } });
  if (!res.ok) throw new Error(`2dhive ${res.status}: ${url}`);
  return res.text();
}
__name(fetchPage, "fetchPage");
function extractPlayerProps(html2) {
  const idx = html2.indexOf("prefetchedHls");
  const propsIdx = idx === -1 ? html2.indexOf('component-export="default"') : html2.lastIndexOf('props="', idx);
  if (propsIdx === -1) return null;
  const attrIdx = html2.indexOf('props="', propsIdx);
  if (attrIdx === -1) return null;
  const valueIdx = attrIdx + 7;
  const endIdx = html2.indexOf('"', valueIdx);
  if (endIdx === -1) return null;
  const raw = html2.slice(valueIdx, endIdx).replace(/&quot;/g, '"').replace(/&amp;/g, "&").replace(/&#39;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}
__name(extractPlayerProps, "extractPlayerProps");
function extractEpisodePlayerProps(html2) {
  const island = html2.match(/<astro-island[^>]+component-url="[^"]*EpisodePlayer[^"]*"[^>]+props="([^"]+)"/);
  if (!island) return null;
  const raw = island[1].replace(/&quot;/g, '"').replace(/&amp;/g, "&").replace(/&#39;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
  try {
    return decodeProps(JSON.parse(raw));
  } catch {
    return null;
  }
}
__name(extractEpisodePlayerProps, "extractEpisodePlayerProps");
function astroDecode(v) {
  if (!Array.isArray(v)) return v;
  const [type, data] = v;
  if (type === 0) {
    if (data === null || typeof data !== "object" || Array.isArray(data)) return data;
    return Object.fromEntries(Object.entries(data).map(([k, val]) => [k, astroDecode(val)]));
  }
  if (type === 1) return Array.isArray(data) ? data.map(astroDecode) : data;
  return data;
}
__name(astroDecode, "astroDecode");
function decodeProps(raw) {
  return Object.fromEntries(Object.entries(raw).map(([k, v]) => [k, astroDecode(v)]));
}
__name(decodeProps, "decodeProps");
function parseEpisodeNums(html2, malId) {
  const re = new RegExp(`/episode\\?anime=${malId}&(?:amp;)?ep_num=(\\d+)`, "gi");
  const nums = /* @__PURE__ */ new Set();
  for (const m of html2.matchAll(re)) nums.add(Number(m[1]));
  return [...nums].sort((a, b) => a - b);
}
__name(parseEpisodeNums, "parseEpisodeNums");
async function fetchEpisodePage(malId, epNum) {
  const html2 = await fetchPage(`${BASE5}/episode?anime=${malId}&ep_num=${epNum}`);
  const rawProps = extractPlayerProps(html2);
  const props = rawProps ? decodeProps(rawProps) : {};
  const player = extractEpisodePlayerProps(html2) ?? {};
  const iframes = [...html2.matchAll(/<iframe[^>]+src="([^"]+)"/g)].map((m) => m[1].replace(/&amp;/g, "&"));
  const servers = [];
  const add = /* @__PURE__ */ __name((name, url, dub = false) => {
    if (!url || servers.some((s) => s.slug === url && Boolean(s.dub) === dub)) return;
    servers.push({ server_name: name, slug: url, dub });
  }, "add");
  for (const src of iframes) add(src.includes("babastream") ? "BabaStream" : src.includes("megaplay") ? "MegaPlay" : "Embed", src, src.endsWith("/dub"));
  add("MegaPlay", `https://megaplay.buzz/stream/mal/${malId}/${epNum}/sub`, false);
  add("BabaStream", `https://babastream.top/embed/${malId}/${epNum}/sub`, false);
  add("MegaPlay", `https://megaplay.buzz/stream/mal/${malId}/${epNum}/dub`, true);
  add("BabaStream", `https://babastream.top/embed/${malId}/${epNum}/dub`, true);
  return {
    ...props,
    prefetchedHls: props.prefetchedHls ?? {},
    servers: Array.isArray(props.servers) && props.servers.length ? props.servers : servers,
    totalEpisodes: player.totalEpisodes ?? null
  };
}
__name(fetchEpisodePage, "fetchEpisodePage");
async function getEpisodes6(anilistId, ctx = {}) {
  const malId = await getMalId(anilistId, ctx);
  const animeHtml = await fetchPage(`${BASE5}/anime?anime=${malId}`);
  const epNums = parseEpisodeNums(animeHtml, malId);
  if (!epNums.length) throw new Error(`2dhive: no episodes found for AniList ${anilistId} (MAL ${malId})`);
  const props = await fetchEpisodePage(malId, epNums[0]);
  const hasDub = Boolean(props.prefetchedHls?.dub?.content) || Array.isArray(props.servers) && props.servers.some((s) => s.dub);
  const expected = expectedCount(ctx.media, ctx.anizip, ctx.jikanEps);
  const sub = [], dub = [];
  for (const num of epNums) {
    if (expected && num > expected) continue;
    const meta = episodeMeta(num, ctx);
    const base = {
      number: num,
      title: meta.title ?? `Episode ${num}`,
      duration: meta.duration ?? null,
      filler: meta.filler ?? false,
      uncensored: meta.uncensored ?? false,
      description: meta.description ?? null,
      image: meta.image ?? null,
      airDate: meta.airDate ?? null
    };
    sub.push({ id: `watch/2dhive/${anilistId}/sub/2dhive-${num}`, ...base, audio: "sub" });
    if (hasDub) dub.push({ id: `watch/2dhive/${anilistId}/dub/2dhive-${num}`, ...base, audio: "dub" });
  }
  return {
    meta: {
      id: String(anilistId),
      source: "2dhive",
      matchScore: 1,
      numbering: "standard",
      episodeOffset: 0
    },
    episodes: { sub, dub }
  };
}
__name(getEpisodes6, "getEpisodes");
async function handleWatch7(anilistId, audio, epNum) {
  const malId = await getMalId(anilistId);
  const referer = `${BASE5}/episode?anime=${malId}&ep_num=${epNum}`;
  const [propsResult, hiAnimeResult, dlContent] = await Promise.allSettled([
    fetchEpisodePage(malId, epNum),
    audio !== "dub" ? fetchHiAnimeHls(malId, epNum, referer) : Promise.resolve(null),
    fetchDownloadHls(malId, audio, epNum)
  ]);
  const streams = [];
  const props = propsResult.status === "fulfilled" ? propsResult.value : null;
  if (props) {
    const hlsContent = audio === "dub" ? props.prefetchedHls?.dub?.content : props.prefetchedHls?.sub?.content;
    if (hlsContent) {
      streams.push({
        server: audio === "dub" ? "HLS DUB" : "HLS SUB",
        url: `/stream/2dhive/${anilistId}/${audio}/${epNum}`
      });
    }
    const rawServers = Array.isArray(props.servers) ? props.servers : [];
    const selectedServers = rawServers.filter(
      (server) => Boolean(server.dub) === (audio === "dub") && typeof server.slug === "string" && server.slug
    );
    const babaStreams = selectedServers.filter((server) => /babastream\.top\/embed\//i.test(server.slug));
    const megaStreams = selectedServers.filter((server) => /megaplay\.[^/]+\/stream\//i.test(server.slug));
    for (const server of babaStreams) {
      streams.push({
        server: server.server_name || "BabaStream",
        url: server.slug,
        type: "embed"
      });
    }
    const babaResults = await Promise.allSettled(babaStreams.map(async (server) => ({
      embed: server.slug,
      source: await extractBabaStreamDetails(server.slug, { userAgent: UA8, referer })
    })));
    for (const result of babaResults) {
      if (result.status !== "fulfilled" || !result.value.source?.url) continue;
      streams.push({
        server: "BabaStream",
        url: result.value.source.url,
        type: result.value.source.type,
        embed: result.value.embed,
        referer: `${result.value.source.origin}/`
      });
    }
    const defaultMegaPlay2 = `https://megaplay.buzz/stream/mal/${malId}/${epNum}/${audio}`;
    const megaPlayEmbeds = [.../* @__PURE__ */ new Set([
      ...megaStreams.map((server) => server.slug),
      defaultMegaPlay2
    ])];
    const megaPlayResults = await Promise.allSettled(megaPlayEmbeds.map(async (embed) => ({
      embed,
      extracted: await extractMegaPlayDetails(embed, { userAgent: UA8, referer })
    })));
    for (const result of megaPlayResults) {
      if (result.status !== "fulfilled") continue;
      for (const source of result.value.extracted.sources) {
        const stream = {
          server: "MegaPlay",
          url: source.url,
          type: "hls",
          variant: source.variant,
          embed: result.value.embed,
          referer: `${result.value.extracted.origin}/`,
          subtitles: result.value.extracted.tracks
        };
        if (result.value.extracted.intro) stream.intro = result.value.extracted.intro;
        if (result.value.extracted.outro) stream.outro = result.value.extracted.outro;
        streams.push(stream);
      }
    }
    for (const server of megaStreams) {
      streams.push({
        server: server.server_name || "Embed",
        url: server.slug,
        type: "embed"
      });
    }
    if (!streams.some((stream) => stream.url === defaultMegaPlay2)) {
      streams.push({
        server: audio === "dub" ? "MegaPlay Dub" : "MegaPlay Sub",
        url: defaultMegaPlay2,
        type: "embed"
      });
    }
    for (const server of selectedServers) {
      if (server.server_name === "HAdfree" || babaStreams.includes(server) || megaStreams.includes(server)) continue;
      streams.push({
        server: server.server_name || "Embed",
        url: server.slug,
        type: "embed"
      });
    }
    const hadfreeEntries = selectedServers.filter((server) => server.server_name === "HAdfree");
    const hadfreeResults = await Promise.allSettled(
      hadfreeEntries.map(
        (entry) => fetch(`${BASE5}/api/hadfree?slug=${encodeURIComponent(entry.slug)}`, {
          headers: { "User-Agent": UA8, "Referer": referer }
        }).then((r) => r.ok ? r.json() : null).catch(() => null)
      )
    );
    for (const r of hadfreeResults) {
      if (r.status === "fulfilled" && r.value?.streamUrl) {
        streams.push({ server: "HAdfree", url: r.value.streamUrl });
      }
    }
  }
  const defaultMegaPlay = `https://megaplay.buzz/stream/mal/${malId}/${epNum}/${audio}`;
  if (!streams.some((s) => s.url === defaultMegaPlay)) {
    streams.push({
      server: audio === "dub" ? "MegaPlay Dub" : "MegaPlay Sub",
      url: defaultMegaPlay,
      type: "embed"
    });
  }
  const hiAnime = hiAnimeResult.status === "fulfilled" ? hiAnimeResult.value : null;
  if (hiAnime?.m3u8) {
    const entry = { server: "hiAnime", url: hiAnime.m3u8, type: "hls" };
    if (hiAnime.subtitle) entry.subtitle = hiAnime.subtitle;
    streams.push(entry);
  }
  if (dlContent.status === "fulfilled" && dlContent.value) {
    streams.push({
      server: "Download",
      url: `/stream/2dhive/download/${anilistId}/${audio}/${epNum}`
    });
  }
  const seen = /* @__PURE__ */ new Set();
  const cleanStreams = streams.filter((s) => {
    const key = `${s.server}:${s.url}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), audio, streams: cleanStreams });
}
__name(handleWatch7, "handleWatch");
async function fetchDownloadHls(malId, audio, epNum) {
  const fileKey = `${malId}_${epNum}_${audio}`;
  try {
    const res = await fetch(`${BASE5}/download?file=${encodeURIComponent(fileKey)}`, {
      headers: {
        "User-Agent": UA8,
        "Referer": `${BASE5}/episode?anime=${malId}&ep_num=${epNum}`
      }
    });
    if (!res.ok) return null;
    const html2 = await res.text();
    const m = html2.match(/downloadPayload\s*=\s*(\{.*?\});/s);
    if (!m) return null;
    const payload = JSON.parse(m[1]);
    return payload.hlsContent || null;
  } catch {
    return null;
  }
}
__name(fetchDownloadHls, "fetchDownloadHls");
async function fetchHiAnimeHls(malId, epNum, referer) {
  try {
    const res = await fetch(`${BASE5}/api/hianime?mal_id=${malId}&ep_num=${epNum}`, {
      headers: { "User-Agent": UA8, "Referer": referer }
    });
    if (!res.ok) return null;
    return res.json().catch(() => null);
  } catch {
    return null;
  }
}
__name(fetchHiAnimeHls, "fetchHiAnimeHls");
async function handleDownloadStream(anilistId, audio, epNum) {
  const malId = await getMalId(anilistId);
  const content = await fetchDownloadHls(malId, audio, epNum);
  if (!content) {
    return new Response(JSON.stringify({ error: "No download stream found" }), {
      status: 404,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }
  return new Response(content, {
    status: 200,
    headers: {
      "Content-Type": "application/vnd.apple.mpegurl",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=3600"
    }
  });
}
__name(handleDownloadStream, "handleDownloadStream");
async function handleStream(anilistId, audio, epNum) {
  const malId = await getMalId(anilistId);
  const referer = `${BASE5}/episode?anime=${malId}&ep_num=${epNum}`;
  const props = await fetchEpisodePage(malId, epNum);
  let content = audio === "dub" ? props.prefetchedHls?.dub?.content : props.prefetchedHls?.sub?.content;
  if (!content && audio !== "dub") {
    const hiAnime = await fetchHiAnimeHls(malId, epNum, referer);
    if (hiAnime?.m3u8) {
      const res = await fetch(hiAnime.m3u8, { headers: { "User-Agent": UA8, "Referer": BASE5 } }).catch(() => null);
      if (res?.ok) content = await res.text();
      else {
        return new Response(null, {
          status: 302,
          headers: {
            "Location": hiAnime.m3u8,
            "Access-Control-Allow-Origin": "*",
            "Cache-Control": "no-store"
          }
        });
      }
    }
  }
  if (!content) {
    return new Response(JSON.stringify({ error: "No HLS stream found" }), {
      status: 404,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }
  return new Response(content, {
    status: 200,
    headers: {
      "Content-Type": "application/vnd.apple.mpegurl",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=3600"
    }
  });
}
__name(handleStream, "handleStream");
var dhive_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    const path = url.pathname;
    try {
      let m = path.match(/^\/watch\/2dhive\/(\d+)\/(sub|dub)\/2dhive-(\d+)\/?$/);
      if (m) return await handleWatch7(m[1], m[2], m[3]);
      m = path.match(/^\/stream\/2dhive\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
      if (m) return await handleStream(m[1], m[2], m[3]);
      m = path.match(/^\/stream\/2dhive\/download\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
      if (m) return await handleDownloadStream(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// providers/animenosub.js
var BASE6 = "https://animenosub.to";
var UA9 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
async function search4(query) {
  const res = await fetch(`${BASE6}/wp-admin/admin-ajax.php`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      "X-Requested-With": "XMLHttpRequest",
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36",
      Origin: BASE6,
      Referer: `${BASE6}/`
    },
    body: `action=ts_ac_do_search&ts_ac_query=${encodeURIComponent(query)}`
  });
  if (!res.ok) throw new Error(`animenosub search HTTP ${res.status}`);
  const data = await res.json();
  const results = [];
  for (const item of data?.anime?.[0]?.all ?? []) {
    const slug = item.post_link?.match(/\/anime\/([^/]+)\/?$/)?.[1];
    if (!slug) continue;
    results.push({ slug, text: item.post_title ?? slug.replace(/-/g, " ") });
  }
  return results;
}
__name(search4, "search");
async function scrapeSeries3(slug) {
  const html2 = await fetchHtml(`${BASE6}/anime/${slug}/`, { Referer: BASE6 });
  const isSlugDub = /-dub$/.test(slug) || /(?:^|[-\s])dub(?:$|[-\s])/i.test(slug);
  const episodes = [];
  const seen = /* @__PURE__ */ new Set();
  const listRe = /<li\b[^>]*data-index="\d+"[^>]*>[\s\S]*?<a\s+href="(https?:\/\/animenosub\.to\/[^"]+)"[\s\S]*?<div\s+class="epl-num">([^<]+)<\/div>/gi;
  for (const m of html2.matchAll(listRe)) {
    const epUrl = decodeEntities(m[1]);
    const label = m[2].trim();
    let number;
    if (/^movie$/i.test(label)) {
      number = 1;
    } else {
      const n = parseFloat(label);
      number = Number.isFinite(n) && n >= 1 ? Math.round(n) : null;
    }
    if (number === null || seen.has(number)) continue;
    seen.add(number);
    const isDub2 = isSlugDub || /-dub(?:$|\/)/.test(epUrl);
    episodes.push({ number, title: /^movie$/i.test(label) ? "Movie" : `Episode ${number}`, epUrl, hasSub: !isDub2, hasDub: isDub2 });
  }
  episodes.sort((a, b) => a.number - b.number);
  return episodes;
}
__name(scrapeSeries3, "scrapeSeries");
async function scrapeEmbeds(epUrl) {
  const html2 = await fetchHtml(epUrl, { Referer: `${BASE6}/` });
  const streams = [];
  for (const m of html2.matchAll(/<option\s+value="([A-Za-z0-9+/=]+)"\s+data-index="\d+"[^>]*>([^<]+)<\/option>/gi)) {
    const b64 = m[1];
    const serverName = m[2].trim();
    if (!serverName || /select video server/i.test(serverName)) continue;
    let embedUrl = null;
    try {
      const decoded = atob(b64);
      embedUrl = decoded.match(/src=["']([^"']+)["']/i)?.[1] ?? null;
    } catch {
      continue;
    }
    if (!embedUrl) continue;
    const embedOrigin = (() => {
      try {
        const u = new URL(embedUrl.startsWith("//") ? `https:${embedUrl}` : embedUrl);
        return `${u.protocol}//${u.host}/`;
      } catch {
        return epUrl;
      }
    })();
    streams.push({
      url: embedUrl,
      type: "embed",
      server: serverName,
      referer: embedOrigin,
      priority: streams.length === 0 ? 2 : 1,
      isActive: streams.length === 0
    });
  }
  if (streams.length === 0) {
    for (const m of html2.matchAll(/<iframe[^>]+src=["']([^"']+)["'][^>]*>/gi)) {
      const src = m[1];
      if (/vidmoly|vtbe|streamtape|dood|filemoon|upn\.one|bysesa/i.test(src)) {
        const embedOrigin = (() => {
          try {
            const u = new URL(src.startsWith("//") ? `https:${src}` : src);
            return `${u.protocol}//${u.host}/`;
          } catch {
            return epUrl;
          }
        })();
        streams.push({ url: src, type: "embed", server: "Direct", referer: embedOrigin, priority: 2, isActive: true });
        break;
      }
    }
  }
  return streams;
}
__name(scrapeEmbeds, "scrapeEmbeds");
async function resolveSeries5(anilistId, ctx = {}) {
  const cacheKey = `np:animenosub:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const candidates = await findTopSlugs(titles, search4);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const offset = await getPrequelOffset(anilistId).catch(() => 0);
  const selected = await selectSeries(candidates, scrapeSeries3, expected, media?.status, offset);
  if (!selected) throw new Error(`animenosub match not found for AniList ${anilistId}`);
  const data = { slug: selected.slug, title: selected.title, mode: selected.mode, offset, score: selected.score };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries5, "resolveSeries");
function buildEpisodeLists4(anilistId, series, providerEpisodes, ctx, expected) {
  const sub = [], dub = [];
  for (const src of providerEpisodes) {
    const number = series.mode === "offset" ? src.number - series.offset : src.number;
    if (number < 1) continue;
    if (expected && number > expected) continue;
    const meta = episodeMeta(number, ctx);
    const base = {
      number,
      title: meta.title ?? src.title ?? `Episode ${number}`,
      duration: meta.duration,
      filler: meta.filler,
      uncensored: meta.uncensored,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate,
      sourceNumber: src.number
    };
    if (src.hasSub) sub.push({ ...base, id: `watch/animenosub/${anilistId}/sub/animenosub-${number}`, audio: "sub" });
    if (src.hasDub) dub.push({ ...base, id: `watch/animenosub/${anilistId}/dub/animenosub-${number}`, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists4, "buildEpisodeLists");
async function getEpisodes7(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const series = await resolveSeries5(anilistId, localCtx);
  const episodes = await scrapeSeries3(series.slug);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  return {
    meta: {
      id: series.slug,
      title: series.title,
      source: "animenosub",
      matchScore: Number(series.score.toFixed(3)),
      numbering: series.mode,
      episodeOffset: series.mode === "offset" ? series.offset : 0
    },
    episodes: buildEpisodeLists4(anilistId, series, episodes, localCtx, expected)
  };
}
__name(getEpisodes7, "getEpisodes");
async function withRetry(fn, attempts = 2) {
  for (let i = 0; i < attempts; i++) {
    try {
      return await fn();
    } catch (_) {
      if (i === attempts - 1) return null;
    }
  }
  return null;
}
__name(withRetry, "withRetry");
async function handleWatch8(anilistId, audio, epNum, ctx = {}) {
  const series = await resolveSeries5(anilistId, ctx);
  const providerEp = series.mode === "offset" ? Number(epNum) + series.offset : Number(epNum);
  const episodes = await scrapeSeries3(series.slug);
  const ep = episodes.find((e) => e.number === providerEp && (audio === "dub" ? e.hasDub : e.hasSub)) ?? episodes.find((e) => e.number === providerEp);
  if (!ep) throw new Error(`animenosub episode ${providerEp} not found`);
  const embeds = await scrapeEmbeds(ep.epUrl);
  const resolvable = embeds.map((stream) => ({ stream, extractor: findVideoExtractor(stream.url) })).filter((item) => item.extractor);
  const resolvedList = await Promise.all(resolvable.map(({ stream, extractor }) => withRetry(() => extractor.extract(stream.url, { userAgent: UA9, referer: `${BASE6}/` }))));
  const resolvedMap = new Map(resolvable.map(({ stream, extractor }, index) => [stream.url, { extractor, urls: resolvedList[index] }]));
  const streams = [];
  for (const stream of embeds) {
    const resolved2 = resolvedMap.get(stream.url);
    if (resolved2?.urls) {
      const referer = resolved2.extractor.name === "vidmoly" ? "https://vidmoly.biz/" : resolved2.extractor.name === "nova" ? "https://nova.upn.one/" : "https://bysesayeveum.com/";
      for (const resolvedSource of resolved2.urls) {
        const source = typeof resolvedSource === "string" ? { url: resolvedSource, type: "hls" } : resolvedSource;
        streams.push({
          url: source.url,
          type: source.type,
          server: stream.server,
          referer,
          priority: stream.priority,
          isActive: stream.isActive
        });
      }
    }
    streams.push(stream);
  }
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), providerEpisode: providerEp, audio, streams });
}
__name(handleWatch8, "handleWatch");
var animenosub_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET,OPTIONS", "Access-Control-Allow-Headers": "*" } });
    }
    try {
      const m = url.pathname.match(/^\/watch\/animenosub\/(\d+)\/(sub|dub)\/animenosub-(\d+)\/?$/);
      if (m) return await handleWatch8(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, "Raw-ERROR": err.rawBody ?? null, stack: err.stack }, 500);
    }
  }
};

// providers/anizone.js
var BASE7 = "https://anizone.to";
var UA10 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36";
function escapeRegex(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
__name(escapeRegex, "escapeRegex");
function normalizeUrl(value) {
  return String(value || "").replace(/\\+\//g, "/");
}
__name(normalizeUrl, "normalizeUrl");
function decodeJsonArgument(raw) {
  if (!raw) return null;
  const marker = "U";
  let value = String(raw).replace(/\\\\u([0-9a-fA-F]{4})/g, `${marker}$1`);
  value = value.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)));
  value = value.replace(/\x01U\x01([0-9a-fA-F]{4})/g, "\\u$1");
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}
__name(decodeJsonArgument, "decodeJsonArgument");
function jsonArgument(html2, name) {
  const pattern = new RegExp(`${escapeRegex(name)}\\s*:\\s*JSON\\.parse\\('((?:[^'\\\\]|\\\\.)*)'\\)`, "i");
  return decodeJsonArgument(String(html2).match(pattern)?.[1]);
}
__name(jsonArgument, "jsonArgument");
function playerData(html2) {
  const match = String(html2).match(/vidstackPlayer\s*\(\s*JSON\.parse\('((?:[^'\\]|\\.)*)'\)\s*\)/i);
  return decodeJsonArgument(match?.[1]);
}
__name(playerData, "playerData");
function responseCookies(response) {
  if (typeof response.headers.getSetCookie === "function") return response.headers.getSetCookie();
  const value = response.headers.get("set-cookie");
  return value ? [value] : [];
}
__name(responseCookies, "responseCookies");
function mergeCookies(jar, values) {
  for (const value of values) {
    const match = String(value).match(/^\s*([^=;\s]+)=([^;]*)/);
    if (match) jar.set(match[1], match[2]);
  }
  return jar;
}
__name(mergeCookies, "mergeCookies");
function cookieHeader2(jar) {
  return [...jar].map(([name, value]) => `${name}=${value}`).join("; ");
}
__name(cookieHeader2, "cookieHeader");
async function request(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      "User-Agent": UA10,
      "Accept-Language": "en-US,en;q=0.9",
      ...options.headers
    }
  });
  const raw = await response.text();
  if (!response.ok) {
    const error = new Error(`AniZone HTTP ${response.status}: ${url}`);
    error.rawBody = raw;
    throw error;
  }
  return { raw, cookies: responseCookies(response) };
}
__name(request, "request");
async function fetchPage2(path) {
  return request(`${BASE7}${path}`, {
    headers: {
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Referer": `${BASE7}/`
    }
  });
}
__name(fetchPage2, "fetchPage");
function pickTitle(titles) {
  return titles?.["1"] || titles?.["5"] || titles?.["8"] || Object.values(titles || {})[0] || "";
}
__name(pickTitle, "pickTitle");
function titleValues(item) {
  return [...new Set([item?.main_title, ...Object.values(item?.title_list || {})].filter(Boolean))];
}
__name(titleValues, "titleValues");
function formatName(value) {
  const type = String(value || "").toLowerCase();
  if (type.includes("special")) return "special";
  if (type.includes("movie")) return "movie";
  if (type.includes("ova")) return "ova";
  if (type.includes("web") || type.includes("ona")) return "ona";
  if (type.includes("tv")) return "tv";
  return "";
}
__name(formatName, "formatName");
function expectedFormat(value) {
  const format = String(value || "").toUpperCase();
  if (format === "TV" || format === "TV_SHORT") return "tv";
  if (format === "MOVIE") return "movie";
  if (format === "OVA") return "ova";
  if (format === "ONA") return "ona";
  if (format === "SPECIAL") return "special";
  return "";
}
__name(expectedFormat, "expectedFormat");
function searchQueries2(titles) {
  const queries = /* @__PURE__ */ new Set();
  for (const raw of titles.slice(0, 8)) {
    const title = String(raw || "").replace(/\s+/g, " ").trim();
    if (!title) continue;
    queries.add(title);
    const plain = title.replace(/[^\p{L}\p{N}]+/gu, " ").replace(/\s+/g, " ").trim();
    if (plain.length >= 3) queries.add(plain);
    const words = plain.split(/\s+/).filter(Boolean);
    if (words.length > 4) queries.add(words.slice(0, 4).join(" "));
    const family = plain.replace(/\b(?:the\s+)?final\s+chapters?\b/gi, " ").replace(/\bfinal\s+(?:arc|edition)\b/gi, " ").replace(/\b(?:kanketsu|kouhen|zenpen)\s*(?:hen)?\b/gi, " ").replace(/\b(?:the\s+)?movie\b/gi, " ").replace(/\b(?:season|part|cour|chapter)\s*(?:\d+|one|two|three|four|final)?\b/gi, " ").replace(/\b(?:final|special)\s*(?:\d+|one|two|three|four)?\b/gi, " ").replace(/\s+/g, " ").trim();
    if (family.length >= 3) queries.add(family);
  }
  return [...queries].filter((query) => query.length >= 3).slice(0, 8);
}
__name(searchQueries2, "searchQueries");
function parseSearchItems(html2) {
  const items = jsonArgument(html2, "items");
  if (!Array.isArray(items)) return [];
  return items.filter((item) => /^[a-z0-9-]+$/i.test(String(item?.slug || ""))).map((item) => ({
    slug: String(item.slug),
    title: pickTitle(item.title_list) || item.main_title || "",
    titles: titleValues(item),
    type: formatName(item.type),
    year: Number(item.start_year) || null,
    episodeCount: Number(item.episode_count) || 0
  })).filter((item) => item.title);
}
__name(parseSearchItems, "parseSearchItems");
async function search5(query) {
  const { raw } = await fetchPage2(`/anime?search=${encodeURIComponent(query)}`);
  return parseSearchItems(raw);
}
__name(search5, "search");
function candidateTitleScore(titles, candidate) {
  let best = 0;
  for (const title of titles) {
    for (const value of candidate.titles) best = Math.max(best, diceCoeff(title, value));
  }
  return best;
}
__name(candidateTitleScore, "candidateTitleScore");
function coverageScore(candidate, expected, status) {
  if (!expected || expected < 1) return 0.5;
  if (candidate.episodeCount < 1) return 0;
  if (expected < 6) return 1;
  const needed = status === "FINISHED" ? Math.ceil(expected * 0.8) : Math.max(1, expected - 3);
  return Math.min(1, candidate.episodeCount / needed);
}
__name(coverageScore, "coverageScore");
function validateCandidate(candidate, media, titles, expected) {
  const titleScore2 = candidateTitleScore(titles, candidate);
  const format = expectedFormat(media?.format);
  const year = Number(media?.startDate?.year ?? media?.seasonYear ?? 0) || null;
  if (titleScore2 < 0.68) return null;
  if (format && candidate.type && format !== candidate.type) return null;
  if (year && candidate.year && year !== candidate.year) return null;
  const coverage = coverageScore(candidate, expected, media?.status);
  if (expected >= 6 && coverage < 0.8) return null;
  const score = titleScore2 * 0.72 + (format && candidate.type === format ? 0.14 : 0.07) + (year && candidate.year === year ? 0.1 : 0.04) + coverage * 0.04;
  return { ...candidate, titleScore: titleScore2, coverage, score };
}
__name(validateCandidate, "validateCandidate");
async function resolveSeries6(anilistId, ctx = {}) {
  const cacheKey = `np:anizone:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const discovered = /* @__PURE__ */ new Map();
  await Promise.all(searchQueries2(titles).map(async (query) => {
    try {
      for (const candidate of await search5(query)) if (!discovered.has(candidate.slug)) discovered.set(candidate.slug, candidate);
    } catch {
    }
  }));
  const valid = [...discovered.values()].map((candidate) => validateCandidate(candidate, media, titles, expected)).filter(Boolean).sort((left, right) => right.score - left.score);
  const selected = valid[0];
  const runnerUp = valid[1];
  if (!selected || selected.score < 0.82 || runnerUp && selected.score - runnerUp.score < 0.08) {
    throw new Error(`AniZone match not confident for AniList ${anilistId}`);
  }
  const data = {
    slug: selected.slug,
    title: selected.title,
    matchScore: selected.titleScore,
    score: selected.score
  };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries6, "resolveSeries");
function snapshot(html2) {
  const match = [...String(html2).matchAll(/wire:snapshot="([^"]*)"/gi)].find((item) => item[1].includes("pages.anime-detail"));
  return match ? decodeEntities(match[1]) : "";
}
__name(snapshot, "snapshot");
function cursor(html2) {
  return String(html2).match(/nextCursor:\s*'([^']+)'/i)?.[1] || null;
}
__name(cursor, "cursor");
function hasMore(html2) {
  return /hasMore:\s*true/i.test(String(html2));
}
__name(hasMore, "hasMore");
function csrf(html2) {
  return String(html2).match(/csrf-token"\s+content="([^"]+)"/i)?.[1] || "";
}
__name(csrf, "csrf");
function seconds(value) {
  const parts = String(value || "").match(/^(\d+):(\d{1,2})$/);
  if (!parts) return null;
  return Number(parts[1]) * 60 + Number(parts[2]);
}
__name(seconds, "seconds");
function episodeNumber(item) {
  const direct = Number(item?.slug);
  if (Number.isFinite(direct) && direct > 0) return direct;
  const fromUrl = normalizeUrl(item?.url).match(/\/(\d+)\/?$/)?.[1];
  const number = Number(fromUrl);
  return Number.isFinite(number) && number > 0 ? number : null;
}
__name(episodeNumber, "episodeNumber");
function parseEpisodes(items) {
  const seen = /* @__PURE__ */ new Set();
  return items.map((item) => {
    const number = episodeNumber(item);
    if (!number || seen.has(number)) return null;
    seen.add(number);
    return {
      number,
      sourceNumber: number,
      title: pickTitle(item.title_list) || `Episode ${number}`,
      duration: seconds(item.duration),
      description: item.summary || null,
      image: normalizeUrl(item.snapshot) || null,
      airDate: item.air_date || null,
      hasSub: Number(item.videos_count) > 0,
      hasDub: false
    };
  }).filter(Boolean).sort((left, right) => left.number - right.number);
}
__name(parseEpisodes, "parseEpisodes");
function initialPage(html2, cookies) {
  const items = jsonArgument(html2, "items");
  const data = {
    items: Array.isArray(items) ? items : [],
    snapshot: snapshot(html2),
    cursor: cursor(html2),
    hasMore: hasMore(html2),
    csrf: csrf(html2),
    cookies: mergeCookies(/* @__PURE__ */ new Map(), cookies)
  };
  if (!data.items.length || !data.snapshot || !data.csrf) {
    const error = new Error("AniZone page payload not found");
    error.rawBody = html2;
    throw error;
  }
  return data;
}
__name(initialPage, "initialPage");
async function loadPage(state, slug) {
  const response = await request(`${BASE7}/livewire/update`, {
    method: "POST",
    headers: {
      "Accept": "application/json, text/plain, */*",
      "Content-Type": "application/json",
      "X-Livewire": "",
      "X-CSRF-TOKEN": state.csrf,
      "X-Requested-With": "XMLHttpRequest",
      "Origin": BASE7,
      "Referer": `${BASE7}/anime/${slug}`,
      "Cookie": cookieHeader2(state.cookies)
    },
    body: JSON.stringify({
      components: [{
        snapshot: state.snapshot,
        updates: {},
        calls: [{ path: "", method: "loadPage", params: [state.cursor] }]
      }]
    })
  });
  let payload;
  try {
    payload = JSON.parse(response.raw);
  } catch {
    const error = new Error("AniZone returned invalid Livewire JSON");
    error.rawBody = response.raw;
    throw error;
  }
  const component = payload?.components?.[0];
  const dispatch = component?.effects?.dispatches?.find((item) => item?.name === "items-loaded");
  if (!component?.snapshot || !dispatch?.params || !Array.isArray(dispatch.params.items)) {
    const error = new Error("AniZone page continuation payload not found");
    error.rawBody = response.raw;
    throw error;
  }
  return {
    items: dispatch.params.items,
    snapshot: component.snapshot,
    cursor: dispatch.params.nextCursor || null,
    hasMore: Boolean(dispatch.params.hasMore),
    csrf: state.csrf,
    cookies: mergeCookies(state.cookies, response.cookies)
  };
}
__name(loadPage, "loadPage");
async function scrapeSeries4(slug, limit, maxPages) {
  const initial = await fetchPage2(`/anime/${slug}`);
  let state = initialPage(initial.raw, initial.cookies);
  const items = [...state.items];
  let pages = 1;
  while (state.hasMore && state.cursor && items.length < limit && pages < maxPages) {
    state = await loadPage(state, slug);
    items.push(...state.items);
    pages++;
  }
  const episodes = parseEpisodes(items);
  if (!episodes.length) throw new Error(`AniZone has no episodes for ${slug}`);
  return episodes;
}
__name(scrapeSeries4, "scrapeSeries");
function chooseMode(episodes, expected, offset) {
  if (!expected || !offset) return "local";
  const local = episodes.filter((episode) => episode.number >= 1 && episode.number <= expected).length;
  const shifted = episodes.filter((episode) => episode.number > offset && episode.number <= offset + expected).length;
  return shifted > local ? "offset" : "local";
}
__name(chooseMode, "chooseMode");
function ordinal(value) {
  const match = String(value || "").toLowerCase().match(/\b(?:part|special|chapter)\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\b/);
  if (!match) return 0;
  const words = { one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9, ten: 10 };
  return Number(match[1]) || words[match[1]] || 0;
}
__name(ordinal, "ordinal");
function alignEpisodes(episodes, media, expected) {
  if (!expected || episodes.length <= expected) return episodes;
  const titles = [media?.title?.english, media?.title?.romaji, media?.title?.native].filter(Boolean);
  const target = Math.max(0, ...titles.map(ordinal));
  if (target < 2) return episodes;
  const start = episodes.findIndex((episode) => ordinal(episode.title) === target);
  if (start < 0 || episodes.length - start < expected) return episodes;
  return episodes.slice(start, start + expected).map((episode, index) => ({ ...episode, number: index + 1 }));
}
__name(alignEpisodes, "alignEpisodes");
function buildEpisodeLists5(anilistId, episodes, mode, offset, ctx, expected) {
  const sub = [];
  const dub = [];
  for (const source of episodes) {
    const number = mode === "offset" ? source.number - offset : source.number;
    if (number < 1 || expected && number > expected) continue;
    const meta = episodeMeta(number, ctx);
    const base = {
      number,
      title: meta.title ?? source.title ?? `Episode ${number}`,
      duration: meta.duration ?? source.duration,
      filler: meta.filler,
      uncensored: meta.uncensored,
      description: meta.description ?? source.description,
      image: meta.image ?? source.image,
      airDate: meta.airDate ?? source.airDate,
      sourceNumber: source.sourceNumber
    };
    if (source.hasSub) sub.push({ id: `watch/anizone/${anilistId}/sub/anizone-${number}`, ...base, audio: "sub" });
    if (source.hasDub) dub.push({ id: `watch/anizone/${anilistId}/dub/anizone-${number}`, ...base, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists5, "buildEpisodeLists");
async function seriesEpisodes(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const [series, offset] = await Promise.all([
    resolveSeries6(anilistId, localCtx),
    getPrequelOffset(anilistId).catch(() => 0)
  ]);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const limit = expected ? expected + offset : Infinity;
  const maxPages = Number.isFinite(ctx.maxPages) ? Math.max(1, ctx.maxPages) : Infinity;
  const rawEpisodes = await scrapeSeries4(series.slug, limit, maxPages);
  const mode = chooseMode(rawEpisodes, expected, offset);
  return {
    media,
    ctx: localCtx,
    series,
    offset,
    expected,
    mode,
    episodes: alignEpisodes(rawEpisodes, media, expected)
  };
}
__name(seriesEpisodes, "seriesEpisodes");
async function getEpisodes8(anilistId, ctx = {}) {
  const data = await seriesEpisodes(anilistId, ctx);
  return {
    meta: {
      id: data.series.slug,
      title: data.series.title,
      source: "anizone",
      matchScore: Number(data.series.matchScore.toFixed(3)),
      numbering: data.mode,
      episodeOffset: data.mode === "offset" ? data.offset : 0
    },
    episodes: buildEpisodeLists5(anilistId, data.episodes, data.mode, data.offset, data.ctx, data.expected)
  };
}
__name(getEpisodes8, "getEpisodes");
async function scrapeWatch(slug, episode) {
  const { raw } = await fetchPage2(`/anime/${slug}/${episode}`);
  const player = playerData(raw);
  if (!player?.src) {
    const error = new Error(`AniZone player payload not found for episode ${episode}`);
    error.rawBody = raw;
    throw error;
  }
  return {
    hls: normalizeUrl(player.src),
    subtitles: (Array.isArray(player.subtitles) ? player.subtitles : []).filter((subtitle) => subtitle?.file).map((subtitle) => ({
      url: normalizeUrl(subtitle.file),
      label: subtitle.title || "",
      srclang: subtitle.language || "",
      format: subtitle.format || "vtt",
      default: Boolean(subtitle.default)
    })),
    storyboard: normalizeUrl(player.storyboard) || null,
    chapters: normalizeUrl(player.chapter) || null
  };
}
__name(scrapeWatch, "scrapeWatch");
async function handleWatch9(anilistId, audio, epNum, ctx = {}) {
  const data = await seriesEpisodes(anilistId, ctx);
  const episode = data.episodes.find((item) => {
    const number = data.mode === "offset" ? item.number - data.offset : item.number;
    return number === Number(epNum);
  });
  if (!episode || audio === "sub" && !episode.hasSub || audio === "dub" && !episode.hasDub) {
    throw new Error(`AniZone ${audio} episode ${epNum} not found`);
  }
  const watch = await scrapeWatch(data.series.slug, episode.sourceNumber);
  return json2({
    anilistId: Number(anilistId),
    episode: Number(epNum),
    providerEpisode: episode.sourceNumber,
    audio,
    streams: [{
      url: watch.hls,
      type: "hls",
      server: "AniZone",
      subtitles: watch.subtitles,
      storyboard: watch.storyboard,
      chapters: watch.chapters,
      priority: 1,
      isActive: true
    }]
  });
}
__name(handleWatch9, "handleWatch");
var anizone_default = {
  async fetch(request2) {
    const url = new URL(request2.url);
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    try {
      const match = url.pathname.match(/^\/watch\/anizone\/(\d+)\/(sub|dub)\/anizone-(\d+)\/?$/);
      if (match) return await handleWatch9(match[1], match[2], match[3]);
      return json2({ error: "Not found" }, 404);
    } catch (error) {
      return json2({ error: error.message, "Raw-ERROR": error.rawBody ?? null, stack: error.stack }, 500);
    }
  }
};

// providers/aniwaves.js
var BASE8 = "https://aniwaves.ru";
var UA11 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36";
function escapeRegex2(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
__name(escapeRegex2, "escapeRegex");
function stripHtml(value = "") {
  return decodeEntities(value.replace(/<[^>]*>/g, " ").replace(/\s+/g, " "));
}
__name(stripHtml, "stripHtml");
function attribute(tag, name) {
  const match = String(tag).match(new RegExp(`\\b${escapeRegex2(name)}=["']([^"']*)["']`, "i"));
  return match ? decodeEntities(match[1]) : "";
}
__name(attribute, "attribute");
function formatName2(value) {
  const name = String(value || "").toUpperCase();
  if (name.includes("SPECIAL")) return "special";
  if (name === "TV" || name === "TV_SHORT") return "tv";
  if (name === "MOVIE") return "movie";
  if (name === "OVA") return "ova";
  if (name === "ONA") return "ona";
  if (name === "SPECIAL") return "special";
  return "";
}
__name(formatName2, "formatName");
function searchQueries3(titles) {
  const queries = /* @__PURE__ */ new Set();
  for (const raw of titles.slice(0, 8)) {
    const title = String(raw || "").replace(/\s+/g, " ").trim();
    if (!title) continue;
    queries.add(title);
    const plain = title.replace(/[^\p{L}\p{N}]+/gu, " ").replace(/\s+/g, " ").trim();
    if (plain.length >= 3) queries.add(plain);
    const words = plain.split(/\s+/).filter(Boolean);
    if (words.length > 4) queries.add(words.slice(0, 4).join(" "));
    if (words.length > 6) queries.add(words.slice(0, 6).join(" "));
    const family = plain.replace(/\b(?:the\s+)?final\s+chapters?\b/gi, " ").replace(/\bfinal\s+(?:arc|edition)\b/gi, " ").replace(/\b(?:kanketsu|kouhen|zenpen)\s*(?:hen)?\b/gi, " ").replace(/\b(?:the\s+)?movie\b/gi, " ").replace(/\b(?:season|part|cour|chapter)\s*(?:\d+|one|two|three|four|final)?\b/gi, " ").replace(/\b(?:final|special)\s*(?:\d+|one|two|three|four)?\b/gi, " ").replace(/\s+/g, " ").trim();
    if (family.length >= 3) queries.add(family);
  }
  return [...queries].filter((query) => query.length >= 3).slice(0, 18);
}
__name(searchQueries3, "searchQueries");
async function fetchText4(url, headers = {}) {
  const response = await fetch(url, {
    headers: {
      "User-Agent": UA11,
      "Accept-Language": "en-US,en;q=0.9",
      ...headers
    }
  });
  const raw = await response.text();
  if (!response.ok) {
    const error = new Error(`AniWaves HTTP ${response.status}: ${url}`);
    error.rawBody = raw;
    throw error;
  }
  return raw;
}
__name(fetchText4, "fetchText");
async function fetchAjax(path, referer) {
  const raw = await fetchText4(`${BASE8}${path}`, {
    "Accept": "application/json, text/javascript, */*; q=0.01",
    "X-Requested-With": "XMLHttpRequest",
    "Referer": referer
  });
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    const error = new Error(`AniWaves returned invalid JSON: ${path}`);
    error.rawBody = raw;
    throw error;
  }
  if (Number(data?.status) !== 200) {
    const error = new Error(data?.message || `AniWaves request failed: ${path}`);
    error.rawBody = raw;
    throw error;
  }
  return data.result;
}
__name(fetchAjax, "fetchAjax");
function parseSearchCards(html2) {
  const found = /* @__PURE__ */ new Map();
  for (const match of html2.matchAll(/<a\b([^>]*)>([\s\S]*?)<\/a>/gi)) {
    const tag = match[1];
    if (!/\bclass=["'][^"']*\bname\b[^"']*\bd-title\b/i.test(tag)) continue;
    const href = attribute(tag, "href");
    const slug = href.match(/^\/watch\/([a-z0-9-]+)$/i)?.[1];
    if (!slug || found.has(slug)) continue;
    const siteId = Number(slug.match(/-(\d+)$/)?.[1]);
    if (!Number.isFinite(siteId)) continue;
    const title = stripHtml(match[2]);
    if (!title) continue;
    found.set(slug, {
      slug,
      siteId,
      title,
      japanese: attribute(tag, "data-jp")
    });
  }
  return [...found.values()];
}
__name(parseSearchCards, "parseSearchCards");
async function search6(query) {
  const html2 = await fetchText4(`${BASE8}/filter?keyword=${encodeURIComponent(query)}`, {
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Referer": `${BASE8}/`
  });
  return parseSearchCards(html2);
}
__name(search6, "search");
function detailField(html2, label) {
  const match = html2.match(new RegExp(`<div>\\s*${escapeRegex2(label)}:\\s*<span[^>]*>([\\s\\S]*?)<\\/span>`, "i"));
  return match ? stripHtml(match[1]) : "";
}
__name(detailField, "detailField");
function parseEpisodeCount(value) {
  const numbers = [...String(value).matchAll(/\d+/g)].map((match) => Number(match[0])).filter(Number.isFinite);
  return { available: numbers[0] ?? 0, total: numbers[1] ?? numbers[0] ?? 0 };
}
__name(parseEpisodeCount, "parseEpisodeCount");
async function fetchDetail(candidate) {
  const html2 = await fetchText4(`${BASE8}/watch/${candidate.slug}`, {
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Referer": `${BASE8}/`
  });
  const premiered = detailField(html2, "Premiered");
  const aired = detailField(html2, "Date aired");
  const year = Number(aired.match(/\d{4}/)?.[0] ?? premiered.match(/\d{4}/)?.[0] ?? "");
  return {
    ...candidate,
    title: candidate.title || stripHtml(html2.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)?.[1] ?? ""),
    type: formatName2(detailField(html2, "Type")),
    year: Number.isFinite(year) ? year : null,
    episodes: parseEpisodeCount(detailField(html2, "Episodes"))
  };
}
__name(fetchDetail, "fetchDetail");
function candidateTitleScore2(titles, candidate) {
  const values = [candidate.title, candidate.japanese, candidate.slug.replace(/-/g, " ")].filter(Boolean);
  let best = 0;
  for (const title of titles) {
    for (const value of values) best = Math.max(best, diceCoeff(title, value));
  }
  return best;
}
__name(candidateTitleScore2, "candidateTitleScore");
function coverageScore2(candidate, expected, status) {
  if (!expected || expected < 1) return 0.5;
  if (candidate.episodes.available < 1) return 0;
  if (expected < 6) return 1;
  const needed = status === "FINISHED" ? Math.ceil(expected * 0.8) : Math.max(1, expected - 3);
  return Math.min(1, candidate.episodes.available / needed);
}
__name(coverageScore2, "coverageScore");
function validateCandidate2(candidate, media, titles, expected) {
  const titleScore2 = candidateTitleScore2(titles, candidate);
  const expectedType = formatName2(media?.format);
  const expectedYear = Number(media?.startDate?.year ?? media?.seasonYear ?? 0) || null;
  if (titleScore2 < 0.68) return null;
  if (expectedType && candidate.type && expectedType !== candidate.type) return null;
  if (expectedYear && candidate.year && expectedYear !== candidate.year) return null;
  const coverage = coverageScore2(candidate, expected, media?.status);
  if (expected >= 6 && coverage < 0.8) return null;
  const score = titleScore2 * 0.72 + (expectedType && candidate.type === expectedType ? 0.14 : 0.07) + (expectedYear && candidate.year === expectedYear ? 0.1 : 0.04) + coverage * 0.04;
  return { ...candidate, titleScore: titleScore2, coverage, score };
}
__name(validateCandidate2, "validateCandidate");
async function resolveSeries7(anilistId, ctx = {}) {
  const cacheKey = `np:aniwaves:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const discovered = /* @__PURE__ */ new Map();
  await Promise.all(searchQueries3(titles).map(async (query) => {
    try {
      for (const candidate of await search6(query)) if (!discovered.has(candidate.slug)) discovered.set(candidate.slug, candidate);
    } catch {
    }
  }));
  const shortlist = [...discovered.values()].map((candidate) => ({ candidate, score: candidateTitleScore2(titles, candidate) })).filter((item) => item.score >= 0.5).sort((left, right) => right.score - left.score).slice(0, 12).map((item) => item.candidate);
  const details = await Promise.all(shortlist.map((candidate) => fetchDetail(candidate).catch(() => null)));
  const valid = details.filter(Boolean).map((candidate) => validateCandidate2(candidate, media, titles, expected)).filter(Boolean).sort((left, right) => right.score - left.score);
  const selected = valid[0];
  const runnerUp = valid[1];
  if (!selected || selected.score < 0.82 || runnerUp && selected.score - runnerUp.score < 0.08) {
    throw new Error(`AniWaves match not confident for AniList ${anilistId}`);
  }
  const data = {
    siteId: selected.siteId,
    slug: selected.slug,
    title: selected.title,
    score: selected.score,
    matchScore: selected.titleScore,
    episodeCount: selected.episodes.available
  };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries7, "resolveSeries");
function parseEpisodes2(html2) {
  const episodes = [];
  const seen = /* @__PURE__ */ new Set();
  for (const match of html2.matchAll(/<a\b([^>]*)>([\s\S]*?)<\/a>/gi)) {
    const attrs = match[1];
    const number = Number(attribute(attrs, "data-num"));
    const sourceNumber = attribute(attrs, "data-slug") || String(number);
    if (!Number.isFinite(number) || number < 1 || seen.has(number)) continue;
    const ids = attribute(attrs, "data-ids");
    if (!ids) continue;
    seen.add(number);
    episodes.push({
      number,
      sourceNumber,
      ids,
      title: stripHtml(match[2]).replace(/^\d+\s*/, "") || `Episode ${number}`,
      airDate: attribute(attrs, "data-aired") || null,
      duration: Number(attribute(attrs, "data-duration")) || null,
      filler: attribute(attrs, "data-filler") === "1",
      recap: attribute(attrs, "data-recap") === "1",
      hasSub: attribute(attrs, "data-sub") === "1",
      hasDub: attribute(attrs, "data-dub") === "1"
    });
  }
  return episodes.sort((left, right) => left.number - right.number);
}
__name(parseEpisodes2, "parseEpisodes");
async function fetchEpisodes(series) {
  const result = await fetchAjax(`/ajax/episode/list/${series.siteId}?vrf=`, `${BASE8}/watch/${series.slug}`);
  const episodes = parseEpisodes2(String(result || ""));
  if (!episodes.length) throw new Error(`AniWaves has no episodes for ${series.slug}`);
  return episodes;
}
__name(fetchEpisodes, "fetchEpisodes");
function ordinal2(value) {
  const match = String(value || "").toLowerCase().match(/\b(?:part|special|chapter)\s*(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\b/);
  if (!match) return 0;
  const word = match[1];
  const numbers = { one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9, ten: 10 };
  return Number(word) || numbers[word] || 0;
}
__name(ordinal2, "ordinal");
function alignEpisodes2(sourceEpisodes, media, expected) {
  if (!expected || sourceEpisodes.length <= expected) return sourceEpisodes;
  const targetTitles = [media?.title?.english, media?.title?.romaji, media?.title?.native].filter(Boolean);
  const targetOrdinal = Math.max(0, ...targetTitles.map(ordinal2));
  if (targetOrdinal < 2) return sourceEpisodes;
  const start = sourceEpisodes.findIndex((episode) => ordinal2(episode.title) === targetOrdinal);
  if (start < 0 || sourceEpisodes.length - start < expected) return sourceEpisodes;
  return sourceEpisodes.slice(start, start + expected).map((episode, index) => ({ ...episode, number: index + 1 }));
}
__name(alignEpisodes2, "alignEpisodes");
function buildEpisodeLists6(anilistId, sourceEpisodes, ctx, expected) {
  const sub = [];
  const dub = [];
  for (const source of sourceEpisodes) {
    if (expected && source.number > expected) continue;
    const meta = episodeMeta(source.number, ctx);
    const base = {
      number: source.number,
      title: meta.title ?? source.title,
      duration: meta.duration ?? source.duration ?? null,
      filler: meta.filler ?? source.filler,
      uncensored: meta.uncensored,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate ?? source.airDate,
      recap: source.recap,
      sourceNumber: source.sourceNumber
    };
    if (source.hasSub) sub.push({ ...base, id: `watch/aniwaves/${anilistId}/sub/aniwaves-${source.number}`, audio: "sub" });
    if (source.hasDub) dub.push({ ...base, id: `watch/aniwaves/${anilistId}/dub/aniwaves-${source.number}`, audio: "dub" });
  }
  return { sub, dub };
}
__name(buildEpisodeLists6, "buildEpisodeLists");
async function getEpisodes9(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const [series, expected] = await Promise.all([
    resolveSeries7(anilistId, localCtx),
    Promise.resolve(expectedCount(media, ctx.anizip, ctx.jikanEps))
  ]);
  const episodes = alignEpisodes2(await fetchEpisodes(series), media, expected);
  return {
    meta: {
      id: series.slug,
      title: series.title,
      source: "aniwaves",
      matchScore: Number(series.matchScore.toFixed(3)),
      numbering: "standard",
      episodeOffset: 0
    },
    episodes: buildEpisodeLists6(anilistId, episodes, localCtx, expected)
  };
}
__name(getEpisodes9, "getEpisodes");
function parseServerGroups(html2) {
  const groups = [];
  const markers = [...html2.matchAll(/<div\b([^>]*)>/gi)].map((match) => ({ index: match.index, attrs: match[1], type: attribute(match[1], "data-type") })).filter((item) => item.type === "sub" || item.type === "dub");
  for (let index = 0; index < markers.length; index++) {
    const current = markers[index];
    const end = markers[index + 1]?.index ?? html2.length;
    const segment = html2.slice(current.index, end);
    for (const match of segment.matchAll(/<li\b([^>]*)>([\s\S]*?)<\/li>/gi)) {
      const attrs = match[1];
      const linkId = attribute(attrs, "data-link-id");
      if (!linkId) continue;
      groups.push({
        audio: current.type,
        linkId,
        serverId: attribute(attrs, "data-sv-id") || null,
        server: stripHtml(match[2]) || "AniWaves"
      });
    }
  }
  return groups;
}
__name(parseServerGroups, "parseServerGroups");
async function fetchServers(series, episode) {
  const result = await fetchAjax(
    `/ajax/server/list?servers=${encodeURIComponent(series.siteId)}&eps=${encodeURIComponent(episode.sourceNumber)}`,
    `${BASE8}/watch/${series.slug}/ep-${episode.sourceNumber}`
  );
  return parseServerGroups(String(result || ""));
}
__name(fetchServers, "fetchServers");
async function fetchSource(linkId, referer) {
  const result = await fetchAjax(`/ajax/sources?id=${encodeURIComponent(linkId)}&asi=0&autoPlay=0`, referer);
  if (!result?.url) throw new Error("AniWaves source response has no embed url");
  return result;
}
__name(fetchSource, "fetchSource");
async function resolveSource(source, referer) {
  const extractor = findVideoExtractor(source.url);
  if (!extractor) return [];
  try {
    const streams = await extractor.extract(source.url, { userAgent: UA11, referer });
    return streams.map((stream) => typeof stream === "string" ? { url: stream, type: "hls" } : stream);
  } catch {
    return [];
  }
}
__name(resolveSource, "resolveSource");
function skipRange(value) {
  if (!Array.isArray(value) || value.length < 2) return null;
  const start = Number(value[0]);
  const end = Number(value[1]);
  return Number.isFinite(start) && Number.isFinite(end) && end > start ? { start, end } : null;
}
__name(skipRange, "skipRange");
async function handleWatch10(anilistId, audio, epNum, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const series = await resolveSeries7(anilistId, { ...ctx, media });
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const episodes = alignEpisodes2(await fetchEpisodes(series), media, expected);
  const episode = episodes.find((item) => item.number === Number(epNum));
  if (!episode || audio === "sub" && !episode.hasSub || audio === "dub" && !episode.hasDub) {
    throw new Error(`AniWaves ${audio} episode ${epNum} not found`);
  }
  const servers = (await fetchServers(series, episode)).filter((server) => server.audio === audio);
  if (!servers.length) throw new Error(`AniWaves has no ${audio} servers for episode ${epNum}`);
  const referer = `${BASE8}/watch/${series.slug}/ep-${episode.sourceNumber}`;
  const settled = await Promise.all(servers.map(async (server) => {
    try {
      return { server, source: await fetchSource(server.linkId, referer) };
    } catch (error) {
      return { server, error };
    }
  }));
  const resolved2 = await Promise.all(settled.map(async (item) => ({
    ...item,
    direct: item.source?.url ? await resolveSource(item.source, referer) : []
  })));
  const streams = [];
  let intro = null;
  let outro = null;
  for (const item of resolved2) {
    if (!item.source?.url) continue;
    const sourceReferer = (() => {
      try {
        return `${new URL(item.source.url).origin}/`;
      } catch {
        return referer;
      }
    })();
    const skip = item.source.skip_data ?? {};
    intro ??= skipRange(skip.intro);
    outro ??= skipRange(skip.outro);
    for (const stream of item.direct) {
      streams.push({
        url: stream.url,
        type: stream.type,
        server: item.server.server,
        referer: sourceReferer,
        quality: stream.quality,
        priority: streams.length ? 4 : 5,
        isActive: streams.length === 0
      });
    }
    streams.push({
      url: item.source.url,
      type: "embed",
      server: item.server.server,
      referer: sourceReferer,
      priority: streams.length ? 4 : 5,
      isActive: streams.length === 0
    });
  }
  if (!streams.length) {
    const failure = settled.find((item) => item.error)?.error;
    throw failure ?? new Error(`AniWaves sources unavailable for episode ${epNum}`);
  }
  return json2({
    anilistId: Number(anilistId),
    episode: Number(epNum),
    providerEpisode: episode.number,
    audio,
    intro,
    outro,
    streams
  });
}
__name(handleWatch10, "handleWatch");
var aniwaves_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    try {
      const match = url.pathname.match(/^\/watch\/aniwaves\/(\d+)\/(sub|dub)\/aniwaves-(\d+)\/?$/);
      if (match) return await handleWatch10(match[1], match[2], match[3]);
      return json2({ error: "Not found" }, 404);
    } catch (error) {
      return json2({ error: error.message, "Raw-ERROR": error.rawBody ?? null, stack: error.stack }, 500);
    }
  }
};

// providers/anibd.js
var BASE9 = "https://epeng.animeapps.top";
var UA12 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
async function fetchJson4(url) {
  const res = await fetch(url, { headers: { "User-Agent": UA12, Accept: "application/json" } });
  if (!res.ok) throw new Error(`anibd ${res.status}: ${url}`);
  return res.json();
}
__name(fetchJson4, "fetchJson");
async function fetchHtml2(url, referer) {
  const res = await fetch(url, {
    headers: {
      "User-Agent": UA12,
      Accept: "text/html,application/xhtml+xml",
      ...referer ? { Referer: referer } : {}
    }
  });
  if (!res.ok) throw new Error(`anibd ${res.status}: ${url}`);
  return res.text();
}
__name(fetchHtml2, "fetchHtml");
async function fetchServers2(anilistId) {
  const data = await fetchJson4(`${BASE9}/api2.php?epid=${anilistId}`);
  return Array.isArray(data) ? data : [];
}
__name(fetchServers2, "fetchServers");
async function fetchPlayerLinks(providerLink) {
  const data = await fetchJson4(`${BASE9}/apilink.php?data=${encodeURIComponent(providerLink)}`);
  return Array.isArray(data) ? data : [];
}
__name(fetchPlayerLinks, "fetchPlayerLinks");
function extractVideoUrl(html2, origin) {
  const m = html2.match(/videoUrl\s*:\s*"([^"]+)"/);
  if (!m) return null;
  const raw = m[1];
  if (/^https?:\/\//i.test(raw)) return raw;
  return `${origin}${raw.startsWith("/") ? "" : "/"}${raw}`;
}
__name(extractVideoUrl, "extractVideoUrl");
async function resolvePlayerStream(playerLink) {
  const origin = new URL(playerLink).origin;
  const referer = `${origin}/`;
  const html2 = await fetchHtml2(playerLink, referer);
  const hls = extractVideoUrl(html2, origin);
  if (!hls) throw new Error(`anibd: no videoUrl found at ${playerLink}`);
  return { hls, referer };
}
__name(resolvePlayerStream, "resolvePlayerStream");
function audioFromServerName(name = "") {
  return /dub/i.test(name) ? "dub" : "sub";
}
__name(audioFromServerName, "audioFromServerName");
function buildEpisodeLists7(anilistId, groups, ctx, expected) {
  const sub = [];
  const dub = [];
  const seenSub = /* @__PURE__ */ new Set();
  const seenDub = /* @__PURE__ */ new Set();
  for (const group of groups) {
    const audio = audioFromServerName(group.server_name);
    for (const ep of group.server_data ?? []) {
      const number = Number(ep.name ?? ep.slug);
      if (!Number.isFinite(number) || number < 1) continue;
      if (expected && number > expected) continue;
      const bucket = audio === "dub" ? dub : sub;
      const seen = audio === "dub" ? seenDub : seenSub;
      if (seen.has(number)) continue;
      seen.add(number);
      const meta = episodeMeta(number, ctx);
      bucket.push({
        id: `watch/anibd/${anilistId}/${audio}/anibd-${number}`,
        number,
        title: meta.title ?? `Episode ${number}`,
        duration: meta.duration,
        filler: meta.filler,
        uncensored: meta.uncensored,
        description: meta.description,
        image: meta.image,
        airDate: meta.airDate,
        sourceLink: ep.link,
        audio
      });
    }
  }
  sub.sort((a, b) => a.number - b.number);
  dub.sort((a, b) => a.number - b.number);
  return { sub, dub };
}
__name(buildEpisodeLists7, "buildEpisodeLists");
async function getEpisodes10(anilistId, ctx = {}) {
  const groups = await fetchServers2(anilistId);
  if (!groups.length) throw new Error(`anibd: no episodes found for AniList ${anilistId}`);
  const expected = expectedCount(ctx.media, ctx.anizip, ctx.jikanEps);
  return {
    meta: {
      id: String(anilistId),
      source: "anibd",
      matchScore: 1,
      numbering: "standard",
      episodeOffset: 0
    },
    episodes: buildEpisodeLists7(anilistId, groups, ctx, expected)
  };
}
__name(getEpisodes10, "getEpisodes");
async function findEpisodeLink(anilistId, audio, epNum) {
  const groups = await fetchServers2(anilistId);
  for (const group of groups) {
    if (audioFromServerName(group.server_name) !== audio) continue;
    for (const ep of group.server_data ?? []) {
      if (Number(ep.name ?? ep.slug) === Number(epNum)) return ep.link;
    }
  }
  return null;
}
__name(findEpisodeLink, "findEpisodeLink");
async function handleWatch11(anilistId, audio, epNum) {
  const providerLink = await findEpisodeLink(anilistId, audio, epNum);
  if (!providerLink) return json2({ error: `anibd episode ${epNum} not found` }, 404);
  const servers = await fetchPlayerLinks(providerLink);
  const streams = [];
  let activeAssigned = false;
  for (const entry of servers) {
    if (!entry?.link) continue;
    try {
      const { hls, referer } = await resolvePlayerStream(entry.link);
      streams.push({
        url: hls,
        type: "hls",
        server: entry.server ?? "AniBD",
        referer,
        priority: activeAssigned ? 4 : 5,
        isActive: !activeAssigned
      });
      activeAssigned = true;
    } catch {
      streams.push({
        url: entry.link,
        type: "embed",
        server: entry.server ?? "AniBD",
        referer: `${new URL(entry.link).origin}/`,
        priority: 1,
        isActive: false
      });
    }
  }
  return json2({ anilistId: Number(anilistId), episode: Number(epNum), audio, streams });
}
__name(handleWatch11, "handleWatch");
var anibd_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    try {
      const m = url.pathname.match(/^\/watch\/anibd\/(\d+)\/(sub|dub)\/anibd-(\d+)\/?$/);
      if (m) return await handleWatch11(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// providers/senshi.js
var BASE10 = "https://senshi.live";
var UA13 = "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0";
var H2 = { "User-Agent": UA13, "Referer": `${BASE10}/` };
async function fetchEpisodeList(malId) {
  const res = await fetch(`${BASE10}/episodes/${malId}`, { headers: H2 });
  if (!res.ok) throw new Error(`Senshi episodes ${res.status} (MAL ${malId})`);
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}
__name(fetchEpisodeList, "fetchEpisodeList");
async function fetchEmbeds(malId, epNum) {
  const res = await fetch(`${BASE10}/episode-embeds/${malId}/${epNum}`, { headers: H2 });
  if (!res.ok) throw new Error(`Senshi embeds ${res.status} (MAL ${malId} ep ${epNum})`);
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}
__name(fetchEmbeds, "fetchEmbeds");
async function resolveMalId(anilistId) {
  const cacheKey = `np:senshi:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = await getMedia(anilistId);
  if (!media?.idMal) throw new Error(`Senshi: no MAL ID found for AniList ${anilistId}`);
  set(cacheKey, media.idMal, SHOW_IDENTITY_TTL);
  return media.idMal;
}
__name(resolveMalId, "resolveMalId");
function isDub(status) {
  return (status ?? "").toLowerCase() === "dub";
}
__name(isDub, "isDub");
async function getEpisodes11(anilistId, ctx = {}) {
  const malId = await resolveMalId(anilistId);
  const items = await fetchEpisodeList(malId);
  if (!items.length) {
    throw new Error(`Senshi: no episodes for AniList ${anilistId} (MAL ${malId})`);
  }
  let hasDub = false;
  try {
    const probe = await fetchEmbeds(malId, 1);
    hasDub = probe.some((e) => isDub(e.status));
  } catch {
  }
  const sub = [];
  const dub = [];
  for (const item of items) {
    const num = item.ep_id;
    const meta = episodeMeta(num, ctx);
    const title = item.ep_title || meta.title || `Episode ${num}`;
    const duration = meta.duration;
    const filler = item.ep_filler || meta.filler || false;
    const recap = item.ep_recap || false;
    const description = meta.description;
    const image = meta.image;
    const airDate = meta.airDate;
    sub.push({
      id: `watch/senshi/${anilistId}/sub/senshi-${num}`,
      number: num,
      title,
      duration,
      audio: "sub",
      filler,
      recap,
      uncensored: false,
      description,
      image,
      airDate
    });
    if (hasDub) {
      dub.push({
        id: `watch/senshi/${anilistId}/dub/senshi-${num}`,
        number: num,
        title,
        duration,
        audio: "dub",
        filler,
        recap,
        uncensored: false,
        description,
        image,
        airDate
      });
    }
  }
  sub.sort((a, b) => a.number - b.number);
  dub.sort((a, b) => a.number - b.number);
  return {
    meta: {
      title: ctx.media?.title?.english ?? ctx.media?.title?.romaji ?? null,
      malId,
      source: "senshi"
    },
    episodes: { sub, dub }
  };
}
__name(getEpisodes11, "getEpisodes");
async function handleWatch12(anilistId, audio, epNum) {
  const malId = await resolveMalId(anilistId);
  const embeds = await fetchEmbeds(malId, epNum);
  if (!embeds.length) {
    return json2({ error: `Senshi: no sources for episode ${epNum}` }, 404);
  }
  const wantDub = audio === "dub";
  const source = embeds.find((e) => wantDub ? isDub(e.status) : !isDub(e.status));
  if (!source) {
    return json2({ error: `Senshi: no ${audio} source for episode ${epNum}` }, 404);
  }
  const list = await fetchEpisodeList(malId).catch(() => []);
  const epItem = list.find((item) => Number(item.ep_id) === Number(epNum));
  const intro = {
    start: epItem?.intro_start ?? 0,
    end: epItem?.intro_end ?? 0
  };
  const outro = {
    start: epItem?.outro_start ?? 0,
    end: epItem?.outro_end ?? 0
  };
  const streams = [];
  const downloads = [];
  if (source.url) {
    streams.push({
      url: source.url,
      type: "hls",
      server: "Senshi",
      referer: `${BASE10}/`,
      priority: 5,
      isActive: true
    });
  }
  if (source.server2) {
    streams.push({
      url: source.server2,
      type: "embed",
      server: "StreamNin",
      referer: `${BASE10}/`,
      priority: 3,
      isActive: false
    });
  }
  if (source.serverFM) {
    streams.push({
      url: source.serverFM,
      type: "embed",
      server: "FileMoon",
      referer: `${BASE10}/`,
      priority: 2,
      isActive: false
    });
  }
  if (source.download) {
    downloads.push({ url: source.download, label: "Download" });
  }
  return json2({
    anilistId: Number(anilistId),
    malId,
    episode: Number(epNum),
    audio,
    intro,
    outro,
    streams,
    downloads,
    headers: H2
  });
}
__name(handleWatch12, "handleWatch");
var senshi_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    try {
      const m = url.pathname.match(/^\/watch\/senshi\/(\d+)\/(sub|dub)\/senshi-(\d+)\/?$/);
      if (m) return await handleWatch12(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// providers/kickassanime.js
var BASE11 = "https://kaa.lt";
var HLS_BASE = "https://hls.krussdomi.com/manifest";
var UA14 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var H3 = { "User-Agent": UA14, Accept: "application/json" };
async function kaaSearch(query) {
  const res = await fetch(`${BASE11}/api/fsearch`, {
    method: "POST",
    headers: { ...H3, "Content-Type": "application/json" },
    body: JSON.stringify({ page: 1, query })
  });
  if (!res.ok) throw new Error(`kaa fsearch HTTP ${res.status}`);
  const data = await res.json();
  return Array.isArray(data?.result) ? data.result : [];
}
__name(kaaSearch, "kaaSearch");
async function kaaShowInfo(showSlug) {
  const res = await fetch(`${BASE11}/api/show/${showSlug}`, { headers: H3 });
  if (!res.ok) throw new Error(`kaa show HTTP ${res.status}: ${showSlug}`);
  return res.json();
}
__name(kaaShowInfo, "kaaShowInfo");
async function kaaEpisodePage(showSlug, ep) {
  const res = await fetch(
    `${BASE11}/api/show/${showSlug}/episodes?ep=${ep}&lang=ja-JP`,
    { headers: H3 }
  );
  if (!res.ok) throw new Error(`kaa episodes HTTP ${res.status}`);
  return res.json();
}
__name(kaaEpisodePage, "kaaEpisodePage");
async function kaaAllEpisodes(showSlug) {
  const first = await kaaEpisodePage(showSlug, 1);
  const pages = Array.isArray(first.pages) ? first.pages : [];
  const all = Array.isArray(first.result) ? [...first.result] : [];
  if (pages.length > 1) {
    const rest = await Promise.all(
      pages.slice(1).map(async (pg) => {
        const startEp = pg.eps?.[0];
        if (!startEp) return [];
        const d = await kaaEpisodePage(showSlug, startEp);
        return Array.isArray(d.result) ? d.result : [];
      })
    );
    for (const batch of rest) all.push(...batch);
  }
  return all;
}
__name(kaaAllEpisodes, "kaaAllEpisodes");
async function kaaEpisodeServers(showSlug, fullEpSlug) {
  const res = await fetch(
    `${BASE11}/api/show/${showSlug}/episode/${fullEpSlug}`,
    { headers: H3 }
  );
  if (!res.ok) throw new Error(`kaa episode servers HTTP ${res.status}`);
  return res.json();
}
__name(kaaEpisodeServers, "kaaEpisodeServers");
function buildKaaQueries(titles) {
  const queries = /* @__PURE__ */ new Set();
  for (const title of titles.slice(0, 4)) {
    if (/[\u3000-\u9fff\u4e00-\u9faf]/.test(title)) continue;
    const clean = title.replace(/[^\w\s]/g, " ").replace(/\s+/g, " ").trim();
    if (!clean || clean.length < 3) continue;
    const words = clean.split(" ").filter(Boolean);
    if (words.length <= 3) {
      queries.add(clean);
    } else {
      queries.add(words.slice(0, 2).join(" "));
      queries.add(words.slice(0, 3).join(" "));
    }
  }
  return [...queries];
}
__name(buildKaaQueries, "buildKaaQueries");
function scoreCandidate2(candidate, titles, seasonYear, anilistFormat) {
  const titleEn = candidate.title_en || "";
  const titleJp = candidate.title || "";
  const kaaYear = Number(candidate.year);
  const kaaType = (candidate.type || "").toLowerCase();
  let base = 0;
  for (const t of titles.slice(0, 3)) {
    if (/[\u3000-\u9fff\u4e00-\u9faf]/.test(t)) continue;
    base = Math.max(base, diceCoeff(t, titleEn), diceCoeff(t, titleJp));
  }
  let yearMult = 1;
  if (seasonYear && kaaYear) {
    const diff = Math.abs(Number(seasonYear) - kaaYear);
    if (diff === 0) yearMult = 1.2;
    else if (diff === 1) yearMult = 0.8;
    else yearMult = 0.5;
  }
  let typeMult = 1;
  const af = (anilistFormat || "").toUpperCase();
  if (af === "MOVIE" && kaaType !== "movie") typeMult = 0.25;
  else if (af !== "MOVIE" && kaaType === "movie") typeMult = 0.25;
  else if ((af === "OVA" || af === "ONA" || af === "SPECIAL") && kaaType === "tv") typeMult = 0.5;
  else if (af === "TV" && (kaaType === "ova" || kaaType === "special")) typeMult = 0.5;
  return Math.min(1, base * yearMult) * typeMult;
}
__name(scoreCandidate2, "scoreCandidate");
async function resolveSeries8(anilistId, ctx = {}) {
  const cacheKey = `np:kaa:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = ctx.media ?? await getMedia(anilistId);
  const titles = buildTitles(media, ctx.anizip);
  const queries = buildKaaQueries(titles);
  const seasonYear = media?.seasonYear;
  const format = media?.format;
  if (!queries.length) throw new Error(`KAA: no usable search queries for AniList ${anilistId}`);
  const allCandidates = /* @__PURE__ */ new Map();
  await Promise.all(
    queries.map(async (q) => {
      try {
        const results = await kaaSearch(q);
        for (const r of results) {
          if (!allCandidates.has(r.slug)) allCandidates.set(r.slug, r);
        }
      } catch {
      }
    })
  );
  if (!allCandidates.size) throw new Error(`KAA: no search results for AniList ${anilistId}`);
  const scored = [];
  for (const [, candidate] of allCandidates) {
    const score = scoreCandidate2(candidate, titles, seasonYear, format);
    if (score >= 0.5) {
      scored.push({
        slug: candidate.slug,
        title: candidate.title_en || candidate.title,
        locales: Array.isArray(candidate.locales) ? candidate.locales : [],
        score
      });
    }
  }
  scored.sort((a, b) => b.score - a.score);
  if (!scored.length) {
    throw new Error(`KAA: no confident match for AniList ${anilistId}`);
  }
  const best = scored[0];
  if (best.score < 0.6) {
    throw new Error(
      `KAA: low confidence match for AniList ${anilistId} \u2014 best "${best.slug}" score ${best.score.toFixed(3)}`
    );
  }
  const data = {
    slug: best.slug,
    title: best.title,
    locales: best.locales,
    score: best.score
  };
  set(cacheKey, data, SHOW_IDENTITY_TTL);
  return data;
}
__name(resolveSeries8, "resolveSeries");
async function buildEpMap(showSlug, showInfo) {
  if (showInfo?.type === "movie") {
    const m = (showInfo.watch_uri || "").match(/\/(ep-(\d+)-([a-f0-9]+))$/i);
    if (m) return [{ number: 1, fullSlug: m[1] }];
    return [];
  }
  const episodes = await kaaAllEpisodes(showSlug);
  return episodes.map((e) => ({
    number: e.episode_number,
    fullSlug: `ep-${e.episode_number}-${e.slug}`,
    title: e.title,
    duration: e.duration_ms ? Math.round(e.duration_ms / 1e3) : null
  }));
}
__name(buildEpMap, "buildEpMap");
async function getEpisodes12(anilistId, ctx = {}) {
  const media = ctx.media ?? await getMedia(anilistId);
  const localCtx = { ...ctx, media };
  const series = await resolveSeries8(anilistId, localCtx);
  const showInfo = await kaaShowInfo(series.slug);
  const locales = Array.isArray(showInfo.locales) ? showInfo.locales : series.locales;
  const hasDub = locales.includes("en-US");
  const epMap = await buildEpMap(series.slug, showInfo);
  if (!epMap.length) throw new Error(`KAA: no episodes found for AniList ${anilistId} (slug: ${series.slug})`);
  const expected = expectedCount(media, ctx.anizip, ctx.jikanEps);
  const sub = [];
  const dub = [];
  for (const ep of epMap) {
    const num = ep.number;
    if (!Number.isFinite(num) || num < 1) continue;
    if (expected && num > expected) continue;
    const meta = episodeMeta(num, localCtx);
    const base = {
      number: num,
      title: meta.title ?? ep.title ?? `Episode ${num}`,
      duration: meta.duration ?? ep.duration,
      filler: meta.filler,
      uncensored: false,
      description: meta.description,
      image: meta.image,
      airDate: meta.airDate
    };
    sub.push({ id: `watch/kaa/${anilistId}/sub/kaa-${num}`, ...base, audio: "sub" });
    if (hasDub) {
      dub.push({ id: `watch/kaa/${anilistId}/dub/kaa-${num}`, ...base, audio: "dub" });
    }
  }
  return {
    meta: {
      id: series.slug,
      title: series.title,
      source: "kaa",
      matchScore: Number(series.score.toFixed(3))
    },
    episodes: { sub, dub }
  };
}
__name(getEpisodes12, "getEpisodes");
async function handleWatch13(anilistId, audio, epNum) {
  const series = await resolveSeries8(anilistId);
  const showInfo = await kaaShowInfo(series.slug);
  const locales = Array.isArray(showInfo.locales) ? showInfo.locales : series.locales;
  if (audio === "dub" && !locales.includes("en-US")) {
    return json2({ error: `KAA: no English dub for AniList ${anilistId}` }, 404);
  }
  const epMap = await buildEpMap(series.slug, showInfo);
  const ep = epMap.find((e) => e.number === Number(epNum));
  if (!ep) {
    return json2({ error: `KAA: episode ${epNum} not found for AniList ${anilistId}` }, 404);
  }
  const episodeData = await kaaEpisodeServers(series.slug, ep.fullSlug);
  const servers = Array.isArray(episodeData.servers) ? episodeData.servers : [];
  if (!servers.length) {
    return json2({ error: `KAA: no streams for episode ${epNum} (AniList ${anilistId})` }, 404);
  }
  const streams = [];
  for (const s of servers) {
    if (!s.src) continue;
    const m = s.src.match(/[?&]id=([^&]+)/);
    if (!m) continue;
    streams.push({
      url: `${HLS_BASE}/${m[1]}/master.m3u8`,
      type: "hls",
      server: s.name || "KAA",
      headers: { Referer: "https://krussdomi.com/" },
      priority: 1,
      isActive: true
    });
  }
  if (!streams.length) {
    return json2({ error: `KAA: could not resolve stream for episode ${epNum}` }, 404);
  }
  return json2({
    anilistId: Number(anilistId),
    episode: Number(epNum),
    audio,
    streams
  });
}
__name(handleWatch13, "handleWatch");
var kickassanime_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    try {
      const m = url.pathname.match(/^\/watch\/kaa\/(\d+)\/(sub|dub)\/kaa-(\d+)\/?$/);
      if (m) return await handleWatch13(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// providers/animedunya.js
var BASE12 = "https://anime-dunya.com";
async function resolveMalId2(anilistId) {
  const cacheKey = `np:animedunya:${anilistId}`;
  const cached = get(cacheKey);
  if (isFresh(cached)) return cached.data;
  const media = await getMedia(anilistId);
  if (!media?.idMal) throw new Error("AnimeDunya: no MAL ID found");
  set(cacheKey, media.idMal, SHOW_IDENTITY_TTL);
  return media.idMal;
}
__name(resolveMalId2, "resolveMalId");
async function fetchHtml3(url) {
  const res = await fetch(url, {
    headers: {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9"
    }
  });
  if (!res.ok) return null;
  return res.text();
}
__name(fetchHtml3, "fetchHtml");
function extractEpisodesList(html2) {
  const match = html2.match(/\\?"episodes\\?":\s*\[/);
  if (!match) return [];
  const idx = match.index;
  const matchLen = match[0].length;
  let braceCount = 1;
  let result = "[";
  for (let i = idx + matchLen; i < html2.length; i++) {
    const char = html2[i];
    if (char === "[") braceCount++;
    else if (char === "]") braceCount--;
    result += char;
    if (braceCount === 0) break;
  }
  try {
    const cleanStr = result.replace(/\\u0026/g, "&").replace(/\\"/g, '"').replace(/\\\\/g, "\\");
    return JSON.parse(cleanStr);
  } catch (e) {
    return [];
  }
}
__name(extractEpisodesList, "extractEpisodesList");
function extractStream(html2) {
  const match = html2.match(/\\?"stream\\?":\s*/);
  if (!match) return null;
  const idx = match.index;
  const matchLen = match[0].length;
  let braceCount = 0;
  let started = false;
  let result = "";
  for (let i = idx + matchLen; i < html2.length; i++) {
    const char = html2[i];
    if (char === "{") {
      braceCount++;
      started = true;
    } else if (char === "}") {
      braceCount--;
    }
    if (started) {
      result += char;
      if (braceCount === 0) break;
    }
  }
  try {
    const cleanStr = result.replace(/\\u0026/g, "&").replace(/\\"/g, '"').replace(/\\\\/g, "\\");
    return JSON.parse(cleanStr);
  } catch (e) {
    const sourceMatch = html2.match(/"source"\s*:\s*"([^"]+)"/);
    if (sourceMatch) {
      return { source: sourceMatch[1].replace(/\\/g, "") };
    }
    return null;
  }
}
__name(extractStream, "extractStream");
async function getEpisodes13(anilistId, ctx = {}) {
  const malId = await resolveMalId2(anilistId);
  const html2 = await fetchHtml3(`${BASE12}/en/anime/${malId}`);
  if (!html2) throw new Error("AnimeDunya: episodes fetch failed");
  let cdnBase = "https://cdn.anime-dunya.com/thumbnail/";
  let cdnExt = "small.jpg";
  const thumbMatch = html2.match(/(https?:\/\/[^\s"'`<>]+?\/thumbnail\/)([a-zA-Z0-9]+?)\/((?:small|large)\.jpg)/);
  if (thumbMatch) {
    cdnBase = thumbMatch[1];
    cdnExt = thumbMatch[3];
  }
  const episodes = extractEpisodesList(html2);
  const watchable = episodes.filter((ep) => ep.streamId !== null && ep.streamId !== void 0);
  const sub = [];
  for (const ep of watchable) {
    const epNum = ep.episodeNumber;
    const meta = episodeMeta(epNum, ctx);
    const customTitle = Array.isArray(ep.translations) ? ep.translations.find((t) => t.language === "en")?.title : ep.translations?.title;
    sub.push({
      id: `watch/animedunya/${anilistId}/sub/animedunya-${epNum}`,
      number: epNum,
      title: customTitle || meta.title || `Episode ${epNum}`,
      duration: meta.duration,
      audio: "sub",
      filler: ep.filler || meta.filler || false,
      uncensored: false,
      description: meta.description,
      image: ep.streamId ? `${cdnBase}${ep.streamId}/${cdnExt}` : meta.image,
      airDate: meta.airDate
    });
  }
  sub.sort((a, b) => a.number - b.number);
  return {
    meta: {
      title: ctx.media?.title?.english ?? ctx.media?.title?.romaji ?? null,
      malId,
      source: "animedunya"
    },
    episodes: { sub, dub: [] }
  };
}
__name(getEpisodes13, "getEpisodes");
async function handleWatch14(anilistId, audio, epNum) {
  const malId = await resolveMalId2(anilistId);
  const html2 = await fetchHtml3(`${BASE12}/en/play/${malId}/${epNum}`);
  if (!html2) return json2({ error: "AnimeDunya watch fetch failed" }, 500);
  const streamData = extractStream(html2);
  if (!streamData || !streamData.source) {
    return json2({ error: "AnimeDunya: stream source not found" }, 404);
  }
  const subtitles = (streamData.subtitles || []).map((s) => ({
    url: s.src,
    label: s.label,
    srclang: s.srclang,
    default: s.default || false
  }));
  const streams = [{
    url: streamData.source,
    type: "hls",
    server: "AnimeDunya",
    referer: `${BASE12}/`,
    subtitles,
    priority: 5,
    isActive: true
  }];
  return json2({
    anilistId: Number(anilistId),
    malId,
    episode: Number(epNum),
    audio,
    streams
  });
}
__name(handleWatch14, "handleWatch");
var animedunya_default = {
  async fetch(request2) {
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    const url = new URL(request2.url);
    try {
      const m = url.pathname.match(/^\/watch\/animedunya\/(\d+)\/(sub|dub)\/animedunya-(\d+)\/?$/);
      if (m) return await handleWatch14(m[1], m[2], m[3]);
      return json2({ error: "Not found" }, 404);
    } catch (err) {
      return json2({ error: err.message, stack: err.stack }, 500);
    }
  }
};

// core/episode-strategy.js
var JIKAN2 = "https://api.jikan.moe/v4";
var UA15 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var inflight2 = /* @__PURE__ */ new Map();
var bgRunning = /* @__PURE__ */ new Set();
function dedupe(key, fn) {
  if (inflight2.has(key)) return inflight2.get(key);
  const p = Promise.resolve().then(fn).finally(() => inflight2.delete(key));
  inflight2.set(key, p);
  return p;
}
__name(dedupe, "dedupe");
function bg(key, fn) {
  if (bgRunning.has(key)) return;
  bgRunning.add(key);
  Promise.resolve().then(fn).catch((e) => console.error(`[bg:${key}]`, e.message)).finally(() => bgRunning.delete(key));
}
__name(bg, "bg");
async function jikanPage(malId, pageNum, retries = 3) {
  for (let attempt = 0; attempt <= retries; attempt++) {
    const res = await fetch(
      `${JIKAN2}/anime/${malId}/episodes?page=${pageNum}`,
      { headers: { "User-Agent": UA15, Accept: "application/json" } }
    ).catch(() => null);
    if (!res) return null;
    if (res.status === 429) {
      const wait = (parseInt(res.headers.get("Retry-After") ?? "1") || 1) * 1e3 + attempt * 600;
      if (attempt < retries) {
        await new Promise((r) => setTimeout(r, wait));
        continue;
      }
      return null;
    }
    if (!res.ok) return null;
    return res.json();
  }
  return null;
}
__name(jikanPage, "jikanPage");
function fetchAllJikanWithCache(malId, status) {
  return dedupe(`jikan:${malId}`, () => _jikanAll(malId, status));
}
__name(fetchAllJikanWithCache, "fetchAllJikanWithCache");
async function _jikanAll(malId, status) {
  const metaKey = `jm:${malId}`;
  const meta = await getAsync(metaKey);
  const isFinished = status === "FINISHED";
  const mustCheckTotal = !isFinished && (!meta || needsRefresh(meta));
  let lastPage = meta?.data?.lastPage ?? null;
  if (mustCheckTotal || !lastPage) {
    const p1 = await jikanPage(malId, 1);
    if (!p1 && !lastPage) return [];
    if (!p1 && lastPage) return _buildPages(malId, lastPage, status);
    const newLast = p1.pagination?.last_visible_page ?? 1;
    const isP1Last = newLast === 1;
    const [p1ttl, p1ref] = jikanPageTTL(isP1Last, status);
    await setAsync(`jp:${malId}:1`, p1.data ?? [], p1ttl, p1ref);
    if (lastPage && newLast > lastPage) {
      const [stableTtl] = jikanPageTTL(false, "FINISHED");
      const oldLastEntry = await getAsync(`jp:${malId}:${lastPage}`);
      if (oldLastEntry) await setAsync(`jp:${malId}:${lastPage}`, oldLastEntry.data, stableTtl, Infinity);
      await Promise.all(
        Array.from({ length: newLast - lastPage }, (_, i) => {
          const pn = lastPage + 1 + i;
          const isLast = pn === newLast;
          return jikanPage(malId, pn).then((pd) => {
            const [t, r] = jikanPageTTL(isLast, status);
            return setAsync(`jp:${malId}:${pn}`, pd?.data ?? [], t, r);
          });
        })
      );
    }
    const [mttl, mref] = episodeTTL(status);
    await setAsync(metaKey, { lastPage: newLast }, mttl, mref);
    lastPage = newLast;
  }
  return _buildPages(malId, lastPage, status);
}
__name(_jikanAll, "_jikanAll");
async function _buildPages(malId, lastPage, status) {
  const pages = await Promise.all(
    Array.from({ length: lastPage }, (_, i) => i + 1).map(async (pn) => {
      const key = `jp:${malId}:${pn}`;
      const isLast = pn === lastPage;
      const entry = await getAsync(key);
      if (isFresh(entry)) {
        if (isLast && status === "RELEASING" && needsRefresh(entry)) {
          bg(key, async () => {
            const pd2 = await jikanPage(malId, pn);
            if (pd2) {
              const [t2, r2] = jikanPageTTL(true, status);
              await setAsync(key, pd2.data ?? [], t2, r2);
            }
          });
        }
        return entry.data;
      }
      const pd = await jikanPage(malId, pn);
      const data = pd?.data ?? [];
      const [t, r] = jikanPageTTL(isLast, status);
      await setAsync(key, data, t, r);
      return data;
    })
  );
  return pages.flat();
}
__name(_buildPages, "_buildPages");
async function withCache(key, status, fetchFn) {
  const [ttl, refreshAfter] = episodeTTL(status);
  const entry = await getAsync(key);
  if (isFresh(entry)) {
    if (needsRefresh(entry)) {
      bg(key, async () => {
        const data2 = await fetchFn();
        await setAsync(key, data2, ttl, refreshAfter);
      });
    }
    return entry.data;
  }
  const data = await fetchFn();
  await setAsync(key, data, ttl, refreshAfter);
  return data;
}
__name(withCache, "withCache");
function orderEpisodeFields(data) {
  if (!data?.episodes || typeof data.episodes !== "object") return data;
  const episodes = Object.fromEntries(Object.entries(data.episodes).map(([audio, list]) => [
    audio,
    Array.isArray(list) ? list.map(({ id, audio: itemAudio, sourceNumber, ...episode }) => ({
      ...id === void 0 ? {} : { id },
      ...sourceNumber === void 0 ? {} : { sourceNumber },
      ...itemAudio === void 0 ? {} : { audio: itemAudio },
      ...episode
    })) : list
  ]));
  return { ...data, episodes };
}
__name(orderEpisodeFields, "orderEpisodeFields");
async function safe(label, fn) {
  try {
    return { ok: true, data: orderEpisodeFields(await fn()) };
  } catch (e) {
    console.error(`[ep:${label}]`, e.message);
    return { ok: false, error: e.message, stack: e.stack };
  }
}
__name(safe, "safe");
var PROVIDER_ALIASES = {
  mkissa: "mkissa",
  reanime: "reanime",
  anikoto: "anikoto",
  animegg: "animegg",
  anineko: "anineko",
  anidbapp: "anidbapp",
  "2dhive": "2dhive",
  animenosub: "animenosub",
  anizone: "anizone",
  aniwaves: "aniwaves",
  anibd: "anibd",
  senshi: "senshi",
  kaa: "kaa",
  animedunya: "animedunya"
};
function resolveProviders(rawNames) {
  const resolved2 = /* @__PURE__ */ new Set();
  const unknown = [];
  for (const raw of rawNames) {
    const name = PROVIDER_ALIASES[raw.toLowerCase()];
    if (name) resolved2.add(name);
    else unknown.push(raw);
  }
  return { resolved: resolved2, unknown };
}
__name(resolveProviders, "resolveProviders");
function providerFns(anilistId, status, ctx) {
  return {
    mkissa: /* @__PURE__ */ __name(() => withCache(`epv:mkissa:${anilistId}`, status, () => handleEpisodes(anilistId, ctx)), "mkissa"),
    reanime: /* @__PURE__ */ __name(() => withCache(`epv:reanime:${anilistId}`, status, () => getEpisodes3(anilistId, ctx)), "reanime"),
    anikoto: /* @__PURE__ */ __name(() => withCache(`epv:anikoto:${anilistId}`, status, () => getEpisodes(anilistId, ctx)), "anikoto"),
    animegg: /* @__PURE__ */ __name(() => withCache(`epv:animegg:${anilistId}`, status, () => getEpisodes2(anilistId, ctx)), "animegg"),
    anineko: /* @__PURE__ */ __name(() => withCache(`epv:anineko:${anilistId}`, status, () => getEpisodes4(anilistId, ctx)), "anineko"),
    anidbapp: /* @__PURE__ */ __name(() => withCache(`epv:anidbapp:${anilistId}`, status, () => getEpisodes5(anilistId, ctx)), "anidbapp"),
    "2dhive": /* @__PURE__ */ __name(() => withCache(`epv:2dhive:${anilistId}`, status, () => getEpisodes6(anilistId, ctx)), "2dhive"),
    animenosub: /* @__PURE__ */ __name(() => withCache(`epv:animenosub:${anilistId}`, status, () => getEpisodes7(anilistId, ctx)), "animenosub"),
    anizone: /* @__PURE__ */ __name(() => withCache(`epv:anizone:${anilistId}`, status, () => getEpisodes8(anilistId, ctx)), "anizone"),
    aniwaves: /* @__PURE__ */ __name(() => withCache(`epv:aniwaves:${anilistId}`, status, () => getEpisodes9(anilistId, ctx)), "aniwaves"),
    anibd: /* @__PURE__ */ __name(() => withCache(`epv:anibd:${anilistId}`, status, () => getEpisodes10(anilistId, ctx)), "anibd"),
    senshi: /* @__PURE__ */ __name(() => withCache(`epv:senshi:${anilistId}`, status, () => getEpisodes11(anilistId, ctx)), "senshi"),
    kaa: /* @__PURE__ */ __name(() => withCache(`epv:kaa:${anilistId}`, status, () => getEpisodes12(anilistId, ctx)), "kaa"),
    animedunya: /* @__PURE__ */ __name(() => withCache(`epv:animedunya:${anilistId}`, status, () => getEpisodes13(anilistId, ctx)), "animedunya")
  };
}
__name(providerFns, "providerFns");
async function buildFilteredEpisodesWithCache(anilistId, providers, media, anizip) {
  const status = media?.status ?? "RELEASING";
  const malId = media?.idMal ?? null;
  const jikanEps = malId ? await fetchAllJikanWithCache(malId, status).catch(() => null) : null;
  const ctx = { media, anizip, jikanEps, maxPages: void 0 };
  const fns = providerFns(anilistId, status, ctx);
  const pairs = await Promise.all(
    [...providers].map(async (name) => {
      const result = await safe(name, fns[name]);
      return [name, result.ok ? result.data : { error: result.error, stack: result.stack }];
    })
  );
  return Object.fromEntries(pairs);
}
__name(buildFilteredEpisodesWithCache, "buildFilteredEpisodesWithCache");
async function buildEpisodesWithCache(anilistId, media, anizip) {
  const status = media?.status ?? "RELEASING";
  const malId = media?.idMal ?? null;
  const jikanEps = malId ? await fetchAllJikanWithCache(malId, status).catch(() => null) : null;
  const ctx = { media, anizip, jikanEps, maxPages: void 0 };
  const [mkissa, reanime, anikoto, animegg, anineko, anidbapp, dhive, animenosub, anizone, aniwaves, anibd, senshi, kaa, animedunya] = await Promise.all([
    safe("mkissa", () => withCache(`epv:mkissa:${anilistId}`, status, () => handleEpisodes(anilistId, ctx))),
    safe("reanime", () => withCache(`epv:reanime:${anilistId}`, status, () => getEpisodes3(anilistId, ctx))),
    safe("anikoto", () => withCache(`epv:anikoto:${anilistId}`, status, () => getEpisodes(anilistId, ctx))),
    safe("animegg", () => withCache(`epv:animegg:${anilistId}`, status, () => getEpisodes2(anilistId, ctx))),
    safe("anineko", () => withCache(`epv:anineko:${anilistId}`, status, () => getEpisodes4(anilistId, ctx))),
    safe("anidbapp", () => withCache(`epv:anidbapp:${anilistId}`, status, () => getEpisodes5(anilistId, ctx))),
    safe("2dhive", () => withCache(`epv:2dhive:${anilistId}`, status, () => getEpisodes6(anilistId, ctx))),
    safe("animenosub", () => withCache(`epv:animenosub:${anilistId}`, status, () => getEpisodes7(anilistId, ctx))),
    safe("anizone", () => withCache(`epv:anizone:${anilistId}`, status, () => getEpisodes8(anilistId, ctx))),
    safe("aniwaves", () => withCache(`epv:aniwaves:${anilistId}`, status, () => getEpisodes9(anilistId, ctx))),
    safe("anibd", () => withCache(`epv:anibd:${anilistId}`, status, () => getEpisodes10(anilistId, ctx))),
    safe("senshi", () => withCache(`epv:senshi:${anilistId}`, status, () => getEpisodes11(anilistId, ctx))),
    safe("kaa", () => withCache(`epv:kaa:${anilistId}`, status, () => getEpisodes12(anilistId, ctx))),
    safe("animedunya", () => withCache(`epv:animedunya:${anilistId}`, status, () => getEpisodes13(anilistId, ctx)))
  ]);
  return {
    mkissa: mkissa.ok ? mkissa.data : { error: mkissa.error, stack: mkissa.stack },
    reanime: reanime.ok ? reanime.data : { error: reanime.error, stack: reanime.stack },
    anikoto: anikoto.ok ? anikoto.data : { error: anikoto.error, stack: anikoto.stack },
    animegg: animegg.ok ? animegg.data : { error: animegg.error, stack: animegg.stack },
    anineko: anineko.ok ? anineko.data : { error: anineko.error, stack: anineko.stack },
    anidbapp: anidbapp.ok ? anidbapp.data : { error: anidbapp.error, stack: anidbapp.stack },
    "2dhive": dhive.ok ? dhive.data : { error: dhive.error, stack: dhive.stack },
    animenosub: animenosub.ok ? animenosub.data : { error: animenosub.error, stack: animenosub.stack },
    anizone: anizone.ok ? anizone.data : { error: anizone.error, stack: anizone.stack },
    aniwaves: aniwaves.ok ? aniwaves.data : { error: aniwaves.error, stack: aniwaves.stack },
    anibd: anibd.ok ? anibd.data : { error: anibd.error, stack: anibd.stack },
    senshi: senshi.ok ? senshi.data : { error: senshi.error, stack: senshi.stack },
    kaa: kaa.ok ? kaa.data : { error: kaa.error, stack: kaa.stack },
    animedunya: animedunya.ok ? animedunya.data : { error: animedunya.error, stack: animedunya.stack }
  };
}
__name(buildEpisodesWithCache, "buildEpisodesWithCache");

// core/episode-cache.js
var ANIZIP4 = "https://api.ani.zip/mappings";
var MIN2 = 6e4;
var HOUR2 = 60 * MIN2;
var DAY2 = 24 * HOUR2;
var FULL_TTL = 30 * DAY2;
var NORMAL_PROBE_INTERVAL = 15 * MIN2;
var AIRING_PROBE_INTERVAL = 5 * MIN2;
var AIRING_EARLY_WINDOW = 10 * MIN2;
var AIRING_FAST_WINDOW = 6 * HOUR2;
var refreshing = /* @__PURE__ */ new Set();
function runBackground(env, promise) {
  const waitUntil = env?.context?.waitUntil ?? env?.waitUntil;
  if (typeof waitUntil === "function") waitUntil.call(env.context ?? env, promise);
  else promise.catch(() => {
  });
}
__name(runBackground, "runBackground");
function latestEpisodeFromResponse(data) {
  let max = 0;
  for (const provider of Object.values(data ?? {})) {
    const episodes = provider?.episodes;
    if (!episodes || typeof episodes !== "object") continue;
    for (const list of Object.values(episodes)) {
      if (!Array.isArray(list)) continue;
      for (const ep of list) {
        const n = Number(ep?.number);
        if (Number.isFinite(n) && n > max) max = n;
      }
    }
  }
  return max || null;
}
__name(latestEpisodeFromResponse, "latestEpisodeFromResponse");
function hasCurrentProviders(data) {
  return data && Object.prototype.hasOwnProperty.call(data, "anidbapp") && Object.prototype.hasOwnProperty.call(data, "anizone") && Object.prototype.hasOwnProperty.call(data, "aniwaves");
}
__name(hasCurrentProviders, "hasCurrentProviders");
function latestEpisodeFromAniZip(anizip) {
  const nums = Object.keys(anizip?.episodes ?? {}).map(Number).filter(Number.isFinite);
  return nums.length ? Math.max(...nums) : null;
}
__name(latestEpisodeFromAniZip, "latestEpisodeFromAniZip");
function resolveShared(anilistId, freshMedia = false) {
  if (freshMedia) forgetMedia(anilistId);
  return Promise.all([
    getMedia(anilistId).catch(() => null),
    fetch(`${ANIZIP4}?anilist_id=${anilistId}`).then((r) => r.json()).catch(() => null)
  ]);
}
__name(resolveShared, "resolveShared");
async function clearProviderCache(anilistId, media) {
  for (const p of ["pahe", "manga", "reanime", "anikoto", "animegg", "anineko", "anidbapp", "2dhive", "anizone", "aniwaves"]) {
    await delAsync(`epv:${p}:${anilistId}`);
  }
  if (media?.idMal) {
    await delAsync(`jm:${media.idMal}`);
    await delByPrefixAsync(`jp:${media.idMal}:`);
  }
}
__name(clearProviderCache, "clearProviderCache");
async function buildResponse(anilistId, media, anizip, forceRefresh = false) {
  if (forceRefresh) await clearProviderCache(anilistId, media);
  const [providerResult, mappingResult] = await Promise.all([
    buildEpisodesWithCache(anilistId, media, anizip),
    mapAnimeIds(anilistId).catch(() => null)
  ]);
  return {
    page: 1,
    type: "all",
    mappings: mappingResult?.mappings ?? null,
    ...providerResult
  };
}
__name(buildResponse, "buildResponse");
function probeInterval(state) {
  const airMs = state?.nextAiringAt ? state.nextAiringAt * 1e3 : null;
  if (!airMs) return NORMAL_PROBE_INTERVAL;
  const now = Date.now();
  return now >= airMs - AIRING_EARLY_WINDOW && now <= airMs + AIRING_FAST_WINDOW ? AIRING_PROBE_INTERVAL : NORMAL_PROBE_INTERVAL;
}
__name(probeInterval, "probeInterval");
function shouldRebuild(entry, media, anizip) {
  if ((media?.status ?? "RELEASING") === "FINISHED") return false;
  const cachedLatest = latestEpisodeFromResponse(entry?.data) ?? 0;
  const knownLatest = Math.max(
    latestEpisodeFromAniZip(anizip) ?? 0,
    Number(media?.episodes) || 0
  );
  if (knownLatest > cachedLatest) return true;
  const next = media?.nextAiringEpisode;
  if (next?.episode && cachedLatest >= Number(next.episode)) return false;
  if (next?.airingAt) {
    const airMs = Number(next.airingAt) * 1e3;
    const now = Date.now();
    if (now < airMs - AIRING_EARLY_WINDOW) return false;
    if (now <= airMs + AIRING_FAST_WINDOW) return true;
  }
  return needsRefresh(entry);
}
__name(shouldRebuild, "shouldRebuild");
function writeSyncState(anilistId, state, ttl = FULL_TTL) {
  set(`sync:${anilistId}`, state, ttl, NORMAL_PROBE_INTERVAL);
}
__name(writeSyncState, "writeSyncState");
function scheduleRefresh(anilistId, entry, env) {
  const key = `ep-bg:${anilistId}`;
  if (refreshing.has(key)) return;
  const syncKey = `sync:${anilistId}`;
  const oldState = get(syncKey)?.data;
  const now = Date.now();
  if (oldState?.lastProbeAt && now - oldState.lastProbeAt < probeInterval(oldState)) return;
  refreshing.add(key);
  writeSyncState(anilistId, { ...oldState, lastProbeAt: now, syncing: true });
  const task = (async () => {
    const [media, anizip] = await resolveShared(anilistId, true);
    const cachedLatest = latestEpisodeFromResponse(entry?.data);
    const next = media?.nextAiringEpisode ?? null;
    if (!shouldRebuild(entry, media, anizip)) {
      writeSyncState(anilistId, {
        lastProbeAt: Date.now(),
        lastSyncAt: oldState?.lastSyncAt ?? null,
        latestEpisode: cachedLatest,
        nextEpisode: next?.episode ?? null,
        nextAiringAt: next?.airingAt ?? null,
        syncing: false
      });
      return;
    }
    const result = await buildResponse(anilistId, media, anizip, true);
    const latestEpisode = latestEpisodeFromResponse(result);
    await setAsync(`episodes:${anilistId}`, result, FULL_TTL, NORMAL_PROBE_INTERVAL);
    writeSyncState(anilistId, {
      lastProbeAt: Date.now(),
      lastSyncAt: Date.now(),
      latestEpisode,
      nextEpisode: next?.episode ?? null,
      nextAiringAt: next?.airingAt ?? null,
      syncing: false
    });
  })().catch((e) => {
    console.error(`[ep-bg:${anilistId}]`, e.message);
    writeSyncState(anilistId, {
      ...oldState,
      lastProbeAt: Date.now(),
      syncing: false,
      error: e.message
    }, HOUR2);
  }).finally(() => refreshing.delete(key));
  runBackground(env, task);
}
__name(scheduleRefresh, "scheduleRefresh");
async function getEpisodesResponse(anilistId, env) {
  const cacheKey = `episodes:${anilistId}`;
  const entry = await getAsync(cacheKey);
  if (entry && hasCurrentProviders(entry.data)) {
    scheduleRefresh(anilistId, entry, env);
    return entry.data;
  }
  const [media, anizip] = await resolveShared(anilistId);
  const result = await buildResponse(anilistId, media, anizip);
  await setAsync(cacheKey, result, FULL_TTL, NORMAL_PROBE_INTERVAL);
  writeSyncState(anilistId, {
    lastProbeAt: Date.now(),
    lastSyncAt: Date.now(),
    latestEpisode: latestEpisodeFromResponse(result),
    nextEpisode: media?.nextAiringEpisode?.episode ?? null,
    nextAiringAt: media?.nextAiringEpisode?.airingAt ?? null,
    syncing: false
  });
  return result;
}
__name(getEpisodesResponse, "getEpisodesResponse");
async function getFilteredEpisodesResponse(anilistId, providers, includeMap) {
  const [media, anizip] = await resolveShared(anilistId);
  const [providerResult, mappingResult] = await Promise.all([
    buildFilteredEpisodesWithCache(anilistId, providers, media, anizip),
    includeMap ? mapAnimeIds(anilistId).catch(() => null) : Promise.resolve(null)
  ]);
  return {
    page: 1,
    type: "filtered",
    ...includeMap ? { mappings: mappingResult?.mappings ?? null } : {},
    ...providerResult
  };
}
__name(getFilteredEpisodesResponse, "getFilteredEpisodesResponse");

// index.js
function json4(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=300"
    }
  });
}
__name(json4, "json");
function rewriteRequest(request2, newPath) {
  const u = new URL(request2.url);
  u.pathname = newPath;
  return new Request(u.toString(), { method: request2.method, headers: request2.headers });
}
__name(rewriteRequest, "rewriteRequest");
var watchInflight = /* @__PURE__ */ new Map();
async function cachedWatch(cacheKey, handlerFn) {
  const entry = await getAsync(cacheKey);
  if (entry && isFresh(entry)) return json4(entry.data);
  if (watchInflight.has(cacheKey)) {
    await watchInflight.get(cacheKey).catch(() => {
    });
    const warm = await getAsync(cacheKey);
    if (warm && isFresh(warm)) return json4(warm.data);
    return handlerFn();
  }
  const promise = (async () => {
    const response = await handlerFn();
    if (response.status === 200) {
      try {
        const data = await response.clone().json();
        await setAsync(cacheKey, data, WATCH_TTL);
      } catch {
      }
    }
    return response;
  })();
  watchInflight.set(cacheKey, promise);
  try {
    return await promise;
  } finally {
    watchInflight.delete(cacheKey);
  }
}
__name(cachedWatch, "cachedWatch");
var index_default = {
  async fetch(request2, env) {
    const url = new URL(request2.url);
    const path = url.pathname;
    if (request2.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Access-Control-Allow-Headers": "*"
        }
      });
    }
    if (path === "/proxy") {
      const target = url.searchParams.get("url");
      const referer = url.searchParams.get("referer") || url.searchParams.get("ref") || "https://flixcloud.cc/";
      const origin = url.searchParams.get("origin") || referer.replace(/\/$/, "");
      if (!target) {
        return json4({ error: "Missing ?url= param" }, 400);
      }
      let targetUrl;
      try {
        targetUrl = new URL(target);
      } catch {
        return json4({ error: "Invalid target URL" }, 400);
      }
      const forwardHeaders = new Headers();
      forwardHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
      forwardHeaders.set("Referer", referer);
      forwardHeaders.set("Origin", origin);
      forwardHeaders.set("Accept", "*/*");
      forwardHeaders.set("Accept-Language", "en-US,en;q=0.9");
      forwardHeaders.set("Sec-Fetch-Dest", "empty");
      forwardHeaders.set("Sec-Fetch-Mode", "cors");
      forwardHeaders.set("Sec-Fetch-Site", "cross-site");
      const range = request2.headers.get("Range");
      if (range) forwardHeaders.set("Range", range);
      try {
        const upstream = await fetch(target, { method: request2.method, headers: forwardHeaders });
        const contentType = upstream.headers.get("Content-Type") || "";
        const isM3U8 = contentType.includes("mpegurl") || contentType.includes("x-mpegurl") || targetUrl.pathname.endsWith(".m3u8") || targetUrl.pathname.endsWith(".m3u");
        const responseHeaders = new Headers(upstream.headers);
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("Access-Control-Allow-Headers", "*");
        responseHeaders.set("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges");
        if (isM3U8) {
          const text = await upstream.text();
          const lines = text.split(/\r?\n/);
          const out = [];
          for (let line of lines) {
            const trimmed = line.trim();
            if (!trimmed) {
              out.push(line);
              continue;
            }
            if (trimmed.startsWith("#")) {
              const rewrittenTag = trimmed.replace(/URI=["']([^"']+)["']/g, (m2, uri) => {
                const absUrl = new URL(uri, target).toString();
                return `URI="${url.origin}/proxy?url=${encodeURIComponent(absUrl)}&referer=${encodeURIComponent(referer)}"`;
              });
              out.push(rewrittenTag);
            } else {
              const absUrl = new URL(trimmed, target).toString();
              out.push(`${url.origin}/proxy?url=${encodeURIComponent(absUrl)}&referer=${encodeURIComponent(referer)}`);
            }
          }
          responseHeaders.set("Content-Type", "application/vnd.apple.mpegurl");
          return new Response(out.join("\n"), { status: upstream.status, headers: responseHeaders });
        }
        return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
      } catch (err) {
        return json4({ error: "Proxy fetch failed: " + err.message }, 502);
      }
    }
    let m = path.match(/^\/map\/(\d+)\/?$/);
    if (m) {
      const anilistId = m[1];
      const cacheKey = `map:${anilistId}`;
      const entry = await getAsync(cacheKey);
      if (entry && isFresh(entry)) return json4(entry.data);
      try {
        const [data, media] = await Promise.all([
          mapAnimeIds(anilistId),
          getMedia(anilistId).catch(() => null)
        ]);
        await setAsync(cacheKey, data, mapTTL(media?.status ?? "RELEASING"));
        return json4(data);
      } catch (e) {
        if (entry) return json4(entry.data);
        return json4({ error: e.message }, 500);
      }
    }
    m = path.match(/^\/episodes\/((?:[\w-]+\/)+)(\d+)\/?$/i);
    if (m) {
      const rawNames = m[1].replace(/\/$/, "").split("/");
      const anilistId = m[2];
      const includeMap = url.searchParams.get("map") !== "false";
      const { resolved: resolved2, unknown } = resolveProviders(rawNames);
      if (resolved2.size === 0) {
        return json4({ error: "No valid providers specified", unknown }, 400);
      }
      try {
        const data = await getFilteredEpisodesResponse(anilistId, resolved2, includeMap);
        if (unknown.length) data._unknownProviders = unknown;
        return json4(data);
      } catch (e) {
        return json4({ error: e.message }, 500);
      }
    }
    m = path.match(/^\/episodes\/(\d+)\/?$/);
    if (m) {
      const anilistId = m[1];
      try {
        return json4(await getEpisodesResponse(anilistId, env));
      } catch (e) {
        return json4({ error: e.message }, 500);
      }
    }
    m = path.match(/^\/watch\/mkissa\/(\d+)\/(sub|dub)\/mkissa-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:mkissa:${id}:${audio}:${ep}`,
        () => mkissa_default.fetch(request2)
      );
    }
    if (path.match(/^\/captcha\/mkissa\/?$/)) {
      return mkissa_default.fetch(request2);
    }
    m = path.match(/^\/watch\/reanime\/(\d+)\/(sub|dub)\/reanime-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:reanime:${id}:${audio}:${ep}`,
        () => reanime_default2.fetch(rewriteRequest(request2, `/watch/${id}/${audio}/${ep}`))
      );
    }
    m = path.match(/^\/stream\/reanime\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return reanime_default2.fetch(rewriteRequest(request2, `/stream/${id}/${audio}/${ep}`));
    }
    m = path.match(/^\/watch\/anikoto\/(\d+)\/(sub|dub)\/anikoto-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anikoto:${id}:${audio}:${ep}`,
        () => anikoto_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/animegg\/(\d+)\/(sub|dub)\/animegg-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animegg:${id}:${audio}:${ep}`,
        () => animegg_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/anineko\/(\d+)\/(sub|dub)\/anineko-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anineko:${id}:${audio}:${ep}`,
        () => anineko_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/anidbapp\/(\d+)\/(sub|dub)\/anidbapp-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anidbapp:${id}:${audio}:${ep}`,
        () => anidbapp_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/2dhive\/(\d+)\/(sub|dub)\/2dhive-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:2dhive:${id}:${audio}:${ep}`,
        () => dhive_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/animenosub\/(\d+)\/(sub|dub)\/animenosub-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animenosub:${id}:${audio}:${ep}`,
        () => animenosub_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/anizone\/(\d+)\/(sub|dub)\/anizone-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anizone:${id}:${audio}:${ep}`,
        () => anizone_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/aniwaves\/(\d+)\/(sub|dub)\/aniwaves-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:aniwaves:${id}:${audio}:${ep}`,
        () => aniwaves_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/anibd\/(\d+)\/(sub|dub)\/anibd-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anibd:${id}:${audio}:${ep}`,
        () => anibd_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/senshi\/(\d+)\/(sub|dub)\/senshi-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:senshi:${id}:${audio}:${ep}`,
        () => senshi_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/kaa\/(\d+)\/(sub|dub)\/kaa-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:kaa:${id}:${audio}:${ep}`,
        () => kickassanime_default.fetch(request2)
      );
    }
    m = path.match(/^\/watch\/animedunya\/(\d+)\/(sub|dub)\/animedunya-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animedunya:${id}:${audio}:${ep}`,
        () => animedunya_default.fetch(request2)
      );
    }
    m = path.match(/^\/stream\/2dhive\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) return dhive_default.fetch(request2);
    m = path.match(/^\/stream\/2dhive\/download\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) return dhive_default.fetch(request2);
    return json4({
      name: "Anivexa API 2.2.1",
      cache: _CACHE_ENABLED,
      providers: [
        "mkissa",
        "reanime",
        "anikoto",
        "animegg",
        "anineko",
        "anidbapp",
        "2dhive",
        "animenosub",
        "anizone",
        "aniwaves",
        "anibd",
        "senshi",
        "kaa",
        "animedunya"
      ],
      routes: [
        "/map/:anilistId",
        "/episodes/:anilistId",
        "/episodes/:provider[/:provider...]/:anilistId?map=true|false",
        "/watch/mkissa/:id/sub|dub/mkissa-:ep",
        "/watch/reanime/:id/sub|dub/reanime-:ep",
        "/stream/reanime/:id/sub|dub/:ep",
        "/watch/anikoto/:id/sub|dub/anikoto-:ep",
        "/watch/animegg/:id/sub|dub/animegg-:ep",
        "/watch/anineko/:id/sub|dub/anineko-:ep",
        "/watch/anidbapp/:id/sub|dub/anidbapp-:ep",
        "/watch/2dhive/:id/sub|dub/2dhive-:ep",
        "/stream/2dhive/:id/sub|dub/:ep",
        "/stream/2dhive/download/:id/sub|dub/:ep",
        "/watch/animenosub/:id/sub|dub/animenosub-:ep",
        "/watch/anizone/:id/sub|dub/anizone-:ep",
        "/watch/aniwaves/:id/sub|dub/aniwaves-:ep",
        "/watch/anibd/:id/sub|dub/anibd-:ep",
        "/watch/senshi/:id/sub|dub/senshi-:ep",
        "/watch/kaa/:id/sub|dub/kaa-:ep",
        "/watch/animedunya/:id/sub|dub/animedunya-:ep"
      ]
    });
  }
};
export {
  index_default as default
};
//# sourceMappingURL=index.js.map
