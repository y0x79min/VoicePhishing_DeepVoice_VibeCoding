package com.y0x79min.voiceguard.ui;

import com.y0x79min.voiceguard.call.CallAnalysisCoordinator;

import java.util.Locale;

/**
 * 통화 화면 위 오버레이에 두 탐지 확률을 표시하고,
 * 임계값 초과 시 경고 문구와 진동을 실행합니다.
 */
public final class FloatingWarningController implements CallAnalysisCoordinator.WarningUi {
    private final OverlayView overlayView;
    private final VibrationDevice vibrationDevice;
    private boolean visible;

    public FloatingWarningController(
            OverlayView overlayView,
            VibrationDevice vibrationDevice
    ) {
        this.overlayView = overlayView;
        this.vibrationDevice = vibrationDevice;
    }

    @Override
    public void showOverlay() {
        visible = true;
        overlayView.show();
        overlayView.showStatus("통화 음성 분석 중");
    }

    @Override
    public void hideOverlay() {
        visible = false;
        overlayView.hide();
    }

    @Override
    public void showDeepVoiceProbability(float probability) {
        if (visible) {
            overlayView.showDeepVoiceProbability(asPercent(probability));
        }
    }

    @Override
    public void showVoicePhishingProbability(float probability) {
        if (visible) {
            overlayView.showVoicePhishingProbability(asPercent(probability));
        }
    }

    @Override
    public void showDeepVoiceWarning() {
        if (visible) {
            overlayView.showStatus("딥보이스가 의심되는 음성이 탐지되었습니다.");
        }
    }

    @Override
    public void showVoicePhishingWarning() {
        if (visible) {
            overlayView.showStatus("보이스피싱이 의심되는 문맥이 탐지되었습니다.");
        }
    }

    @Override
    public void vibrate() {
        vibrationDevice.vibrate(new long[]{0L, 250L, 120L, 250L});
    }

    private static String asPercent(float probability) {
        return String.format(Locale.US, "%.1f%%", probability * 100.0f);
    }

    public interface OverlayView {
        void show();
        void hide();
        void showStatus(String message);
        void showDeepVoiceProbability(String probability);
        void showVoicePhishingProbability(String probability);
    }

    @FunctionalInterface
    public interface VibrationDevice {
        void vibrate(long[] pattern);
    }
}
