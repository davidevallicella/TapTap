package com.cellasoft.taptap.listener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import com.cellasoft.taptap.utils.Utils;

import java.util.Date;

public class GestureListener extends GestureDetector implements SensorEventListener, OnTapListener, OnShakeListener, OnInPocketListener {
    public static final int    CALIBRATING_LIMIT      = 3000;
    public static final double ACCEL_DEVIATION_LIMIT  = 0.05;
    public static final int    ACCEL_DEVIATION_LENGTH = 5;
    public static final long   DIFF_UPDATE_TIMEOUT    = 3L;
    static final        String LOG_TAG                = "SENSORS";
    /**
     * OnShakeListener that is called when shake is detected.
     */
    private OnGestureListener mGestureListener;
    private ShakeDetector     sd;
    private TapDetector       td;
    private InPocketDetector  pd;
    private int               counter;
    private double simulatedGravity[] = new double[3];
    private long   previousTimeStamp;
    private long   diffTimeStamp;
    private int    calibratingLimit;
    private int    calibratingAccelCounter;
    private double gravityAccelLen;
    private double gravityAccelHighLimit;
    private double gravityAccelLowLimit;
    private int    gravityAccelLimitLen;
    private int    state;
    private int     shakeCount = 0;
    private boolean isInPocket = false;

    public GestureListener() {
        sd = new ShakeDetector();
        td = new TapDetector();
        pd = new InPocketDetector();
        sd.setOnShakeListener(this);
        td.setOnTapListener(this);
        pd.setOnInPocketListener(this);

        init();
    }

    private void init() {
        previousTimeStamp = -1L;
        // Values related to calibrating state
        calibratingAccelCounter = 0;
        calibratingLimit = CALIBRATING_LIMIT;
        simulatedGravity[DATA_X] = 0.0;
        simulatedGravity[DATA_Y] = 0.0;
        simulatedGravity[DATA_Z] = 0.0;
        diffTimeStamp = -1L;
        gravityAccelLimitLen = -1;
        setState(ENGINESTATES_CALIBRATING);
    }

    public void setOnGestureListener(OnGestureListener listener) {
        mGestureListener = listener;
    }

    private void process(SensorEvent sensorEvent) {
        double values[] = new double[3];
        values[DATA_X] = sensorEvent.values[DATA_X];
        values[DATA_Y] = sensorEvent.values[DATA_Y];
        values[DATA_Z] = sensorEvent.values[DATA_Z];

        if (values.length < 3) {
            return;
        }

        updateCounter();

        switch (state) {
            case ENGINESTATES_CALIBRATING:
                processCalibrating(sensorEvent.timestamp, sensorEvent.sensor.getType(), values);
                break;
            case ENGINESTATES_MEASURING:
                processMeasuring(sensorEvent.timestamp, sensorEvent.sensor.getType(), values);
                break;
            default:
        }
    }

    private void processCalibrating(long timeStamp, int sensorType, double values[]) {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            simulatedGravity[DATA_X] += values[DATA_X];
            simulatedGravity[DATA_Y] += values[DATA_Y];
            simulatedGravity[DATA_Z] += values[DATA_Z];
            ++calibratingAccelCounter;
        }
        if (sensorType == Sensor.TYPE_GYROSCOPE)
            previousTimeStamp = timeStamp;
        if (counter >= calibratingLimit) {
            if (calibratingAccelCounter == 0) { // This shouldn't happen
                calibratingLimit += CALIBRATING_LIMIT;
                Log.d(LOG_TAG, "Increasing calibrating limit to " + calibratingLimit);
            } else {
                double avgDiv = (double) calibratingAccelCounter;
                simulatedGravity[DATA_X] /= avgDiv;
                simulatedGravity[DATA_Y] /= avgDiv;
                simulatedGravity[DATA_Z] /= avgDiv;
                gravityAccelLen = Math.sqrt(
                        simulatedGravity[DATA_X] * simulatedGravity[DATA_X] +
                                simulatedGravity[DATA_Y] * simulatedGravity[DATA_Y] +
                                simulatedGravity[DATA_Z] * simulatedGravity[DATA_Z]);
                gravityAccelHighLimit = gravityAccelLen * (1.0 + ACCEL_DEVIATION_LIMIT);
                gravityAccelLowLimit = gravityAccelLen * (1.0 - ACCEL_DEVIATION_LIMIT);
                Log.d(LOG_TAG,
                        "Simulated gravity vector: x: " + simulatedGravity[DATA_X] +
                                "; y: " + simulatedGravity[DATA_Y] +
                                "; z: " + simulatedGravity[DATA_Z] +
                                "; len: " + gravityAccelLen);
                setState(ENGINESTATES_MEASURING);
            }
        }
    }

    @Override
    public void processMeasuring(long timeStamp, int sensorType, double values[]) {

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                double magnitude = Math.sqrt(values[DATA_X] * values[DATA_X] + values[DATA_Y] * values[DATA_Y] + values[DATA_Z] * values[DATA_Z]);
                if ((magnitude < gravityAccelHighLimit) && (magnitude > gravityAccelLowLimit)) {
                    if (gravityAccelLimitLen < 0) {
                        gravityAccelLimitLen = ACCEL_DEVIATION_LENGTH;
                    }

                    --gravityAccelLimitLen;

                    if (gravityAccelLimitLen <= 0) {
                        gravityAccelLimitLen = 0;
                        simulatedGravity[DATA_X] = values[DATA_X];
                        simulatedGravity[DATA_Y] = values[DATA_Y];
                        simulatedGravity[DATA_Z] = values[DATA_Z];
                    }
                } else {
                    gravityAccelLimitLen = -1;
                }

                sd.processMeasuring(timeStamp, sensorType, values);

                if (shakeCount < 2 && !isInPocket) {
                    // (strappo)la derivata dell'accelerazione rispetto al tempo;
                    // indica la variazione dell'accelerazione nel tempo
                    double[] jerk = Utils.vecdiff(values, simulatedGravity);
                    td.processMeasuring(timeStamp, sensorType, jerk);
                } else if (shakeCount > 0) {
                    --shakeCount;
                }

                break;
            case Sensor.TYPE_GYROSCOPE:
                if (previousTimeStamp >= 0L) {
                    double dt = (double) (timeStamp - previousTimeStamp) / 1000000000.0;
                    double dx = Utils.gyroNoiseLimiter(values[DATA_X] * dt);
                    double dy = Utils.gyroNoiseLimiter(values[DATA_Y] * dt);
                    double dz = Utils.gyroNoiseLimiter(values[DATA_Z] * dt);
                    Utils.rotx(simulatedGravity, -dx);
                    Utils.roty(simulatedGravity, -dy);
                    Utils.rotz(simulatedGravity, -dz);
                }
                previousTimeStamp = timeStamp;
                break;
            case Sensor.TYPE_PROXIMITY:
                isInPocket = false;
                pd.processMeasuring(timeStamp, sensorType, values);
                break;
        }
    }

    public void setState(int newState) {
        state = newState;
    }

    private void updateCounter() {
        ++counter;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // set event timestamp to current time in milliseconds
        sensorEvent.timestamp = (new Date()).getTime() + (sensorEvent.timestamp - System.nanoTime()) / 1000000L;
        process(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onShake() {
        shakeCount++;
        mGestureListener.onShake();
    }

    @Override
    public void onTap() {
        mGestureListener.onTap();
    }

    @Override
    public void onDoubleTap() {
        mGestureListener.onDoubleTap();
    }

    @Override
    public void onInPocket() {
        isInPocket = true;
        mGestureListener.onInPocket();
    }
}
