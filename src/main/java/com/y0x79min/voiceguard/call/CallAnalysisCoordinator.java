package com.y0x79min.voiceguard.call;

import com.y0x79min.voiceguard.analysis.DeepVoiceAnalyzer;

import java.io.File;

/**
 * 발표자료의 통화 상태 변경 및 saveRecordingSnapshot 흐름을 의존성 인터페이스로 분리했습니다.
 * Android Service, MediaRecorder, Vosk, 서버 문맥 모델과 UI 구현은 공개 범위에서 제외합니다.
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

    public void onCallStarted(String phoneNumber, String recordingFilePath) throws Exception {
        loadOrCreateCallLog();
        createNewCallRecord(phoneNumber, recordingFilePath);
        startRecording();
        warningUi.showOverlay();
    }

    /** 통화 중 5초마다, 그리고 통화 종료 직전에 한 번 더 호출합니다. */
    public void saveRecordingSnapshot() throws Exception {
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

    public void onCallEnded(String duration) throws Exception {
        saveRecordingSnapshot();
        currentRecord.updateDuration(duration);
        callLogStore.save(currentRecord);
        snapshotWriter.stopRecording();
        warningUi.hideOverlay();
    }

    private void loadOrCreateCallLog() throws Exception {
        callLogStore.loadOrCreate();
    }

    private void createNewCallRecord(String phoneNumber, String recordingFilePath) {
        currentRecord = new CallLogRecord(phoneNumber, recordingFilePath, System.currentTimeMillis());
    }

    private void startRecording() throws Exception {
        snapshotWriter.startRecording();
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
