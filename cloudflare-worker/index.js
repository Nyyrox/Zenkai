import { getMedia }                from "./core/anilist.js";
import { mapAnimeIds }             from "./core/mapper.js";
import mkissaHandler               from "./providers/mkissa.js";
import reanimeHandler              from "./providers/reanime.js";
import anikotoHandler              from "./providers/anikoto.js";
import animeggHandler              from "./providers/animegg.js";
import aninekoHandler              from "./providers/anineko.js";
import anidbappHandler             from "./providers/anidbapp.js";
import dhiveHandler                from "./providers/2dhive.js";
import animenosubHandler           from "./providers/animenosub.js";
import anizoneHandler              from "./providers/anizone.js";
import aniwavesHandler             from "./providers/aniwaves.js";
import anibdHandler                from "./providers/anibd.js";
import senshiHandler               from "./providers/senshi.js";
import kaaHandler                  from "./providers/kickassanime.js";
import animedunyaHandler           from "./providers/animedunya.js";
import animesaltHandler            from "./providers/animesalt.js";
import avatarsData                 from "./avatars.json";
import bannersData                 from "./banners.json";
import { getEpisodesResponse, getFilteredEpisodesResponse } from "./core/episode-cache.js";
import { resolveProviders }         from "./core/episode-strategy.js";
import { getAsync, setAsync, isFresh, mapTTL, WATCH_TTL, _CACHE_ENABLED } from "./core/smartcache.js";

function json(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=300",
    },
  });
}

function rewriteRequest(request, newPath) {
  const u = new URL(request.url);
  u.pathname = newPath;
  return new Request(u.toString(), { method: request.method, headers: request.headers });
}

const watchInflight = new Map();

async function cachedWatch(cacheKey, handlerFn) {
  const entry = await getAsync(cacheKey);
  if (entry && isFresh(entry)) return json(entry.data);

  if (watchInflight.has(cacheKey)) {
    await watchInflight.get(cacheKey).catch(() => {});
    const warm = await getAsync(cacheKey);
    if (warm && isFresh(warm)) return json(warm.data);
    return handlerFn();
  }

  const promise = (async () => {
    const response = await handlerFn();
    if (response.status === 200) {
      try {
        const data = await response.clone().json();
        await setAsync(cacheKey, data, WATCH_TTL);
      } catch {}
    }
    return response;
  })();

  watchInflight.set(cacheKey, promise);
  try   { return await promise; }
  finally { watchInflight.delete(cacheKey); }
}

