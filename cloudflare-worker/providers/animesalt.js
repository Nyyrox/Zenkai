import { getMedia } from "../core/anilist.js";
import {
  attr,
  decodeEntities,
  episodeMeta,
  expectedCount,
  fetchHtml,
  findTopSlugs,
  getPrequelOffset,
  json,
  selectSeries,
  stripTags,
} from "../core/new-provider-utils.js";
import { get, set, isFresh, SHOW_IDENTITY_TTL } from "../core/smartcache.js";
import { extractAnimeSaltDetails, canExtractAnimeSalt } from "../extractors/animesalt.js";

const BASE = "https://animesalt.cx";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export function normalizeAnimeSaltTitle(title = "") {
  return title
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

async function search(query) {
  const normTitle = normalizeAnimeSaltTitle(query);
  const results = [];
  try {
    const html = await fetchHtml(`${BASE}/?s=${encodeURIComponent(query)}`, {
      "User-Agent": UA,
      Referer: `${BASE}/`,
    });

    for (const m of html.matchAll(/<article\b[^>]*class=["'][^"']*anime[^"']*["'][^>]*>[\s\S]*?<\/article>/gi)) {
      const block = m[0];
      const linkTag = block.match(/<a\b[^>]*href=["']([^"']+)["'][^>]*>/i);
      if (!linkTag) continue;
      const href = linkTag[1];
      const slug = href.match(/\/(?:series|movies|anime)\/([^/?#]+)/)?.[1];
      if (!slug) continue;
      const titleMatch = block.match(/<(?:h2|h3|span)\b[^>]*class=["'][^"']*title[^"']*["'][^>]*>([\s\S]*?)<\/(?:h2|h3|span)>/i);
      const title = titleMatch ? stripTags(titleMatch[1]) : slug.replace(/-/g, " ");
      results.push({ slug, text: title });
    }
  } catch {}

  if (!results.length && normTitle) {
    results.push({ slug: normTitle, text: query });
  }

  return results;
}

async function scrapeSeries(slug) {
  const episodes = [];
  try {
    const html = await fetchHtml(`${BASE}/series/${slug}/`, {
      "User-Agent": UA,
      Referer: `${BASE}/`,
    });

    const epMatches = [...html.matchAll(/<a\b[^>]*href=["']([^"']+\/(?:episode|movies)\/([^"'/?#]+)\/?)["'][^>]*>([\s\S]*?)<\/a>/gi)];
    for (const m of epMatches) {
      const epSlug = m[2];
      const epNumMatch = epSlug.match(/-(\d+)x(\d+)$/i) || epSlug.match(/-(\d+)$/);
      const num = epNumMatch ? Number(epNumMatch[2] || epNumMatch[1]) : (episodes.length + 1);
      const titleText = stripTags(m[3]) || `Episode ${num}`;
      episodes.push({
        number: num,
        title: titleText,
        epSlug: epSlug,
        hasSub: true,
        hasDub: true,
        hasMulti: true,
      });
    }
  } catch {}

  if (!episodes.length) {
    // If not found in series page, generate default 1..100 episode slots
    for (let i = 1; i <= 24; i++) {
      episodes.push({
        number: i,
        title: `Episode ${i}`,
        epSlug: `${slug}-1x${i}`,
        hasSub: true,
        hasDub: true,
        hasMulti: true,
      });
    }
  }

  episodes.sort((a, b) => a.number - b.number);
  const seen = new Set();
  return episodes.filter((e) => (seen.has(e.number) ? false : (seen.add(e.number), true)));
}

export async function scrapeEpisodeWatch(slug, audio = "multi", baseUrl = "https://zenkai-stream-proxy.ammhfoo.workers.dev") {
  const candidateUrls = [
    `${BASE}/episode/${slug}/`,
    `${BASE}/movies/${slug}/`,
    `https://animesalt.ac/episode/${slug}/`,
    `https://animesalt.ro/episode/${slug}/`,
  ];

  for (const url of candidateUrls) {
    try {
      const details = await extractAnimeSaltDetails(url, `${BASE}/`);
      if (details?.streamUrl) {
        const rawUrl = details.streamUrl;
        const referer = details.headers?.Referer || "https://animesalt.cx/";
        const proxiedStreamUrl = `${baseUrl}/proxy?url=${encodeURIComponent(rawUrl)}&referer=${encodeURIComponent(referer)}`;

        return [
          {
            url: proxiedStreamUrl,
            rawUrl: rawUrl,
            type: "hls",
            audio: audio,
            server: "acdn (Multi-Audio)",
            priority: 10,
            headers: details.headers,
            subtitles: details.subtitles,
            videoImage: details.videoImage,
            isActive: true,
          },
        ];
      }
    } catch {}
  }
  return [];
}

export async function getEpisodes(anilistId, ctx = {}) {
  const media = ctx.media ?? (await getMedia(anilistId));
  const titles = [media?.title?.english, media?.title?.romaji, media?.title?.userPreferred].filter(Boolean);
  const isMovie = (media?.format ?? "").toUpperCase() === "MOVIE";
  const normTitle = normalizeAnimeSaltTitle(titles[0] || "");
  const slug = isMovie ? normTitle : normTitle;

  const episodes = await scrapeSeries(slug);
  const count = episodes.length || (media?.episodes ?? 12);

  const sub = [];
  const dub = [];
  const multi = [];

  for (let i = 1; i <= count; i++) {
    const epSlug = isMovie ? normTitle : `${normTitle}-1x${i}`;
    const epData = {
      id: `watch/animesalt/${anilistId}/multi/animesalt-${i}`,
      number: i,
      title: `Episode ${i}`,
      audio: "multi",
      filler: false,
      recap: false,
      uncensored: false,
    };
    multi.push(epData);
    sub.push({ ...epData, id: `watch/animesalt/${anilistId}/sub/animesalt-${i}`, audio: "sub" });
    dub.push({ ...epData, id: `watch/animesalt/${anilistId}/dub/animesalt-${i}`, audio: "dub" });
  }

  return {
    episodes: {
      sub,
      dub,
      multi,
    },
  };
}

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const workerOrigin = url.origin;

    // Pattern: /watch/animesalt/:id/(sub|dub|multi)/animesalt-:ep
    const m = path.match(/^\/watch\/animesalt\/(\d+)\/(sub|dub|multi)\/animesalt-(\d+)\/?$/);
    if (m) {
      const [, id, audio, ep] = m;
      const media = await getMedia(id).catch(() => null);
      const title = media?.title?.english || media?.title?.romaji || media?.title?.userPreferred || "";
      const isMovie = (media?.format ?? "").toUpperCase() === "MOVIE";
      const normTitle = normalizeAnimeSaltTitle(title);
      const slug = isMovie ? normTitle : `${normTitle}-1x${ep}`;

      const streams = await scrapeEpisodeWatch(slug, audio, workerOrigin);
      if (streams.length) {
        return json({
          status: 200,
          streams,
        });
      }

      return json(
        {
          status: 200,
          streams: [
            {
              url: `https://animesalt.cx/episode/${slug}/`,
              type: "embed",
              audio,
              server: "acdn (Multi-Audio)",
              priority: 1,
              isActive: true,
            },
          ],
        },
        200
      );
    }

    // Pattern: /stream/animesalt/:slug
    const m2 = path.match(/^\/stream\/animesalt\/([^/?#]+)\/?$/);
    if (m2) {
      const slug = m2[1];
      const streams = await scrapeEpisodeWatch(slug, "multi", workerOrigin);
      return json({ status: 200, streams });
    }

    // Direct extraction query: /extract/animesalt?url=...
    if (path === "/extract/animesalt") {
      const targetUrl = url.searchParams.get("url");
      if (!targetUrl) return json({ error: "Missing url parameter" }, 400);
      const details = await extractAnimeSaltDetails(targetUrl);
      if (!details) return json({ error: "Failed to extract AnimeSalt HLS" }, 404);
      const referer = details.headers?.Referer || "https://animesalt.cx/";
      const proxiedStreamUrl = `${workerOrigin}/proxy?url=${encodeURIComponent(details.streamUrl)}&referer=${encodeURIComponent(referer)}`;
      return json({
        streamUrl: proxiedStreamUrl,
        rawStreamUrl: details.streamUrl,
        isHls: true,
        headers: details.headers,
        subtitles: details.subtitles,
        videoImage: details.videoImage,
      });
    }

    return json({ error: "Not found" }, 404);
  },
};
