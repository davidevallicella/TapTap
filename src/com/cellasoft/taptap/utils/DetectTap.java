package com.cellasoft.taptap.utils;

import android.util.Log;

public class DetectTap {
    // Accelerometer data state
    static class PulseState {
        public static final int
                Active = 0,
                NonActive = 1,
                PositivePulse = 2,
                NegativePulse = 3,
                UnknownPulse = -1;
    }

    static class TapState {
        public static final int
                NoTap = 0,
                SingleTap = 1,
                DoubleTap = 2;
    }

    // Declare the previous state, current state of x axis accelerometer data. 
    private static int CurrentState_X;

    static final long PULSE_LTCY = 10L;
    static final long PULSE_TMLT = 30L;
    static final long PULSE_THSZ = 2;
    long t_initial;
    /*
    *
    PULSE_THSZ﹕ -25.164306606064294  t: 32464502445410
    02-01 22:53:13.867  17942-17942/com.cellasoft.taptap D/----﹕ x: 2.18 y: -1.154 z: 7.293
    02-01 22:53:13.867  17942-17942/com.cellasoft.taptap D/PULSE_THSZ﹕ 7.279592120602066  t:  32464507591503
    02-01 22:53:13.873  17942-17942/com.cellasoft.taptap D/----﹕ x: 0.424 y: 0.205 z: -5.163
    02-01 22:53:13.873  17942-17942/com.cellasoft.taptap D/PULSE_THSZ﹕ -5.167223227615324  t: 32464513207233
    02-01 22:53:13.879  17942-17942/com.cellasoft.taptap D/----﹕ x: -0.771 y: -0.217 z: -0.298
    02-01 22:53:13.885  17942-17942/com.cellasoft.taptap D/----﹕ x: -0.176 y: -0.478 z: 2.257
    02-01 22:53:13.885  17942-17942/com.cellasoft.taptap D/PULSE_THSZ﹕ 2.259978165469478  t:  32464525143378
    *
    *
    * * * * * * */
    long pt;
    int tap = 0;

    // State machine to detect the pulse on x axis accelerometer data.
    public int detectSinglePulse(double deltax) {

        long t = System.currentTimeMillis();

        switch (CurrentState_X) {
            case PulseState.PositivePulse:
            case PulseState.NegativePulse:
                if ((t - pt) > PULSE_LTCY) {
                    Log.d("DEBUG", "Non active --->");
                    CurrentState_X = PulseState.NonActive;
                }
                break;
            case PulseState.Active:
                long elapse = t - t_initial;

                if (Math.abs(deltax) < PULSE_THSZ) {
                    if (elapse <= PULSE_TMLT) {
                        pt = t;
                        if (deltax > 0)
                            CurrentState_X = PulseState.PositivePulse;
                        else
                            CurrentState_X = PulseState.NegativePulse;
                    } else {
                        CurrentState_X = PulseState.NonActive; // Pulse Time Limit expires
                    }
                } else if (elapse > PULSE_TMLT) {
                    CurrentState_X = PulseState.NonActive;
                }
                break;
            case PulseState.NonActive:
                if (Math.abs(deltax) >= PULSE_THSZ) {
                    t_initial = System.currentTimeMillis();
                    CurrentState_X = PulseState.Active;
                }
                break;
        }

        return CurrentState_X;
    }

    long dt_initial;
    long PULSE_WIND = 300L;

    public boolean detectDoublePulse(double x) {
        long t = System.currentTimeMillis();

        if (tap == TapState.DoubleTap) {
            tap = TapState.NoTap;
        }

        switch (detectSinglePulse(x)) {
            case PulseState.PositivePulse:
                switch (tap) {
                    case TapState.NoTap:
                        Log.d("DEBUG", "TAP");
                        dt_initial = t;
                        tap = TapState.SingleTap;
                        break;
                    case TapState.SingleTap:
                        long elapse = t - dt_initial;
                        if (elapse > 20) {
                            if (elapse > PULSE_WIND) {
                                Log.d("DEBUG", "RESET");
                                tap = TapState.NoTap;
                                CurrentState_X = PulseState.NonActive;
                            } else {
                                tap = TapState.DoubleTap;
                            }
                        } else {
                            CurrentState_X = PulseState.NonActive;
                        }
                        break;
                    case TapState.DoubleTap:
                        tap = TapState.NoTap;
                        break;
                }
                break;
            case PulseState.NonActive:
                switch (tap) {
                    case TapState.NoTap:
                        //do nothing
                        break;
                    case TapState.SingleTap:
                        long elapse = t - dt_initial;
                        if (elapse > PULSE_WIND) {
                            Log.d("DEBUG", "RESET");
                            tap = TapState.NoTap;
                            CurrentState_X = PulseState.NonActive;
                        }
                        break;
                    case TapState.DoubleTap:
                        tap = TapState.NoTap;
                        break;
                }
                break;
        }

        return tap == TapState.DoubleTap;
    }
}