export default {
  async fetch(request, env) {
    const url  = new URL(request.url);
    const path = url.pathname;

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin":  "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Access-Control-Allow-Headers": "*",
        },
      });
    }

    if (path === "/proxy") {
      const target = url.searchParams.get("url");
      const requestedReferer = url.searchParams.get("referer") || url.searchParams.get("ref");

      if (!target) {
        return json({ error: "Missing ?url= param" }, 400);
      }

      let targetUrl;
      try { targetUrl = new URL(target); } catch {
        return json({ error: "Invalid target URL" }, 400);
      }

      const host = targetUrl.hostname.toLowerCase();
      let referer = requestedReferer;

      if (!referer || referer === "https://flixcloud.cc/" || referer === "https://flixcloud.cc") {
        if (host.includes("krussdomi") || host.includes("advancedairesearchlab")) {
          referer = "https://krussdomi.com/";
        } else if (host.includes("animeapps") || host.includes("nukitashi") || host.includes("playeng")) {
          referer = "https://playeng.animeapps.top/";
        } else if (host.includes("megap") || host.includes("shiora") || host.includes("akirax") || host.includes("imgnex")) {
          referer = "https://megaplay.top/";
        } else if (host.includes("animegg")) {
          referer = "https://www.animegg.org/";
        } else if (host.includes("vibevibe") || host.includes("vivibebe")) {
          referer = "https://anineko.to/";
        } else if (host.includes("anizone")) {
          referer = "https://anizone.to/";
        } else if (host.includes("aniwave")) {
          referer = "https://aniwave.to/";
        } else if (host.includes("animesalt") || host.includes("as-cdn") || host.includes("acdn.top")) {
          referer = "https://animesalt.cx/";
        } else if (host.includes("flixcloud") || host.includes("atomic4cdn") || host.includes("rundowncdn")) {
          referer = "https://flixcloud.cc/";
        } else {
          referer = requestedReferer || `${targetUrl.origin}/`;
        }
      }

      const origin = url.searchParams.get("origin") || referer.replace(/\/$/, "");

      const forwardHeaders = new Headers();
      forwardHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
      forwardHeaders.set("Referer", referer);
      forwardHeaders.set("Origin", origin);
      forwardHeaders.set("Accept", "*/*");
      forwardHeaders.set("Accept-Language", "en-US,en;q=0.9");
      forwardHeaders.set("Sec-Fetch-Dest", "empty");
      forwardHeaders.set("Sec-Fetch-Mode", "cors");
      forwardHeaders.set("Sec-Fetch-Site", "cross-site");

      const range = request.headers.get("Range");
      if (range) forwardHeaders.set("Range", range);

      try {
        const upstream = await fetch(target, { method: request.method, headers: forwardHeaders });
        const contentType = upstream.headers.get("Content-Type") || "";
        
        const responseHeaders = new Headers(upstream.headers);
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("Access-Control-Allow-Headers", "*");
        responseHeaders.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        responseHeaders.set("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges");

        if (!upstream.ok) {
          return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
        }

        const isLikelyM3U8 = contentType.includes("mpegurl") || contentType.includes("x-mpegurl") || targetUrl.pathname.endsWith(".m3u8") || targetUrl.pathname.endsWith(".m3u");

        if (isLikelyM3U8) {
          const text = await upstream.text();
          if (text.trim().startsWith("<") || text.includes("<html") || text.includes("<head")) {
            return new Response(text, { status: upstream.status, headers: responseHeaders });
          }

          const lines = text.split(/\r?\n/);
          const out = [];
          for (let line of lines) {
            const trimmed = line.trim();
            if (!trimmed) { out.push(line); continue; }
            if (trimmed.startsWith("#")) {
              const rewrittenTag = trimmed.replace(/URI=["']([^"']+)["']/g, (m, uri) => {
                let absUrl;
                try {
                  absUrl = new URL(uri, target);
                  if (!absUrl.search && targetUrl.search) absUrl.search = targetUrl.search;
                } catch {
                  return m;
                }
                return `URI="${url.origin}/proxy?url=${encodeURIComponent(absUrl.toString())}&referer=${encodeURIComponent(referer)}"`;
              });
              out.push(rewrittenTag);
            } else {
              let absUrl;
              try {
                absUrl = new URL(trimmed, target);
                if (!absUrl.search && targetUrl.search) absUrl.search = targetUrl.search;
              } catch {
                out.push(trimmed);
                continue;
              }
              out.push(`${url.origin}/proxy?url=${encodeURIComponent(absUrl.toString())}&referer=${encodeURIComponent(referer)}`);
            }
          }
          responseHeaders.set("Content-Type", "application/vnd.apple.mpegurl");
          return new Response(out.join("\n"), { status: upstream.status, headers: responseHeaders });
        }

        return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
      } catch (err) {
        return json({ error: "Proxy fetch failed: " + err.message }, 502);
      }
    }

    let m = path.match(/^\/map\/(\d+)\/?$/);
    if (m) {
      const anilistId = m[1];
      const cacheKey  = `map:${anilistId}`;
      const entry     = await getAsync(cacheKey);
      if (entry && isFresh(entry)) return json(entry.data);

      try {
        const [data, media] = await Promise.all([
          mapAnimeIds(anilistId),
          getMedia(anilistId).catch(() => null),
        ]);
        await setAsync(cacheKey, data, mapTTL(media?.status ?? "RELEASING"));
        return json(data);
      } catch (e) {
        if (entry) return json(entry.data);
        return json({ error: e.message }, 500);
      }
    }

    m = path.match(/^\/episodes\/((?:[\w-]+\/)+)(\d+)\/?$/i);
    if (m) {
      const rawNames  = m[1].replace(/\/$/, "").split("/");
      const anilistId = m[2];
      const includeMap = url.searchParams.get("map") !== "false";
      const { resolved, unknown } = resolveProviders(rawNames);

      if (resolved.size === 0) {
        return json({ error: "No valid providers specified", unknown }, 400);
      }

      try {
        const data = await getFilteredEpisodesResponse(anilistId, resolved, includeMap);
        if (unknown.length) data._unknownProviders = unknown;
        return json(data);
      } catch (e) {
        return json({ error: e.message }, 500);
      }
    }

    m = path.match(/^\/episodes\/(\d+)\/?$/);
    if (m) {
      const anilistId = m[1];
      try {
        return json(await getEpisodesResponse(anilistId, env));
      } catch (e) {
        return json({ error: e.message }, 500);
      }
    }

    m = path.match(/^\/watch\/mkissa\/(\d+)\/(sub|dub)\/mkissa-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:mkissa:${id}:${audio}:${ep}`,
        () => mkissaHandler.fetch(request)
      );
    }

    if (path.match(/^\/captcha\/mkissa\/?$/)) {
      return mkissaHandler.fetch(request);
    }

    m = path.match(/^\/watch\/reanime\/(\d+)\/(sub|dub)\/reanime-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:reanime:${id}:${audio}:${ep}`,
        () => reanimeHandler.fetch(rewriteRequest(request, `/watch/${id}/${audio}/${ep}`))
      );
    }

    m = path.match(/^\/stream\/reanime\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return reanimeHandler.fetch(rewriteRequest(request, `/stream/${id}/${audio}/${ep}`));
    }

    m = path.match(/^\/watch\/anikoto\/(\d+)\/(sub|dub)\/anikoto-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anikoto:${id}:${audio}:${ep}`,
        () => anikotoHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/animegg\/(\d+)\/(sub|dub)\/animegg-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animegg:${id}:${audio}:${ep}`,
        () => animeggHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/anineko\/(\d+)\/(sub|dub)\/anineko-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anineko:${id}:${audio}:${ep}`,
        () => aninekoHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/anidbapp\/(\d+)\/(sub|dub)\/anidbapp-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anidbapp:${id}:${audio}:${ep}`,
        () => anidbappHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/2dhive\/(\d+)\/(sub|dub)\/2dhive-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:2dhive:${id}:${audio}:${ep}`,
        () => dhiveHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/animenosub\/(\d+)\/(sub|dub)\/animenosub-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animenosub:${id}:${audio}:${ep}`,
        () => animenosubHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/anizone\/(\d+)\/(sub|dub)\/anizone-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anizone:${id}:${audio}:${ep}`,
        () => anizoneHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/aniwaves\/(\d+)\/(sub|dub)\/aniwaves-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:aniwaves:${id}:${audio}:${ep}`,
        () => aniwavesHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/anibd\/(\d+)\/(sub|dub)\/anibd-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:anibd:${id}:${audio}:${ep}`,
        () => anibdHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/senshi\/(\d+)\/(sub|dub)\/senshi-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:senshi:${id}:${audio}:${ep}`,
        () => senshiHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/kaa\/(\d+)\/(sub|dub)\/kaa-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:kaa:${id}:${audio}:${ep}`,
        () => kaaHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/animedunya\/(\d+)\/(sub|dub)\/animedunya-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animedunya:${id}:${audio}:${ep}`,
        () => animedunyaHandler.fetch(request)
      );
    }

    m = path.match(/^\/watch\/animesalt\/(\d+)\/(sub|dub|multi)\/animesalt-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      return cachedWatch(
        `watch:animesalt:${id}:${audio}:${ep}`,
        () => animesaltHandler.fetch(request)
      );
    }

    m = path.match(/^\/stream\/animesalt\/([^/?#]+)\/?$/);
    if (m) return animesaltHandler.fetch(request);

    if (path === "/extract/animesalt") {
      return animesaltHandler.fetch(request);
    }

    if (path === "/avatars.json" || path === "/avatars") {
      return json(avatarsData);
    }

    if (path === "/banners.json" || path === "/banners") {
      return json(bannersData);
    }

    m = path.match(/^\/stream\/2dhive\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) return dhiveHandler.fetch(request);

    m = path.match(/^\/stream\/2dhive\/download\/(\d+)\/(sub|dub)\/(\d+)\/?$/);
    if (m) return dhiveHandler.fetch(request);

    return json({
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
        "animedunya",
        "animesalt",
      ],
      routes: [
        "/avatars.json",
        "/banners.json",
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
        "/watch/animedunya/:id/sub|dub/animedunya-:ep",
        "/watch/animesalt/:id/sub|dub|multi/animesalt-:ep",
        "/stream/animesalt/:slug",
        "/extract/animesalt?url=:url",
      ],
    });
  },
};
