# Technical Decisions

## Assignment A

### Deterministic code owns every number

The model may classify/phrase but never calculate. Calculator outputs are typed,
structured records. If live LLM phrasing contains a different multiset of
numeric tokens than the deterministic text, the response is discarded and the
offline response is shown. This deliberately favours trust over conversational
variety.

The proof of concept uses a narrow parser because it is runnable without keys
and easy to evaluate. In production I would use schema-constrained tool calling
for extraction, validate the schema server-side, and still execute these same
calculators.

### Yarn model

There is no universal formula for metres from dimensions: fibre, stitch
geometry, gauge, borders, shaping, and knitter tension matter. The estimator
therefore exposes editable weight and stitch tables, applies gauge density
quadratically, and adds 10%. Ball count is derived only after metres are known.
The UI must label this an estimate and expose all assumptions.

Sources:

- Craft Yarn Council yarn weights:
  https://www.craftyarncouncil.com/standards/yarn-weight-system
- Craft Yarn Council needle conversions:
  https://www.craftyarncouncil.com/standards/hooks-and-needles
- Interweave yarn-estimation discussion:
  https://www.interweave.com/article/knitting/how-much-yarn-do-i-need/

Before production, we would knit and weigh a matrix of standardized swatches,
fit/calibrate these constants, publish confidence ranges, and version results.

### OpenAI for optional language

OpenAI was selected because its HTTP API is easy to isolate behind a provider
interface. The application has no SDK dependency and no-key/offline behaviour
is first class. Another provider can implement the same interface.

## Assignment B

### OpenAI image generation behind an interface

The image provider is replaceable. Prompts are assembled from validated fields,
and each stitch maps to an explicit structural description. Hex is treated as
the colour source of truth; the human-readable colour name is secondary.

### Full-input, content-addressed cache

The normalized stitch, hex colour, weight, and fibre form the cache key. This
prevents accidental reuse across inputs. Provider/model/prompt version should
also enter the key in production so prompt changes invalidate stale images.

### Visible, deterministic fallback

Provider failure returns an intentionally stylized Pillow placeholder in the
requested colour. Returning a marked fallback is better than a broken screen,
but it must not be presented as a photorealistic prediction.

### Ravelry is optional enrichment

Ravelry lookup cannot block swatch generation. Missing credentials, timeout,
no match, or malformed response produces an explicit unavailable result while
the generated preview remains usable. Photo licensing and attribution must be
reviewed before production display.

## Quality and safety

Production gates would include image moderation, stitch-structure classification,
perceptual colour delta against the requested hex, corruption checks, provider
timeouts, rate limits, spend caps, observability, deletion policy, and a user
report flow. Cached output would store provenance and quality-gate versions.
