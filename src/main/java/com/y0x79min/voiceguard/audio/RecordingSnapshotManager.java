package com.y0x79min.voiceguard.audio;

import com.y0x79min.voiceguard.call.CallAnalysisCoordinator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 통화 녹음은 계속 유지하면서 현재까지의 PCM을 별도 파일로 복사하고,
 * 분석 경로에는 WAV 스냅샷만 전달합니다.
 */
public final class RecordingSnapshotManager
        implements CallAnalysisCoordinator.AudioSnapshotWriter {
    private final RecordingBackend backend;
    private final File livePcmFile;
    private final File snapshotDirectory;
    private final PcmToWavConverter converter;

    public RecordingSnapshotManager(
            RecordingBackend backend,
            File livePcmFile,
            File snapshotDirectory,
            PcmToWavConverter converter
    ) {
        this.backend = backend;
        this.livePcmFile = livePcmFile;
        this.snapshotDirectory = snapshotDirectory;
        this.converter = converter;
    }

    @Override
    public void startRecording() throws Exception {
        Files.createDirectories(snapshotDirectory.toPath());
        backend.start(livePcmFile);
    }

    @Override
    public File saveCurrentSnapshotAsWav() throws Exception {
        long capturedAt = System.currentTimeMillis();
        File pcmSnapshot = new File(snapshotDirectory, "snapshot-" + capturedAt + ".pcm");
        File wavSnapshot = new File(snapshotDirectory, "snapshot-" + capturedAt + ".wav");

        backend.copyCurrentPcmTo(pcmSnapshot);
        try {
            return converter.convert(pcmSnapshot, wavSnapshot);
        } finally {
            Files.deleteIfExists(pcmSnapshot.toPath());
        }
    }

    @Override
    public void stopRecording() throws Exception {
        backend.stop();
    }

    /**
     * 실제 Android 구현에서는 AudioRecord/MediaRecorder 및 앱 권한 처리를 이 경계에 연결합니다.
     */
    public interface RecordingBackend {
        void start(File destinationPcm) throws Exception;
        void copyCurrentPcmTo(File snapshotPcm) throws IOException;
        void stop() throws Exception;
    }
}
