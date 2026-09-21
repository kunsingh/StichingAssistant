package com.knittingai.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KnittingAssistantApplication {
    private KnittingAssistantApplication() {}

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        List<String> values = new ArrayList<>(Arrays.asList(args));
        boolean json = values.remove("--json");
        boolean interactive = values.remove("--interactive") | values.remove("-i");
        values.remove("--verbose");

        KnittingAssistant assistant = OpenAiLanguageProvider.fromEnvironment()
            .<KnittingAssistant>map(KnittingAssistant::new)
            .orElseGet(KnittingAssistant::new);

        if (interactive) {
            runInteractive(assistant, json);
            return;
        }

        if (values.isEmpty()) {
            values = List.of("How much DK yarn for a 50 x 60cm blanket in stockinette?");
        }
        printResponse(assistant.answer(String.join(" ", values)), json);
    }

    private static void runInteractive(KnittingAssistant assistant, boolean json) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("Knitting assistant. Ask a question, or type 'exit' to quit.");
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            String question = line.trim();
            if (question.isEmpty()) continue;
            if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) break;
            printResponse(assistant.answer(question), json);
        }
        System.out.println("Goodbye!");
    }

    private static void printResponse(KnittingAssistant.AssistantResponse response, boolean json) throws Exception {
        if (json) {
            ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            System.out.println(writer.writeValueAsString(response));
        } else {
            System.out.println(response.answer());
        }
    }
}
