package com.y0x79min.voiceguard.call;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** 통화 중 녹음 스냅샷 분석을 5초 간격으로 실행합니다. */
public final class FiveSecondAnalysisLoop implements AutoCloseable {
    public static final long ANALYSIS_INTERVAL_SECONDS = 5L;

    private final ScheduledExecutorService executor;
    private final ErrorHandler errorHandler;
    private ScheduledFuture<?> task;

    public FiveSecondAnalysisLoop(ErrorHandler errorHandler) {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "voice-guard-analysis");
            thread.setDaemon(true);
            return thread;
        });
        this.errorHandler = errorHandler;
    }

    public synchronized void start(AnalysisTask analysisTask) {
        stop();
        task = executor.scheduleAtFixedRate(() -> {
            try {
                analysisTask.run();
            } catch (Exception error) {
                errorHandler.onAnalysisError(error);
            }
        }, ANALYSIS_INTERVAL_SECONDS, ANALYSIS_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    @Override
    public synchronized void close() {
        stop();
        executor.shutdownNow();
    }

    @FunctionalInterface
    public interface AnalysisTask {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ErrorHandler {
        void onAnalysisError(Exception error);
    }
}
