package com.knittingai.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AssignmentEvaluatorTest {
    @Test
    void allEvaluationCasesPassAndReportRequiredMetrics() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int exitCode = AssignmentEvaluator.run(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        String report = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(report.contains("Routing: 16/16"));
        assertTrue(report.contains("Answer values: 16/16"));
        assertTrue(report.contains("Mean response time:"));
    }
}
