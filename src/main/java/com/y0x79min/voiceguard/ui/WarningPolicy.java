package com.y0x79min.voiceguard.ui;

/** 두 탐지 확률의 임계값을 독립적으로 적용해 화면 경고 상태를 계산합니다. */
public final class WarningPolicy {
    private final float deepVoiceThreshold;
    private final float voicePhishingThreshold;

    public WarningPolicy(float deepVoiceThreshold, float voicePhishingThreshold) {
        this.deepVoiceThreshold = deepVoiceThreshold;
        this.voicePhishingThreshold = voicePhishingThreshold;
    }

    public WarningState evaluate(float deepVoiceProbability, float voicePhishingProbability) {
        return new WarningState(
                deepVoiceProbability,
                voicePhishingProbability,
                deepVoiceProbability >= deepVoiceThreshold,
                voicePhishingProbability >= voicePhishingThreshold
        );
    }

    public record WarningState(
            float deepVoiceProbability,
            float voicePhishingProbability,
            boolean deepVoiceWarning,
            boolean voicePhishingWarning
    ) {
        public boolean anyWarning() {
            return deepVoiceWarning || voicePhishingWarning;
        }

        public boolean combinedWarning() {
            return deepVoiceWarning && voicePhishingWarning;
        }
    }
}
