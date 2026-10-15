import { fetchHtml, decodeEntities } from "../core/new-provider-utils.js";

const DEFAULT_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export function canExtractAnimeSalt(url = "") {
  const low = url.toLowerCase();
  return (
    low.includes("animesalt.cx") ||
    low.includes("animesalt.ac") ||
    low.includes("animesalt.ro") ||
    low.includes("as-cdn") ||
    low.includes("acdn.top") ||
    low.includes("animesalt")
  );
}

/**
 * Extracts HLS stream from an AnimeSalt episode or player embed URL
 */
export async function extractAnimeSalt(url, referer = "https://animesalt.cx/") {
  const details = await extractAnimeSaltDetails(url, referer);
  return details?.streamUrl ?? null;
}

export async function extractAnimeSaltDetails(url, referer = "https://animesalt.cx/") {
  try {
    let targetPageUrl = url;
    let videoOrigin = "";
    let videoHash = "";
    let iframeUrl = "";

    // 1. If input is already an as-cdn player iframe URL (e.g. https://as-cdn26.top/video/hash)
    const directPlayerMatch = url.match(/https?:\/\/(?:as-cdn\d*|acdn)\.top\/video\/([a-zA-Z0-9_-]+)/i);
    if (directPlayerMatch) {
      iframeUrl = url;
      videoHash = directPlayerMatch[1];
      videoOrigin = new URL(url).origin;
    } else {
      // Input is episode webpage URL: fetch HTML and locate video iframe
      const rawHtml = await fetchHtml(url, {
        Referer: referer,
        "User-Agent": DEFAULT_UA,
      });

      const m =
        rawHtml.match(/src=["'](https?:\/\/(?:as-cdn\d*|acdn)\.top\/video\/([a-zA-Z0-9_-]+))["']/i) ||
        rawHtml.match(/data-src=["'](https?:\/\/(?:as-cdn\d*|acdn)\.top\/video\/([a-zA-Z0-9_-]+))["']/i) ||
        rawHtml.match(/(https?:\/\/(?:as-cdn\d*|acdn)\.top\/video\/([a-zA-Z0-9_-]+))/i);

      if (m) {
        iframeUrl = m[1];
        videoHash = m[2];
        videoOrigin = new URL(iframeUrl).origin;
      }
    }

    // 2. If we found the as-cdn player hash, make the getVideo API request to obtain the signed master.m3u8
    if (videoHash && videoOrigin) {
      const postUrl = `${videoOrigin}/player/index.php?data=${videoHash}&do=getVideo`;
      const postBody = new URLSearchParams({
        hash: videoHash,
        r: targetPageUrl,
      }).toString();

      const resp = await fetch(postUrl, {
        method: "POST",
        headers: {
          "User-Agent": DEFAULT_UA,
          "Referer": iframeUrl || `${videoOrigin}/video/${videoHash}`,
          "X-Requested-With": "XMLHttpRequest",
          "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        },
        body: postBody,
      });

      if (resp.ok) {
        const data = await resp.json().catch(() => null);
        const videoSource = data?.videoSource || data?.securedLink;
        if (videoSource) {
          return {
            streamUrl: videoSource,
            isHls: true,
            headers: {
              "Referer": `${videoOrigin}/`,
              "User-Agent": DEFAULT_UA,
              "Origin": videoOrigin,
            },
            subtitles: [],
            videoImage: data.videoImage || null,
          };
        }
      }
    }

    return null;
  } catch (err) {
    console.error("[extractAnimeSalt] Failed:", err.message);
    return null;
  }
}
