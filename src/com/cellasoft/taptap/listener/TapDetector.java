package com.cellasoft.taptap.listener;

import android.util.Log;
import com.cellasoft.taptap.utils.Utils;

/**
 * Class that detects tap gesture.
 */
public class TapDetector extends GestureDetector {
    static final         String LOG_TAG             = "TapDetector";
    // Set the Pulse window to 300 ms
    // 300 ms/20 ms = 15 counts
    private static final long   WINDOW_TIMER        = 300L;
    private static final long   LATENCY_TIMER       = 100L;
    private static final long   DIFF_UPDATE_TIMEOUT = 3L;
    private static final long   PI_LOWER_THRESH     = 10L;
    private static final long   PI_HIGHER_THRESH    = 40L;
    private static final long   Z_THRESH            = 3L;
    private static final long   X_THRESH            = 2L;
    private static final long   Y_THRESH            = 2L;

    private long diffTimeStamp;

    /**
     * Time when the tap started.
     */
    private long mFirstTapTime;

    /**
     * How many taps are considered so far.
     */
    private int mTapCount;


    private boolean delayFastest;

    /**
     * OnTapListener that is called when tap or double tap is detected.
     */
    private OnTapListener mTapListener;

    public void setOnTapListener(OnTapListener listener) {
        mTapListener = listener;
    }

    @Override
    public void processMeasuring(long timeStamp, int sensorType, double jerk[]) {

        // rate of change of acceleration
        double x = Math.abs(jerk[DATA_X]);
        double y = Math.abs(jerk[DATA_Y]);
        double z = Math.abs(jerk[DATA_Z]);

        // total change
        double pi = x + y + z;

        // check if the last movement was not long ago
        long lastChangeWasAgo = timeStamp - mFirstTapTime;

        if (mFirstTapTime > 0) {
            if (lastChangeWasAgo > WINDOW_TIMER) {
                resetTapParameters();
            } else {
                delayFastest = lastChangeWasAgo >= LATENCY_TIMER;
            }
        }

        if (delayFastest || (diffTimeStamp < 0L) || (timeStamp - diffTimeStamp > DIFF_UPDATE_TIMEOUT)) {
            diffTimeStamp = timeStamp;

            // check if pi is between valid range for tap detection
            boolean isValidThreshRange = Utils.isBetween(pi, PI_LOWER_THRESH, PI_HIGHER_THRESH);
            // check if z axis is the highest
            boolean isZAxisHighest = z > 3 && z > x && z > y;

            if (isValidThreshRange && isZAxisHighest) {
                mTapCount++;
                // store first tap time
                if (mTapCount == 1) {
                    mFirstTapTime = timeStamp;
                    mTapListener.onTap();
                } else if (mTapCount <= 3) {
                    // check if elapse time is between valid range for double tap detection
                    boolean isValidTimeRange = Utils.isBetween(lastChangeWasAgo, LATENCY_TIMER, WINDOW_TIMER);

                    if (isValidTimeRange) {
                        mTapListener.onDoubleTap();
                        resetTapParameters();
                    }
                }
            }
        }
    }

    /**
     * Resets the tap parameters to their default values.
     */
    private void resetTapParameters() {
        Log.d(LOG_TAG, "resetTapParameters");

        mTapCount = 0;
        mFirstTapTime = 0;
        delayFastest = false;
    }

}
