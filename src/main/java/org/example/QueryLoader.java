package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QueryLoader {

    /**
     * Загружает один SQL файл из resources
     */
    public static String loadQuery(String path) {

        try (InputStream is = QueryLoader.class.getClassLoader().getResourceAsStream(path)) {

            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }

            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            String sql = sb.toString().trim();

            // убираем последний ;
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }

            return sql;
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to load query: " + path, e);
        }
    }

    /**
     * Загружает все TPC-H запросы q1.sql ... qN.sql
     */
    public static List<String> loadAllQueries(String folder, int count) {

        List<String> queries = new ArrayList<>();

        for (int i = 1; i <= count; i++) {

            String path = folder + "/q" + i + ".sql";

            String sql = loadQuery(path);

            if (sql != null && !sql.isBlank()) {
                queries.add(sql);
            }
        }

        return queries;
    }
}