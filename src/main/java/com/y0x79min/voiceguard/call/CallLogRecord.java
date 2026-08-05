package com.y0x79min.voiceguard.call;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** 통화 종료 후 기록 화면에서 다시 확인할 분석 결과입니다. */
public final class CallLogRecord {
    private final String phoneNumber;
    private final String recordingFile;
    private final long startedAt;
    private float deepVoiceProbability;
    private float voicePhishingProbability;
    private String stt = "";
    private String duration = "00:00";

    public CallLogRecord(String phoneNumber, String recordingFile, long startedAt) {
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber;
        this.recordingFile = recordingFile == null ? "" : recordingFile;
        this.startedAt = startedAt;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String recordingFile() {
        return recordingFile;
    }

    public long startedAt() {
        return startedAt;
    }

    public float deepVoiceProbability() {
        return deepVoiceProbability;
    }

    public float voicePhishingProbability() {
        return voicePhishingProbability;
    }

    public String stt() {
        return stt;
    }

    public String duration() {
        return duration;
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
        this.duration = duration == null ? "00:00" : duration;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("phoneNumber", phoneNumber)
                .put("recordingFile", recordingFile)
                .put("startedAt", startedAt)
                .put("deepVoiceProbability", deepVoiceProbability)
                .put("voicePhishingProbability", voicePhishingProbability)
                .put("stt", stt)
                .put("duration", duration);
    }

    public static CallLogRecord fromJson(JSONObject value) {
        CallLogRecord record = new CallLogRecord(
                value.optString("phoneNumber", ""),
                value.optString("recordingFile", ""),
                value.optLong("startedAt", 0L)
        );
        record.updateDeepVoiceProbability((float) value.optDouble("deepVoiceProbability", 0.0));
        record.updateVoicePhishingProbability((float) value.optDouble("voicePhishingProbability", 0.0));
        record.updateStt(value.optString("stt", ""));
        record.updateDuration(value.optString("duration", "00:00"));
        return record;
    }

    /** 발표 당시 통화 기록 화면에서 사용한 한글 키 구조입니다. */
    public Map<String, Object> toDisplayMap() {
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
