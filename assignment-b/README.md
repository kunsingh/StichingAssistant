# Assignment B: AI Knit Swatch Preview

A plain Java 25 CLI (built with Maven, no application framework) that creates
stitch-specific prompts, calls an OpenAI-compatible Images endpoint in live
mode, and always returns an image. Provider failure produces a deterministic
Java2D texture in the requested hex colour rather than crashing.

## Run offline

```powershell
mvn -pl assignment-b test
mvn -pl assignment-b compile exec:java
```

The demo writes comparison PNGs, a JSON report, and its SHA-256 cache beneath
`artifacts`. It also writes `artifacts/gallery.html` — open it in a browser to
see the hero previews side by side and zoom into the individual swatches.

Or build a self-contained runnable jar (bundles dependencies) and run it
directly:

```powershell
mvn -pl assignment-b package
java -jar assignment-b/target/swatch-preview.jar
```

## Optional live services

Configure credentials in `.env` or the process environment. Either OpenAI or
Azure OpenAI works; if `OPENAI_BASE_URL` is unset, `AZURE_OPENAI_ENDPOINT` is
used and the OpenAI-compatible `/openai/v1` surface is called with the key as a
bearer token.

```powershell
# OpenAI
$env:OPENAI_API_KEY = "..."
$env:OPENAI_BASE_URL = "https://api.openai.com/v1"

# or Azure OpenAI / AI Foundry (endpoint + key)
$env:AZURE_OPENAI_API_KEY = "..."
$env:AZURE_OPENAI_ENDPOINT = "https://<resource>.openai.azure.com"

# image model (defaults to FLUX.2-pro when unset)
$env:OPENAI_IMAGE_MODEL = "FLUX.2-pro"

# Ravelry (optional)
$env:RAVELRY_USERNAME = "..."
$env:RAVELRY_ACCESS_KEY = "..."
mvn -pl assignment-b compile exec:java '-Dexec.args=--live --output artifacts-live'
```

Image config resolves in this order: `OPENAI_API_KEY` then `AZURE_OPENAI_API_KEY`
for the key; `OPENAI_BASE_URL` then `AZURE_OPENAI_ENDPOINT` (+ `/openai/v1`) for
the endpoint; and `OPENAI_IMAGE_MODEL` for the model, defaulting to `FLUX.2-pro`.

For a Foundry provider model whose route differs from `/images/generations` (for
example FLUX.2-pro on Azure AI Foundry), set the full image URL explicitly and
the request body switches from `size` to `width`/`height`:

```powershell
$env:OPENAI_IMAGE_URL = "https://<resource>.services.ai.azure.com/providers/blackforestlabs/v1/flux-2-pro?api-version=preview"
$env:OPENAI_IMAGE_MODEL = "FLUX.2-pro"
```

Ravelry lookup is optional and failures remain isolated from image generation.
