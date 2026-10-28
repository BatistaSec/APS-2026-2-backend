package com.app.util.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class AsyncLogger {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int QUEUE_CAPACITY = 4096;

    private static final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Thread workerThread;

    static {
        workerThread = new Thread(AsyncLogger::processLogs, "AsyncLogger-Worker");
        workerThread.setDaemon(true);
        workerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(AsyncLogger::shutdown, "AsyncLogger-ShutdownHook"));
    }

    private static void processLogs() {
        while (running.get() || !queue.isEmpty()) {
            try {
                LogEntry entry = queue.poll();
                if (entry != null) {
                    imprimir(entry);
                } else {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {

                System.err.println("[LOG ERROR] " + e.getMessage());
            }
        }
    }

    private static void imprimir(LogEntry entry) {
        String msg = String.format("[%s] [%s] [%s] %s%n",
                entry.timestamp(),
                entry.level(),
                entry.threadName(),
                entry.message()
        );

        if ("ERROR".equals(entry.level())) {
            System.err.print(msg);
            if (entry.throwable() != null) {
                entry.throwable().printStackTrace(System.err);
            }
        } else {
            System.out.print(msg);
        }
    }

    public static void info(String message) {
        enqueue("INFO", message, null);
    }

    public static void warn(String message) {
        enqueue("WARN", message, null);
    }

    public static void error(String message) {
        enqueue("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        enqueue("ERROR", message, throwable);
    }

    public static void debug(String message) {
        enqueue("DEBUG", message, null);
    }

    private static void enqueue(String level, String message, Throwable throwable) {
        if (!running.get()) {
            imprimir(new LogEntry(LocalDateTime.now().format(TIME_FORMAT), level, Thread.currentThread().getName(), message, throwable));
            return;
        }

        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(TIME_FORMAT),
                level,
                Thread.currentThread().getName(),
                message,
                throwable
        );


        if (!queue.offer(entry)) {
            System.err.println("[ASYNC LOG BUFFER FULL] Descartando log: " + message);
        }
    }

    public static void shutdown() {
        running.set(false);
        try {

            while (!queue.isEmpty()) {
                LogEntry entry = queue.poll();
                if (entry != null) imprimir(entry);
            }
        } catch (Exception ignored) {}
    }

    private record LogEntry(String timestamp, String level, String threadName, String message, Throwable throwable) {}
}
