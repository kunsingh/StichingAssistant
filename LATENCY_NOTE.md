# Swatch Preview Latency

The interaction target is immediate acknowledgement and a visually stable
preview within roughly 100 ms; a several-second generation request cannot sit
on the tap path.

## Recommended path

For common combinations, pre-generate a bounded catalogue by stitch, weight,
and fibre. Generate each texture in a neutral tone and recolour it using a
mask-aware transformation. The client downloads the neutral texture and colour
mask once, then applies preset colours locally. A colour tap can therefore
update in one frame without a network request while preserving shadows and
highlights. The hex value, not the colour name, drives the transformation.

Use three cache layers: bundled popular previews, CDN content-addressed assets,
and a device LRU cache. Warm likely next colours when the project-planner screen
opens. A cache key includes all structured inputs plus provider, model, prompt,
and quality-gate versions.

For an uncached structure, show an immediate colour-correct procedural knit
placeholder with a subtle "refining preview" state. Submit generation in the
background, deduplicate concurrent identical requests, and replace the
placeholder only after moderation, colour-distance, and stitch-structure gates
pass. Polling should use a job ID or a push channel; navigation must not cancel
the durable job.

## Service controls

Use a faster model tier for interactive misses and a higher-quality model for
overnight catalogue generation. Apply a strict timeout and circuit breaker.
When providers degrade, continue serving cached and recoloured assets instead
of retrying aggressively. Limit generations per user/project, enforce a global
daily budget, and alert on latency, cache hit rate, rejection rate, and cost.

Measure tap-to-placeholder, tap-to-final, p50/p95 generation latency, memory
pressure, CDN hit rate, and perceptual colour error on representative devices.
The key product experiment compares per-colour generation with neutral-structure
recolouring; I would accept minor realism loss if expert knitters judge the
stitch accurately and the colour interaction feels instant.
