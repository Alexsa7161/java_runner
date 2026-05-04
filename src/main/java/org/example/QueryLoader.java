package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QueryLoader {

    public static String loadQuery(String path) {

        InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);

        if (is == null) {
            throw new RuntimeException("Resource not found: " + path);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load query: " + path, e);
        }
    }

    public static List<String> loadAllQueries(String folder, int count) {

        List<String> queries = new ArrayList<>();

        for (int i = 1; i <= count; i++) {

            String path = folder + "/q" + i + ".sql";

            queries.add(loadQuery(path));
        }

        return queries;
    }
}