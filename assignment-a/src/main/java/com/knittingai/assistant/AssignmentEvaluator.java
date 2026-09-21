package com.knittingai.assistant;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

public final class AssignmentEvaluator {
    private record Case(String question, String calculation, List<String> expectedFragments) {}

    private static final List<Case> CASES = List.of(
        new Case("How much DK yarn for a 50 x 60cm blanket in stockinette?", "yarn_quantity", List.of("364", "2")),
        new Case("How many metres of worsted yarn for a 100 by 100 cm cable blanket?", "yarn_quantity",
            List.of("1857", "11")),
        new Case("How much fingering yarn for a 20 x 180cm stockinette scarf?", "yarn_quantity",
            List.of("317", "1")),
        new Case("How much bulky yarn for a 80 x 100 cm garter blanket?", "yarn_quantity",
            List.of("1761", "15")),
        new Case("How much sport yarn for a 30 x 150cm lace scarf?", "yarn_quantity", List.of("377", "2")),
        new Case("What needle size for worsted yarn for a balanced scarf?", "needle_size",
            List.of("5.5", "UK 5", "US 9")),
        new Case("What needles for DK yarn for a firm hat?", "needle_size", List.of("3.5", "US 4")),
        new Case("Recommend needles for bulky yarn for a drapey blanket", "needle_size",
            List.of("8.0", "US 11")),
        new Case("Needle size for fingering yarn and balanced socks?", "needle_size",
            List.of("2.75", "US 2")),
        new Case("Needle size for aran yarn and firm sweater?", "needle_size", List.of("5.0", "US 8")),
        new Case("My swatch is 24 stitches per 10cm but the pattern says 22", "tension",
            List.of("too tight", "9.1%")),
        new Case("My swatch is 19 stitches per 10cm but the target is 22", "tension",
            List.of("too loose", "13.6%")),
        new Case("My actual gauge is 22 stitches per 10cm and target is 22", "tension",
            List.of("on target", "0.0%")),
        new Case("Which sweater pattern is prettiest?", null, List.of("can't answer")),
        new Case("How much yarn do I need?", null, List.of("can't answer")),
        new Case("Will this yarn itch?", null, List.of("can't answer"))
    );

    private AssignmentEvaluator() {}

    public static void main(String[] args) {
        System.exit(run(System.out));
    }

    static int run(PrintStream output) {
        KnittingAssistant assistant = new KnittingAssistant();
        int routePasses = 0;
        int valuePasses = 0;
        long totalElapsedNanos = 0;

        output.println("case | route | values | latency_ms");
        for (int index = 0; index < CASES.size(); index++) {
            Case evaluationCase = CASES.get(index);
            long start = System.nanoTime();
            KnittingAssistant.AssistantResponse response = assistant.answer(evaluationCase.question());
            long elapsedNanos = System.nanoTime() - start;
            double elapsedMs = elapsedNanos / 1_000_000.0;

            boolean routeOk = java.util.Objects.equals(response.calculation(), evaluationCase.calculation());
            String normalizedAnswer = response.answer().toLowerCase(Locale.ROOT);
            boolean valuesOk = evaluationCase.expectedFragments().stream()
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .allMatch(normalizedAnswer::contains);

            if (routeOk) routePasses++;
            if (valuesOk) valuePasses++;
            totalElapsedNanos += elapsedNanos;
            output.printf(Locale.ROOT, "%4d | %-5s | %-6s | %10.3f%n",
                index + 1, routeOk ? "PASS" : "FAIL", valuesOk ? "PASS" : "FAIL", elapsedMs);
        }

        double meanElapsedMs = totalElapsedNanos / 1_000_000.0 / CASES.size();
        output.printf(Locale.ROOT, "%nRouting: %d/%d%n", routePasses, CASES.size());
        output.printf(Locale.ROOT, "Answer values: %d/%d%n", valuePasses, CASES.size());
        output.printf(Locale.ROOT, "Mean response time: %.3f ms%n", meanElapsedMs);
        return routePasses == CASES.size() && valuePasses == CASES.size() ? 0 : 1;
    }
}
