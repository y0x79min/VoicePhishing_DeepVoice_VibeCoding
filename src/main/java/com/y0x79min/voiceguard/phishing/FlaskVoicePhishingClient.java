package com.y0x79min.voiceguard.phishing;

import com.y0x79min.voiceguard.call.CallAnalysisCoordinator;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Vosk STT 결과를 Flask 문맥 탐지 API에 전송하고
 * 보이스피싱 확률을 Android 경고 흐름으로 반환합니다.
 */
public final class FlaskVoicePhishingClient
        implements CallAnalysisCoordinator.VoicePhishingAnalyzer {
    private final URL endpoint;
    private final float warningThreshold;
    private final int timeoutMillis;

    public FlaskVoicePhishingClient(
            URL endpoint,
            float warningThreshold,
            int timeoutMillis
    ) {
        this.endpoint = endpoint;
        this.warningThreshold = warningThreshold;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public float analyze(String text) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        byte[] request = new JSONObject()
                .put("text", text)
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        try {
            try (BufferedOutputStream output =
                         new BufferedOutputStream(connection.getOutputStream())) {
                output.write(request);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("문맥 탐지 서버 오류: HTTP " + status);
            }

            String response;
            try (BufferedInputStream input =
                         new BufferedInputStream(connection.getInputStream())) {
                response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            JSONObject json = new JSONObject(response);
            double rawProbability = json.has("probability")
                    ? json.getDouble("probability")
                    : json.getDouble("voice_phishing_probability");
            return (float) Math.max(0.0, Math.min(1.0, rawProbability));
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public boolean isWarning(float probability) {
        return probability >= warningThreshold;
    }
}
