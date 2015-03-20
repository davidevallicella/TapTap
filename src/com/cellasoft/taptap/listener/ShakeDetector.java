package com.cellasoft.taptap.listener;

/**
 * Class that detects shake gesture.
 */
public class ShakeDetector extends GestureDetector {
    static final String LOG_TAG = "ShakeDetector";

    /**
     * Minimum movement force to consider.
     */
    private static final int MIN_FORCE = 10;

    /**
     * Minimum times in a shake gesture that the direction of movement needs to
     * change.
     */
    private static final int MIN_DIRECTION_CHANGE = 3;

    /**
     * Maximum pause between movements.
     */
    private static final int MAX_PAUSE_BETHWEEN_DIRECTION_CHANGE = 200;

    /**
     * Maximum allowed time for shake gesture.
     */
    private static final int MAX_TOTAL_DURATION_OF_SHAKE = 400;

    /**
     * Time when the gesture started.
     */
    private long mFirstDirectionChangeTime = 0;

    /**
     * Time when the last movement started.
     */
    private long mLastDirectionChangeTime;

    /**
     * How many movements are considered so far.
     */
    private int mDirectionChangeCount = 0;

    /**
     * The last x position.
     */
    private double lastX = 0;

    /**
     * The last y position.
     */
    private double lastY = 0;

    /**
     * The last z position.
     */
    private double lastZ = 0;

    /**
     * OnShakeListener that is called when shake is detected.
     */
    private OnShakeListener mShakeListener;

    public void setOnShakeListener(OnShakeListener listener) {
        mShakeListener = listener;
    }

    @Override
    public void processMeasuring(long timeStamp, int sensorType, double values[]) {
        // get sensor data
        double x = values[DATA_X];
        double y = values[DATA_Y];
        double z = values[DATA_Z];

        // calculate movement
        double totalMovement = Math.abs(x + y + z - lastX - lastY - lastZ);

        if (totalMovement > MIN_FORCE) {

            // store first movement time
            if (mFirstDirectionChangeTime == 0) {
                mFirstDirectionChangeTime = timeStamp;
                mLastDirectionChangeTime = timeStamp;
            }

            // check if the last movement was not long ago
            long lastChangeWasAgo = timeStamp - mLastDirectionChangeTime;
            if (lastChangeWasAgo < MAX_PAUSE_BETHWEEN_DIRECTION_CHANGE) {

                // store movement data
                mLastDirectionChangeTime = timeStamp;
                mDirectionChangeCount++;

                // store last sensor data
                lastX = x;
                lastY = y;
                lastZ = z;

                // check how many movements are so far
                if (mDirectionChangeCount >= MIN_DIRECTION_CHANGE) {

                    // check total duration
                    long totalDuration = timeStamp - mFirstDirectionChangeTime;
                    if (totalDuration < MAX_TOTAL_DURATION_OF_SHAKE) {
                        mShakeListener.onShake();
                        resetShakeParameters();
                    }
                }

            } else {
                resetShakeParameters();
            }
        }
    }

    /**
     * Resets the shake parameters to their default values.
     */
    private void resetShakeParameters() {
        mFirstDirectionChangeTime = 0;
        mDirectionChangeCount = 0;
        mLastDirectionChangeTime = 0;
        lastX = 0;
        lastY = 0;
        lastZ = 0;
    }
}