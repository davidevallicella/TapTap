package com.cellasoft.taptap;

public class HighPassFilter {
    private final float alpha = 0.4f;
    private float[] acceleration = {0, 0, 0};


    public float[] highPassFilter(float[] input) {
        // Update the Android Developer low-pass filter
        // y[i] = y[i] * alpha + (1 - alpha) * x[i]final float alpha = 0.8;

        //high-pass filter to eliminate gravity
        this.acceleration[0] = input[0] * alpha + acceleration[0] * (1.0f - alpha);
        this.acceleration[1] = input[1] * alpha + acceleration[1] * (1.0f - alpha);
        this.acceleration[2] = input[2] * alpha + acceleration[2] * (1.0f - alpha);

        input[0] -= this.acceleration[0];
        input[1] -= this.acceleration[1];
        input[2] -= this.acceleration[2];

        return input;
    }
}