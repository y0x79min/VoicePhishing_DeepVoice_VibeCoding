package com.y0x79min.voiceguard.audio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Android 녹음기의 raw PCM 스냅샷에 WAV 헤더를 붙여 분석 가능한 파일로 변환합니다. */
public final class PcmToWavConverter {
    private final int sampleRate;
    private final short channels;
    private final short bitsPerSample;

    public PcmToWavConverter(int sampleRate, short channels, short bitsPerSample) {
        if (sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0) {
            throw new IllegalArgumentException("유효한 PCM 형식이 필요합니다.");
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
    }

    public File convert(File pcmFile, File wavFile) throws IOException {
        long pcmLength = pcmFile.length();
        if (pcmLength <= 0L) {
            throw new IOException("녹음 스냅샷이 비어 있습니다: " + pcmFile);
        }

        File parent = wavFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("WAV 폴더를 만들 수 없습니다: " + parent);
        }

        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(pcmFile));
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(wavFile))) {
            writeHeader(output, pcmLength);
            input.transferTo(output);
        }
        return wavFile;
    }

    private void writeHeader(OutputStream output, long dataSize) throws IOException {
        int blockAlign = channels * bitsPerSample / 8;
        long byteRate = (long) sampleRate * blockAlign;

        output.write(new byte[]{'R', 'I', 'F', 'F'});
        writeLittleEndian(output, 36L + dataSize, 4);
        output.write(new byte[]{'W', 'A', 'V', 'E'});
        output.write(new byte[]{'f', 'm', 't', ' '});
        writeLittleEndian(output, 16, 4);
        writeLittleEndian(output, 1, 2);
        writeLittleEndian(output, channels, 2);
        writeLittleEndian(output, sampleRate, 4);
        writeLittleEndian(output, byteRate, 4);
        writeLittleEndian(output, blockAlign, 2);
        writeLittleEndian(output, bitsPerSample, 2);
        output.write(new byte[]{'d', 'a', 't', 'a'});
        writeLittleEndian(output, dataSize, 4);
    }

    private static void writeLittleEndian(OutputStream output, long value, int bytes)
            throws IOException {
        for (int index = 0; index < bytes; index++) {
            output.write((int) (value >> (8 * index)) & 0xff);
        }
    }
}
