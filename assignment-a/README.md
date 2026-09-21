# Assignment A: Trustworthy Knitting Assistant

This plain Java 25 CLI (built with Maven, no application framework) separates
language handling from deterministic calculation. It supports yarn quantity,
needle size, and tension questions. Unsupported or incomplete questions are
declined rather than guessed.

## Run

From the repository root:

```powershell
mvn -pl assignment-a test
mvn -pl assignment-a compile exec:java `
  '-Dexec.args=How much DK yarn for a 50 x 60cm blanket in stockinette?'
```

For structured output, put `--json` first:

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.args=--json What needle size for worsted yarn for a balanced scarf?'
```

Or build a self-contained runnable jar (bundles dependencies) and run it
directly:

```powershell
mvn -pl assignment-a package
java -jar assignment-a/target/knitting-assistant.jar "How much DK yarn for a 50 x 60cm blanket in stockinette?"
```

## Interactive mode

Pass `--interactive` (or `-i`) to ask questions in a loop. Type a question at
the `>` prompt and press Enter; type `exit` (or `quit`, or Ctrl+Z then Enter on
Windows) to stop. Add `--json` for structured output per answer.

```powershell
mvn -q -pl assignment-a compile exec:java '-Dexec.args=--interactive'
```

No API key is required. Set `OPENAI_API_KEY` to enable optional OpenAI
understanding and phrasing. Provider output is accepted only when its numeric
tokens match the deterministic result; otherwise the safe offline response is
returned.

## Evaluate

Run the offline evaluation suite from the repository root:

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.mainClass=com.knittingai.assistant.AssignmentEvaluator'
```

The evaluator runs 16 representative questions and reports routing accuracy,
answer-value accuracy, per-question latency, and mean response time. It exits
with a nonzero status if any expected calculation or answer value does not
match.

## Dependencies

The only third-party runtime dependency is Jackson (JSON handling for the
optional OpenAI provider and `--json` output). Tests use JUnit 5. HTTP calls use
the JDK's built-in `java.net.http.HttpClient`.

## Calculation model

Yarn metres are:

`area m2 * metres/m2 for weight * stitch multiplier * gauge ratio squared * 1.10`

The result is rounded up. Ball count uses a configurable metres-per-ball value,
defaulting to a representative 100 g ball for each weight.

Run the offline evaluation suite from the repository root:

```powershell
mvn -pl assignment-a compile exec:java `
  '-Dexec.mainClass=com.knittingai.assistant.AssignmentEvaluator'
```

The evaluator runs 16 representative questions and reports routing accuracy,
answer-value accuracy, per-question latency, and mean response time. It exits
with a nonzero status if any expected calculation or answer value does not
match.

## Calculation model

Yarn metres are:

`area m2 * metres/m2 for weight * stitch multiplier * gauge ratio squared * 1.10`

The result is rounded up. Ball count uses a configurable metres-per-ball value,
defaulting to a representative 100 g ball for each weight.
