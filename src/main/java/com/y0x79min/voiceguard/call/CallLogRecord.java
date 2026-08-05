package com.y0x79min.voiceguard.call;

import java.util.LinkedHashMap;
import java.util.Map;

/** 통화 종료 후 화면에서 다시 확인할 분석 결과입니다. */
public final class CallLogRecord {
    private final String phoneNumber;
    private final String recordingFile;
    private final long startedAt;
    private float deepVoiceProbability;
    private float voicePhishingProbability;
    private String stt = "";
    private String duration = "00:00";

    public CallLogRecord(String phoneNumber, String recordingFile, long startedAt) {
        this.phoneNumber = phoneNumber;
        this.recordingFile = recordingFile;
        this.startedAt = startedAt;
    }

    public void updateDeepVoiceProbability(float probability) {
        this.deepVoiceProbability = probability;
    }

    public void updateVoicePhishingProbability(float probability) {
        this.voicePhishingProbability = probability;
    }

    public void updateStt(String stt) {
        this.stt = stt == null ? "" : stt;
    }

    public void updateDuration(String duration) {
        this.duration = duration;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("전화번호", phoneNumber);
        value.put("녹음파일", recordingFile);
        value.put("시작시간", startedAt);
        value.put("딥보이스탐지", deepVoiceProbability);
        value.put("보이스피싱탐지", voicePhishingProbability);
        value.put("STT", stt);
        value.put("통화길이", duration);
        return value;
    }
}
