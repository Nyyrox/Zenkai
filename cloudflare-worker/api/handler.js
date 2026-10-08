import worker from "../index.js";

export const config = { runtime: "edge" };

export default (request) => worker.fetch(request, {});
