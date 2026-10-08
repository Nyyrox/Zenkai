/**
 * Cloudflare Worker HLS Stream & CORS Reverse Proxy
 * Rewrites .m3u8 playlists and proxies video/segment chunks with appropriate headers.
 */
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle Preflight OPTIONS
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Access-Control-Allow-Headers": "*",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    if (url.pathname === "/healthz" || url.pathname === "/") {
      return new Response(JSON.stringify({ status: "ok", service: "Zenkai HLS Proxy Worker" }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const target = url.searchParams.get("url");
    const referer = url.searchParams.get("referer") || url.searchParams.get("ref") || "https://flixcloud.cc/";
    const origin = url.searchParams.get("origin") || referer.replace(/\/$/, "");

    if (!target) {
      return new Response(JSON.stringify({ error: "Missing ?url= query parameter" }), {
        status: 400,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    let targetUrl;
    try {
      targetUrl = new URL(target);
    } catch {
      return new Response(JSON.stringify({ error: "Invalid target URL format" }), {
        status: 400,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const forwardHeaders = new Headers();
    forwardHeaders.set(
      "User-Agent",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    );
    forwardHeaders.set("Referer", referer);
    forwardHeaders.set("Origin", origin);
    forwardHeaders.set("Accept", "*/*");
    forwardHeaders.set("Accept-Language", "en-US,en;q=0.9");
    forwardHeaders.set("Sec-Fetch-Dest", "empty");
    forwardHeaders.set("Sec-Fetch-Mode", "cors");
    forwardHeaders.set("Sec-Fetch-Site", "cross-site");

    // Forward Range header if present (for seeking in video players)
    const range = request.headers.get("Range");
    if (range) {
      forwardHeaders.set("Range", range);
    }

    try {
      const upstream = await fetch(target, {
        method: request.method,
        headers: forwardHeaders,
      });

      const contentType = upstream.headers.get("Content-Type") || "";
      const isM3U8 =
        contentType.includes("mpegurl") ||
        contentType.includes("x-mpegurl") ||
        targetUrl.pathname.endsWith(".m3u8") ||
        targetUrl.pathname.endsWith(".m3u");

      const responseHeaders = new Headers(upstream.headers);
      responseHeaders.set("Access-Control-Allow-Origin", "*");
      responseHeaders.set("Access-Control-Allow-Headers", "*");
      responseHeaders.set("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges");

      if (isM3U8) {
        const text = await upstream.text();
        const rewritten = rewriteM3U8(text, target, url.origin, referer);
        responseHeaders.set("Content-Type", "application/vnd.apple.mpegurl");
        return new Response(rewritten, {
          status: upstream.status,
          headers: responseHeaders,
        });
      }

      return new Response(upstream.body, {
        status: upstream.status,
        headers: responseHeaders,
      });
    } catch (err) {
      return new Response(JSON.stringify({ error: "Proxy fetch failed: " + err.message }), {
        status: 502,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }
  },
};

function rewriteM3U8(manifest, baseUrl, proxyOrigin, referer) {
  const lines = manifest.split(/\r?\n/);
  const out = [];

  for (let line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      out.push(line);
      continue;
    }

    if (trimmed.startsWith("#")) {
      // Rewrite URI="..." attributes in tags like #EXT-X-KEY or #EXT-X-MEDIA
      const rewrittenTag = trimmed.replace(/URI=["']([^"']+)["']/g, (match, uri) => {
        const absUrl = new URL(uri, baseUrl).toString();
        const proxied = `${proxyOrigin}/?url=${encodeURIComponent(absUrl)}&referer=${encodeURIComponent(referer)}`;
        return `URI="${proxied}"`;
      });
      out.push(rewrittenTag);
    } else {
      // Video chunk or sub-playlist URL
      const absUrl = new URL(trimmed, baseUrl).toString();
      const proxied = `${proxyOrigin}/?url=${encodeURIComponent(absUrl)}&referer=${encodeURIComponent(referer)}`;
      out.push(proxied);
    }
  }

  return out.join("\n");
}
