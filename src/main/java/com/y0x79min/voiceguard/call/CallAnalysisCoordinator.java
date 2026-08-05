package com.y0x79min.voiceguard.call;

import com.y0x79min.voiceguard.analysis.DeepVoiceAnalyzer;

import java.io.File;

/**
 * 녹음 스냅샷 하나를 딥보이스 분석과 보이스피싱 문맥 분석으로 나누고,
 * 경고 UI 및 통화 기록까지 연결하는 Android 앱의 중심 흐름입니다.
 */
public final class CallAnalysisCoordinator {
    private final AudioSnapshotWriter snapshotWriter;
    private final AudioFeatureExtractor featureExtractor;
    private final DeepVoiceAnalyzer deepVoiceAnalyzer;
    private final SpeechToText speechToText;
    private final VoicePhishingAnalyzer voicePhishingAnalyzer;
    private final CallLogStore callLogStore;
    private final WarningUi warningUi;

    private CallLogRecord currentRecord;

    public CallAnalysisCoordinator(
            AudioSnapshotWriter snapshotWriter,
            AudioFeatureExtractor featureExtractor,
            DeepVoiceAnalyzer deepVoiceAnalyzer,
            SpeechToText speechToText,
            VoicePhishingAnalyzer voicePhishingAnalyzer,
            CallLogStore callLogStore,
            WarningUi warningUi
    ) {
        this.snapshotWriter = snapshotWriter;
        this.featureExtractor = featureExtractor;
        this.deepVoiceAnalyzer = deepVoiceAnalyzer;
        this.speechToText = speechToText;
        this.voicePhishingAnalyzer = voicePhishingAnalyzer;
        this.callLogStore = callLogStore;
        this.warningUi = warningUi;
    }

    public synchronized void onCallStarted(String phoneNumber, String recordingFilePath)
            throws Exception {
        if (currentRecord != null) {
            throw new IllegalStateException("이미 통화 분석이 진행 중입니다.");
        }
        callLogStore.loadOrCreate();
        currentRecord = new CallLogRecord(
                phoneNumber,
                recordingFilePath,
                System.currentTimeMillis()
        );
        snapshotWriter.startRecording();
        warningUi.showOverlay();
    }

    /** 통화 중 5초마다, 그리고 통화 종료 직전에 한 번 더 호출합니다. */
    public synchronized void saveRecordingSnapshot() throws Exception {
        if (currentRecord == null) {
            return;
        }

        File wavFile = snapshotWriter.saveCurrentSnapshotAsWav();

        float[][][] input = featureExtractor.preprocess(wavFile);
        DeepVoiceAnalyzer.Result deepVoice = deepVoiceAnalyzer.analyze(input);
        currentRecord.updateDeepVoiceProbability(deepVoice.probability());
        warningUi.showDeepVoiceProbability(deepVoice.probability());
        if (deepVoice.warning()) {
            warningUi.showDeepVoiceWarning();
            warningUi.vibrate();
        }

        speechToText.reset();
        String stt = speechToText.recognize(wavFile);
        if (stt != null && !stt.isBlank()) {
            currentRecord.updateStt(stt);
            float phishingProbability = voicePhishingAnalyzer.analyze(stt);
            currentRecord.updateVoicePhishingProbability(phishingProbability);
            warningUi.showVoicePhishingProbability(phishingProbability);
            if (voicePhishingAnalyzer.isWarning(phishingProbability)) {
                warningUi.showVoicePhishingWarning();
                warningUi.vibrate();
            }
        }

        callLogStore.save(currentRecord);
    }

    public synchronized void onCallEnded(String duration) throws Exception {
        if (currentRecord == null) {
            return;
        }

        Exception failure = null;
        try {
            saveRecordingSnapshot();
            currentRecord.updateDuration(duration);
            callLogStore.save(currentRecord);
        } catch (Exception error) {
            failure = error;
        }

        try {
            snapshotWriter.stopRecording();
        } catch (Exception stopError) {
            if (failure == null) {
                failure = stopError;
            } else {
                failure.addSuppressed(stopError);
            }
        } finally {
            warningUi.hideOverlay();
            currentRecord = null;
        }

        if (failure != null) {
            throw failure;
        }
    }

    public interface AudioSnapshotWriter {
        void startRecording() throws Exception;
        File saveCurrentSnapshotAsWav() throws Exception;
        void stopRecording() throws Exception;
    }

    public interface AudioFeatureExtractor {
        float[][][] preprocess(File wavFile) throws Exception;
    }

    public interface SpeechToText {
        void reset();
        String recognize(File wavFile) throws Exception;
    }

    public interface VoicePhishingAnalyzer {
        float analyze(String text) throws Exception;
        boolean isWarning(float probability);
    }

    public interface CallLogStore {
        void loadOrCreate() throws Exception;
        void save(CallLogRecord record) throws Exception;
    }

    public interface WarningUi {
        void showOverlay();
        void hideOverlay();
        void showDeepVoiceProbability(float probability);
        void showVoicePhishingProbability(float probability);
        void showDeepVoiceWarning();
        void showVoicePhishingWarning();
        void vibrate();
    }
}
