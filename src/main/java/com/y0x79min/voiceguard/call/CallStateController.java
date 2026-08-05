package com.y0x79min.voiceguard.call;

import java.util.Locale;

/**
 * Android TelephonyCallback/BroadcastReceiver에서 전달된 통화 상태를
 * 녹음 시작, 5초 분석, 통화 종료 처리로 연결합니다.
 */
public final class CallStateController {
    public enum State {
        IDLE,
        RINGING,
        ACTIVE
    }

    private final CallAnalysisCoordinator coordinator;
    private final FiveSecondAnalysisLoop analysisLoop;
    private final RecordingFileFactory recordingFileFactory;
    private final Clock clock;

    private State state = State.IDLE;
    private String phoneNumber = "";
    private long callStartedAt;

    public CallStateController(
            CallAnalysisCoordinator coordinator,
            FiveSecondAnalysisLoop analysisLoop,
            RecordingFileFactory recordingFileFactory,
            Clock clock
    ) {
        this.coordinator = coordinator;
        this.analysisLoop = analysisLoop;
        this.recordingFileFactory = recordingFileFactory;
        this.clock = clock;
    }

    public synchronized void onRinging(String incomingNumber) {
        state = State.RINGING;
        phoneNumber = incomingNumber == null ? "" : incomingNumber;
    }

    public synchronized void onOutgoingCall(String outgoingNumber) {
        phoneNumber = outgoingNumber == null ? "" : outgoingNumber;
    }

    public synchronized void onOffHook() throws Exception {
        if (state == State.ACTIVE) {
            return;
        }

        callStartedAt = clock.now();
        String recordingPath = recordingFileFactory.create(phoneNumber, callStartedAt);
        coordinator.onCallStarted(phoneNumber, recordingPath);
        analysisLoop.start(coordinator::saveRecordingSnapshot);
        state = State.ACTIVE;
    }

    public synchronized void onIdle() throws Exception {
        if (state == State.ACTIVE) {
            analysisLoop.stop();
            coordinator.onCallEnded(formatDuration(clock.now() - callStartedAt));
        }
        state = State.IDLE;
        phoneNumber = "";
        callStartedAt = 0L;
    }

    public synchronized State state() {
        return state;
    }

    private static String formatDuration(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis / 1_000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    @FunctionalInterface
    public interface RecordingFileFactory {
        String create(String phoneNumber, long startedAt) throws Exception;
    }

    @FunctionalInterface
    public interface Clock {
        long now();
    }
}
