const __name = (fn, _) => fn;

var resolved = new Map();
var inflight = new Map();
var UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
var ARM = "https://arm.haglund.dev/api/v2/ids";
var JIKAN = "https://api.jikan.moe/v4";
var STATUS_MAP = {
  "Currently Airing": "RELEASING",
  "Finished Airing": "FINISHED",
  "Not yet aired": "NOT_YET_RELEASED",
  "On Hiatus": "HIATUS"
};

const AL_STATUS_MAP = {
  RELEASING: "RELEASING",
  FINISHED: "FINISHED",
  NOT_YET_RELEASED: "NOT_YET_RELEASED",
  CANCELLED: "FINISHED",
  HIATUS: "HIATUS",
};

async function fetchFromAniList(id) {
  const fullQuery = `query($id:Int){Media(id:$id,type:ANIME){id title{english romaji native} status format episodes seasonYear startDate{year} synonyms nextAiringEpisode{episode airingAt timeUntilAiring}}}`;
  const res = await fetch("https://graphql.anilist.co", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json", "User-Agent": UA },
    body: JSON.stringify({ query: fullQuery, variables: { id } }),
  }).catch(() => null);
  if (!res || !res.ok) return null;
  const json = await res.json();
  return json.data?.Media ?? null;
}

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
      const al = await fetchFromAniList(id);
      if (!al) throw new Error(`No data found for AniList ID ${id}`);
      const media = {
        id,
        idMal: null,
        title: {
          english: al.title?.english ?? null,
          romaji: al.title?.romaji ?? null,
          native: al.title?.native ?? null,
        },
        status: AL_STATUS_MAP[al.status] ?? "RELEASING",
        format: al.format ?? null,
        episodes: al.episodes ?? null,
        seasonYear: al.seasonYear ?? null,
        startDate: al.startDate ?? null,
        nextAiringEpisode: al.nextAiringEpisode ?? null,
        synonyms: Array.isArray(al.synonyms) ? al.synonyms : [],
      };
      resolved.set(id, media);
      inflight.delete(id);
      return media;
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
      // On 5xx / network errors, fall back to AniList-only data if available rather than hard-failing.
      if (!r.ok) {
        if (al) break; // exit loop, jikan stays null, fall through to AniList fallback below
        throw new Error(`Jikan ${r.status}`);
      }
      jikan = await r.json();
      break;
    }
    const d = jikan?.data ?? null;
    // If Jikan was unavailable but we have AniList data, build a partial media object from AniList only.
    if (!d && al) {
      const media = {
        id,
        idMal: malId,
        title: {
          english: al.title?.english ?? null,
          romaji: al.title?.romaji ?? null,
          native: al.title?.native ?? null,
        },
        status: AL_STATUS_MAP[al.status] ?? "RELEASING",
        format: al.format ?? null,
        episodes: al.episodes ?? null,
        seasonYear: al.seasonYear ?? null,
        startDate: al.startDate ?? null,
        nextAiringEpisode: al.nextAiringEpisode ?? null,
        synonyms: Array.isArray(al.synonyms) ? al.synonyms : [],
      };
      resolved.set(id, media);
      inflight.delete(id);
      return media;
    }
    if (!d) throw new Error(`Jikan returned no data for MAL ID ${malId}`);
    const media = {
      id,
      idMal: malId,
      title: {
        english: al?.title?.english ?? d.title_english ?? null,
        romaji: al?.title?.romaji ?? d.title ?? null,
        native: al?.title?.native ?? d.title_japanese ?? null,
      },
      status: AL_STATUS_MAP[al?.status] ?? STATUS_MAP[d.status] ?? "RELEASING",
      format: al?.format ?? d.type ?? null,
      episodes: al?.episodes ?? d.episodes ?? null,
      seasonYear: al?.seasonYear ?? d.year ?? null,
      startDate: al?.startDate ?? (d.aired?.from ? { year: new Date(d.aired.from).getFullYear() } : null),
      nextAiringEpisode: al?.nextAiringEpisode ?? null,
      synonyms: [
        ...(d.titles?.map((t) => t.title).filter(Boolean) ?? []),
        ...(Array.isArray(al?.synonyms) ? al.synonyms : []),
      ],
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

function forgetMedia(anilistId) {
  resolved.delete(Number(anilistId));
}

export { getMedia, forgetMedia };
