# Demo Flow

A scripted walkthrough of both proofs of concept. It doubles as the running
order for the video. Everything runs offline with no API keys; the Assignment B
live image step is optional.

Run from the repository root.

## One-command demo

```powershell
# Offline (no keys needed): both assignments end to end
.\demo.ps1

# Pause between steps (good for screen recording) and open the gallery at the end
.\demo.ps1 -Pause -Open

# Also run the real image provider for Assignment B (uses .env / provider quota)
.\demo.ps1 -Live -Open
```

Switches: `-Live` (real images), `-Pause` (wait between steps), `-SkipTests`
(demonstrations only), `-Open` (open the gallery when finished).

---

## Assignment A — AI Assistant with Trustworthy Numbers

**Point to make:** every number in an answer comes from deterministic Java
calculators, and the assistant refuses questions it cannot compute.

### A1. Tests

```powershell
mvn -pl assignment-a test
```

Shows calculator, assistant, and evaluator tests passing (numbers are covered by
unit tests, not trusted from the model).

### A2. Answer real calculation questions

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.args=How much DK yarn for a 50 x 60cm blanket in stockinette?'

mvn -pl assignment-a compile exec:java `
  '-Dexec.args=What needle size for worsted yarn for a balanced scarf?'

mvn -pl assignment-a compile exec:java `
  '-Dexec.args=My swatch is 24 stitches per 10cm but the pattern says 22'
```

Each routes to a different calculator (yarn quantity, needle size, tension).

> Tip: `exec:java` splits `exec.args` and rejects apostrophes/quotes. For
> questions containing an apostrophe (e.g. "What's wrong?"), build the jar once
> and run it directly - this is what `demo.ps1` does:
>
> ```powershell
> mvn -pl assignment-a package
> java -jar assignment-a/target/knitting-assistant.jar "My swatch is 24 stitches per 10cm but the pattern says 22. What's wrong?"
> ```

### A3. Decline an unanswerable question (trust behavior)

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.args=Which sweater pattern is the prettiest?'
```

The assistant says it cannot answer rather than guessing.

### A4. Show provenance with JSON

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.args=--json What needle size for worsted yarn for a balanced scarf?'
```

`calculation` and `values` show exactly which calculator produced the numbers;
`providerStatus` shows whether the LLM phrasing was accepted or rejected.

### A5. Evaluation suite (16 questions)

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.mainClass=com.knittingai.assistant.AssignmentEvaluator'
```

Reports routing accuracy, answer-value accuracy, per-question latency, and mean
response time. Exits non-zero if any expected calculation or value is wrong.

### A6. (Optional) interactive mode

```powershell
mvn -pl assignment-a compile exec:java '-Dexec.args=--interactive'
```

---

## Assignment B — AI Knit Swatch Preview

**Points to make:** stitch structure actually differs (cable vs rib vs
stockinette), colour matches the hex, the provider-failure fallback triggers on
purpose, the cache skips a repeated call, and Ravelry is a real (optional)
lookup.

### B1. Tests

```powershell
mvn -pl assignment-b test
```

Covers input validation, stitch-specific prompts, SHA-256 cache keying, the
fallback path, and graceful Ravelry handling.

### B2. Offline demo (fallback + cache, no keys)

```powershell
mvn -pl assignment-b compile exec:java '-Dexec.args=--output artifacts-demo'
```

The report shows `simulated_provider_failure: true`, `fallback_used: true`, and
`cache_hit_demonstrated: true`. Open the gallery:

```powershell
start artifacts-demo\gallery.html
```

The gallery shows the colour-switching row (one stitch, three colours), the
stitch-variation row (three stitches, one colour), and the Ravelry comparison.

### B3. Live image generation (optional)

Configure credentials in `.env` (see `.env.example`), then:

```powershell
mvn -pl assignment-b compile exec:java '-Dexec.args=--live --output artifacts-live'
start artifacts-live\gallery.html
```

Now `fallback_used: false` and generation takes several seconds per image — the
real latency tension the product must design around.

---

## Video running order (maps to the brief)

1. One-line intro: what each assignment is and why it is structured this way.
2. Assignment A: answer a calculation question, then a refusal (A2 + A3), then
   JSON provenance (A4) and the evaluation run (A5).
3. Assignment B: offline gallery (B2) — call out cable vs rib vs stockinette and
   the exact colour; show `fallback_used` and the cache hit in the report.
4. Assignment B: live gallery (B3) if credentials are available.
5. Production readiness (separate from latency): image moderation / content
   safety, cost controls when thousands of users try many colours, and behavior
   when an AI provider is down. See `DECISIONS.md`.
6. Two-minute delivery-plan summary and the single biggest risk. See
   `DELIVERY_PLAN.md` and `LATENCY_NOTE.md`.

## Generated output (safe to delete; git-ignored)

- `artifacts-demo/` — offline demo (placeholders) + `gallery.html`
- `artifacts-live/` — live images + `gallery.html`
- Each contains `demo-report.json`, comparison PNGs, and a SHA-256 `cache/`.
