package com.y0x79min.voiceguard.analysis;

import com.jlibrosa.audio.JLibrosa;

/**
 * 발표자료에 보존된 preprocessAudio 흐름을 공개용으로 정리한 코드입니다.
 * 음성 파일을 읽어 Mel-spectrogram을 만들고 TFLite 입력 크기로 변환합니다.
 */
public final class AudioPreprocessor {
    private AudioPreprocessor() {
    }

    public static float[][][] preprocessAudio(
            String filePath,
            int desiredWidth,
            int desiredHeight,
            int sampleRate,
            int nFft,
            int nMels,
            int hopLength,
            double topDb
    ) throws Exception {
        JLibrosa jLibrosa = new JLibrosa();
        float[] audioMono = jLibrosa.loadAndRead(filePath, sampleRate, -1);
        if (audioMono == null || audioMono.length == 0) {
            throw new IllegalArgumentException("오디오를 읽을 수 없습니다: " + filePath);
        }

        float[][] melPower = jLibrosa.generateMelSpectroGram(
                audioMono,
                sampleRate,
                nFft,
                nMels,
                hopLength
        );
        if (melPower.length == 0 || melPower[0].length == 0) {
            throw new IllegalStateException("Mel-spectrogram이 비어 있습니다.");
        }

        float[][] decibels = powerToDecibels(melPower, topDb);
        return resizeAndRepeatChannels(decibels, desiredWidth, desiredHeight);
    }

    private static float[][] powerToDecibels(float[][] power, double topDb) {
        int height = power.length;
        int width = power[0].length;
        float[][] result = new float[height][width];
        double maxDb = -Double.MAX_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double db = 10.0 * Math.log10(Math.max(power[y][x], 1.0e-10));
                result[y][x] = (float) db;
                maxDb = Math.max(maxDb, db);
            }
        }

        double floor = maxDb - topDb;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double clipped = Math.max(result[y][x], floor);
                result[y][x] = (float) ((clipped - floor) / Math.max(topDb, 1.0));
            }
        }
        return result;
    }

    private static float[][][] resizeAndRepeatChannels(
            float[][] source,
            int desiredWidth,
            int desiredHeight
    ) {
        int sourceHeight = source.length;
        int sourceWidth = source[0].length;
        float[][][] result = new float[desiredHeight][desiredWidth][3];

        for (int y = 0; y < desiredHeight; y++) {
            int sourceY = Math.min(sourceHeight - 1, y * sourceHeight / desiredHeight);
            for (int x = 0; x < desiredWidth; x++) {
                int sourceX = Math.min(sourceWidth - 1, x * sourceWidth / desiredWidth);
                float value = source[sourceY][sourceX];
                result[y][x][0] = value;
                result[y][x][1] = value;
                result[y][x][2] = value;
            }
        }
        return result;
    }
}
