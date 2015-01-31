package com.cellasoft.taptap;

public class DetectTap {
    // Accelerometer data state
    static class PulseState {
        public static final int NonActive = 0,
                PositivePulse = 1,
                NegativePulse = 2,
                UnknownPulse = -1;
    }

    // Threshold on x, y, z axes for identifying whether a tap triggers the
// accelerometer data change exceeds a threshold or not.  
    private static double deltax_threshold = 0.01;
    private static double deltay_threshold = 0.02;
    private static double deltaz_threshold = 0.03;

    // Declare the average x, y, z accelerometer data when device is static. 
    public double x_initial;
    public double y_initial;
    public double z_initial;

    // Declare the number of samples to calibrate the x_initial, y_intial, z_initial. 
    public int samplecounter_calibrate;
    // Declare the maximum number of samples for calibration. 
    public int MaxCalibrateInterval = 10;
    // Declare the previous state, current state of x axis accelerometer data. 
    private static int PreviousState_X, CurrentState_X;
    private double DeltaxPeak;

    // Ininitialization
    public DetectTap() {
        samplecounter_calibrate = 0;
    }

    // Accelerometer calibration for the NonActive state.
    public int CalibrateInitialReading(double x, double y, double z) {
        int done = 0;

        // Initialize the variables.
        if (samplecounter_calibrate == 0) {
            x_initial = 0;
            y_initial = 0;
            z_initial = 0;
        }

        // Increment the sample number of calibration.
        samplecounter_calibrate++;

        // Skip the first 5 samples and then average the rest samplings of
        // accelerometer data. The skipping is to skip the accelerometer data
        // change due to the button press for calibration.
        if (samplecounter_calibrate > 5
                && samplecounter_calibrate <= MaxCalibrateInterval) {
            x_initial = (x_initial * (samplecounter_calibrate - 6) + x) /
                    (samplecounter_calibrate - 5);
            y_initial = (y_initial * (samplecounter_calibrate - 6) + y) /
                    (samplecounter_calibrate - 5);
            z_initial = (z_initial * (samplecounter_calibrate - 6) + z) /
                    (samplecounter_calibrate - 5);
        }

        if (samplecounter_calibrate >= MaxCalibrateInterval) {
            done = 1;
        }

        return done;
    }

    // State machine to detect the pulse on x axis accelerometer data.
    public int DetectXPulse(double x) {
        double deltax;
        deltax = x - x_initial;


        if (Math.abs(deltax) < deltax_threshold) {
            CurrentState_X = PulseState.NonActive;
        } else {

            if (Math.abs(deltax) > Math.abs(DeltaxPeak))
                DeltaxPeak = deltax;

            switch (PreviousState_X) {
                case PulseState.PositivePulse:
                    if (deltax > 0)
                        CurrentState_X = PulseState.PositivePulse;
                    else
                        CurrentState_X = PulseState.NegativePulse;
                    break;

                case PulseState.NegativePulse:
                    if (deltax > 0)
                        CurrentState_X = PulseState.PositivePulse;
                    else
                        CurrentState_X = PulseState.NegativePulse;
                    break;

                case PulseState.NonActive:
                    if (deltax > 0)
                        CurrentState_X = PulseState.PositivePulse;
                    else
                        CurrentState_X = PulseState.NegativePulse;
                    break;
                default:
                    break;
            }

        }

        PreviousState_X = CurrentState_X;

        return CurrentState_X;
    }

}