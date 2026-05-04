package org.example.adaptive.experiment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoadGenerator {

    private final DataSource dataSource;
    private final List<String> queries;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public LoadGenerator(DataSource dataSource, List<String> queries) {
        this.dataSource = dataSource;
        this.queries = queries;
    }

    public void start() {

        if (running.get()) return;

        running.set(true);

        thread = new Thread(() -> {

            while (running.get()) {

                for (String q : queries) {

                    if (!running.get()) break;

                    try (Connection c = dataSource.getConnection();
                         PreparedStatement ps = c.prepareStatement(q)) {

                        ps.execute();


                        Thread.sleep(10);

                    } catch (Exception ignored) {
                    }
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);

        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(2000);
            } catch (InterruptedException ignored) {}
        }
    }
}