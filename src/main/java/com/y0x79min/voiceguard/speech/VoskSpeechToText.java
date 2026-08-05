package com.y0x79min.voiceguard.speech;

import com.y0x79min.voiceguard.call.CallAnalysisCoordinator;

import org.json.JSONObject;
import org.vosk.Recognizer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/** WAV 스냅샷을 Vosk에 공급하고 통화 음성을 한국어 텍스트로 변환합니다. */
public final class VoskSpeechToText implements CallAnalysisCoordinator.SpeechToText, AutoCloseable {
    private static final int WAV_HEADER_BYTES = 44;
    private static final int BUFFER_SIZE = 4_096;

    private final RecognizerFactory recognizerFactory;
    private Recognizer recognizer;

    public VoskSpeechToText(RecognizerFactory recognizerFactory) {
        this.recognizerFactory = recognizerFactory;
    }

    @Override
    public synchronized void reset() {
        closeRecognizer();
    }

    @Override
    public synchronized String recognize(File wavFile) throws Exception {
        closeRecognizer();
        recognizer = recognizerFactory.create();

        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(wavFile))) {
            long skipped = input.skip(WAV_HEADER_BYTES);
            if (skipped < WAV_HEADER_BYTES) {
                throw new IllegalArgumentException("WAV 헤더를 읽을 수 없습니다: " + wavFile);
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, read);
            }
        }

        JSONObject result = new JSONObject(recognizer.getFinalResult());
        return result.optString("text", "").trim();
    }

    @Override
    public synchronized void close() {
        closeRecognizer();
    }

    private void closeRecognizer() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
    }

    @FunctionalInterface
    public interface RecognizerFactory {
        Recognizer create() throws Exception;
    }
}
