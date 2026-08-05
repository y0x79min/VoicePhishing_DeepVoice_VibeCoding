package com.y0x79min.voiceguard.analysis;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** TFLite 입력 버퍼 구성과 딥보이스 확률 추론을 담당합니다. */
public final class DeepVoiceAnalyzer {
    public static final int INPUT_SIZE = 224;
    public static final int CHANNELS = 3;

    private final Interpreter interpreter;
    private final float warningThreshold;

    public DeepVoiceAnalyzer(Interpreter interpreter, float warningThreshold) {
        this.interpreter = interpreter;
        this.warningThreshold = warningThreshold;
    }

    public Result analyze(float[][][] inputImage) {
        validateShape(inputImage);

        ByteBuffer buffer = ByteBuffer.allocateDirect(
                        INPUT_SIZE * INPUT_SIZE * CHANNELS * Float.BYTES
                )
                .order(ByteOrder.nativeOrder());

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                for (int channel = 0; channel < CHANNELS; channel++) {
                    buffer.putFloat(inputImage[y][x][channel]);
                }
            }
        }
        buffer.rewind();

        float[][] output = new float[1][1];
        interpreter.run(buffer, output);
        float probability = output[0][0];
        return new Result(probability, probability > warningThreshold);
    }

    private static void validateShape(float[][][] inputImage) {
        if (inputImage.length != INPUT_SIZE
                || inputImage[0].length != INPUT_SIZE
                || inputImage[0][0].length != CHANNELS) {
            throw new IllegalArgumentException("입력은 224x224x3 배열이어야 합니다.");
        }
    }

    public record Result(float probability, boolean warning) {
    }
}
