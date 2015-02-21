package com.cellasoft.taptap.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.IBinder;
import android.util.Log;
import com.cellasoft.taptap.manager.DeviceManager;
import com.cellasoft.taptap.receiver.OnAllarmScreenReceiver;
import com.cellasoft.taptap.utils.DetectTap;

/**
 * Created by Davide Vallicella on 02/02/2015.
 */
public class SensorService extends Service implements SensorEventListener {
    static final String LOG_TAG = "SENSORS";
    public static final int ENGINESTATES_IDLE = 0;
    public static final int ENGINESTATES_CALIBRATING = 1;
    public static final int ENGINESTATES_MEASURING = 2;
    public static final int CALIBRATING_LIMIT = 3000;
    public static final double ACCEL_DEVIATION_LIMIT = 0.05;
    public static final int ACCEL_DEVIATION_LENGTH = 5;
    public static final double ACCEL_NOISE_LIMIT = 0.1;
    public static final double GYRO_NOISE_LIMIT = 0.06;
    public long DIFF_UPDATE_TIMEOUT = 3L;
    public static final int SENSORTYPE_NA = 0;
    public static final int SENSORTYPE_ACCEL = 1;
    public static final int SENSORTYPE_GYRO = 0;
    public static final int IDX_X = 0;
    public static final int IDX_Y = 1;
    public static final int IDX_Z = 2;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");
        dm = DeviceManager.getInstance(getApplicationContext());

        stop();        // just in case the activity-level service management fails
        start(true);

        receiver = new OnAllarmScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(receiver, filter);

        Log.d(LOG_TAG, "onStartCommand ends");

        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        stop();
        unregisterReceiver(receiver);
    }

    private void init() {
        previousTimeStamp = -1L;
        // Values related to calibrating state
        calibratingAccelCounter = 0;
        calibratingLimit = CALIBRATING_LIMIT;
        simulatedGravity[IDX_X] = 0.0;
        simulatedGravity[IDX_Y] = 0.0;
        simulatedGravity[IDX_Z] = 0.0;
        diffTimeStamp = -1L;
        gravityAccelLimitLen = -1;
        setState(ENGINESTATES_CALIBRATING);
    }

    /**
     * Disattiva l'ascolto dei sensori.
     */
    private void stop() {
        if (!isStarted) {
            return;
        }

        if (dm != null) {
            Log.d(LOG_TAG, "unregisterListener/SensorService");
            dm.unregisterListener(this);
        }

        isStarted = false;
        setState(ENGINESTATES_IDLE);
    }

    /**
     * Attiva l'ascolto dei sensori.
     */
    private void start(boolean init) {
        if (isStarted) {
            return;
        }

        accelSensor = dm.getAccelSensor();
        gyroSensor = dm.getGyroSensor();

        init();

        if ((accelSensor != null) && (gyroSensor != null)) {
            Log.d(LOG_TAG, "registerListener/SensorService");
            dm.registerListener(this, accelSensor);
            dm.registerListener(this, gyroSensor);
        } else {
            Log.d(LOG_TAG, "Sensore(i) assenti: accelerometro: " + accelSensor + "; gyroscopio: " + gyroSensor);
        }

        isStarted = true;
    }

    private void setState(int newState) {
        state = newState;
    }

    private void updateCounter() {
        ++counter;
    }

    private void process(SensorEvent sensorEvent) {

        float values[] = sensorEvent.values;

        if (values.length < 3) {
            return;
        }


        int sensorType = SENSORTYPE_NA;
        if (sensorEvent.sensor == accelSensor) {
            sensorType = SENSORTYPE_ACCEL;
        } else if (sensorEvent.sensor == gyroSensor) {
            sensorType = SENSORTYPE_GYRO;
        }

        updateCounter();
        switch (state) {
            case ENGINESTATES_CALIBRATING:
                processCalibrating(sensorEvent.timestamp, sensorType, values);
                break;
            case ENGINESTATES_MEASURING:
                processMeasuring(sensorEvent.timestamp, sensorType, values);
                break;
        }
    }

    private void processCalibrating(long timeStamp, int sensorType, float values[]) {
        if (sensorType == SENSORTYPE_ACCEL) {
            simulatedGravity[IDX_X] += (double) values[IDX_X];
            simulatedGravity[IDX_Y] += (double) values[IDX_Y];
            simulatedGravity[IDX_Z] += (double) values[IDX_Z];
            ++calibratingAccelCounter;
        }
        if (sensorType == SENSORTYPE_GYRO)
            previousTimeStamp = timeStamp;
        if (counter >= calibratingLimit) {
            if (calibratingAccelCounter == 0) { // This shouldn't happen
                calibratingLimit += CALIBRATING_LIMIT;
                Log.d(LOG_TAG, "Increasing calibrating limit to " + calibratingLimit);
            } else {
                double avgDiv = (double) calibratingAccelCounter;
                simulatedGravity[IDX_X] /= avgDiv;
                simulatedGravity[IDX_Y] /= avgDiv;
                simulatedGravity[IDX_Z] /= avgDiv;
                gravityAccelLen = Math.sqrt(
                        simulatedGravity[IDX_X] * simulatedGravity[IDX_X] +
                                simulatedGravity[IDX_Y] * simulatedGravity[IDX_Y] +
                                simulatedGravity[IDX_Z] * simulatedGravity[IDX_Z]);
                gravityAccelHighLimit = gravityAccelLen * (1.0 + ACCEL_DEVIATION_LIMIT);
                gravityAccelLowLimit = gravityAccelLen * (1.0 - ACCEL_DEVIATION_LIMIT);
                Log.d(LOG_TAG,
                        "Simulated gravity vector: x: " + simulatedGravity[IDX_X] +
                                "; y: " + simulatedGravity[IDX_Y] +
                                "; z: " + simulatedGravity[IDX_Z] +
                                "; len: " + gravityAccelLen);
                setState(ENGINESTATES_MEASURING);
            }
        }
    }

    long tinitial;
    int tap = 0;
    boolean delayFast = false;

    private void processMeasuring(long timeStamp, int sensorType, float values[]) {
        if (dm.isScreenOn()) {
            return;
        }

        float dv[] = values.clone();

        if (sensorType == SENSORTYPE_ACCEL) {
            double magnitude = Math.sqrt(dv[IDX_X] * dv[IDX_X] + dv[IDX_Y] * dv[IDX_Y] + dv[IDX_Z] * dv[IDX_Z]);
            if ((magnitude < gravityAccelHighLimit) && (magnitude > gravityAccelLowLimit)) {
                if (gravityAccelLimitLen < 0) {
                    gravityAccelLimitLen = ACCEL_DEVIATION_LENGTH;
                }

                --gravityAccelLimitLen;

                if (gravityAccelLimitLen <= 0) {
                    gravityAccelLimitLen = 0;
                    simulatedGravity[IDX_X] = dv[IDX_X];
                    simulatedGravity[IDX_Y] = dv[IDX_Y];
                    simulatedGravity[IDX_Z] = dv[IDX_Z];
                }
            } else {
                gravityAccelLimitLen = -1;
            }

            // (strappo)la derivata dell'accelerazione rispetto al tempo;
            // indica la variazione dell'accelerazione nel tempo
            double[] jerk = vecdiff(dv, simulatedGravity);
            //double[] rotatedDiff = rotateToEarth(jerk);

            double pi = Math.abs(jerk[IDX_X]) + Math.abs(jerk[IDX_Y]) + Math.abs(jerk[IDX_Z]);

            long currentTime = System.currentTimeMillis();

            if (tinitial > 0) {
                long elapse = currentTime - tinitial;

                if (elapse > 255) {
                    Log.d(LOG_TAG, "RESET t: " + (currentTime - tinitial));
                    tap = 0;
                    tinitial = 0;
                    delayFast = false;
                } else if(elapse >= 100){
                    delayFast = elapse >= 100;
                }
            }

            if (delayFast || (diffTimeStamp < 0L) || (currentTime - diffTimeStamp > DIFF_UPDATE_TIMEOUT)) {
                diffTimeStamp = currentTime;

                if (pi > 10) {
                    Log.d(LOG_TAG, "PI " + pi + "\t\tt: " + (currentTime - tinitial) + "\t\t n: " + tap);
                    tap += 1;
                    if (tap == 1) {
                        tinitial = currentTime;
                        Log.d(LOG_TAG, "TAP");
                    } else if (tap < 3) {
                        if ((currentTime - tinitial) > 100 && (currentTime - tinitial) < 255) {
                            Log.d(LOG_TAG, "DOUBLE TAP");

                            if (!dm.isScreenOn()) {
                                dm.turnScreenON(1);
                            }
                        }
                    }
                }
            }

        } else if (sensorType == SENSORTYPE_GYRO) {
            if (previousTimeStamp >= 0L) {
                double dt = (double) (timeStamp - previousTimeStamp) / 1000000000.0;
                double dx = gyroNoiseLimiter(dv[IDX_X] * dt);
                double dy = gyroNoiseLimiter(dv[IDX_Y] * dt);
                double dz = gyroNoiseLimiter(dv[IDX_Z] * dt);
                rotx(simulatedGravity, -dx);
                roty(simulatedGravity, -dy);
                rotz(simulatedGravity, -dz);

            }
            previousTimeStamp = timeStamp;
        }


    }

    private void gestureRecognition(double v[]) {
        boolean isdtx = dtx.detectDoublePulse(v[IDX_X]);
        boolean isdtz = dtz.detectDoublePulse(v[IDX_Z]);

        if (isdtx && isdtz) {
            Log.d("TEST", "DOUBLE TAP X");
            if (dm.isScreenOn()) {
                dm.turnScreenOFF();
            } else {
                dm.turnScreenON(20);
            }
        }
    }

    private double gyroNoiseLimiter(double gyroValue) {
        double v = gyroValue;
        if (Math.abs(v) < GYRO_NOISE_LIMIT)
            v = 0.0;
        return v;
    }

    private void rotz(double vec[], double dz) {
        double x = vec[IDX_X];
        double y = vec[IDX_Y];
        vec[IDX_X] = x * Math.cos(dz) - y * Math.sin(dz);
        vec[IDX_Y] = x * Math.sin(dz) + y * Math.cos(dz);
    }

    private void rotx(double vec[], double dx) {
        double y = vec[IDX_Y];
        double z = vec[IDX_Z];
        vec[IDX_Y] = y * Math.cos(dx) - z * Math.sin(dx);
        vec[IDX_Z] = y * Math.sin(dx) + z * Math.cos(dx);
    }

    private void roty(double vec[], double dy) {
        double x = vec[IDX_X];
        double z = vec[IDX_Z];
        vec[IDX_Z] = z * Math.cos(dy) - x * Math.sin(dy);
        vec[IDX_X] = z * Math.sin(dy) + x * Math.cos(dy);
    }

    private double[] vecdiff(float v1[], double v2[]) {
        double diff[] = new double[3];
        diff[IDX_X] = v1[IDX_X] - v2[IDX_X];
        diff[IDX_Y] = v1[IDX_Y] - v2[IDX_Y];
        diff[IDX_Z] = v1[IDX_Z] - v2[IDX_Z];
        return diff;
    }

    private double fixAtanDegree(double deg, double y, double x) {
        double rdeg = deg;
        if ((x < 0.0) && (y > 0.0))
            rdeg = Math.PI - deg;
        if ((x < 0.0) && (y < 0.0))
            rdeg = Math.PI + deg;
        return rdeg;
    }

    private double[] rotateToEarth(double diff[]) {
        double rotatedDiff[] = new double[3];
        rotatedDiff[IDX_X] = diff[IDX_X];
        rotatedDiff[IDX_Y] = diff[IDX_Y];
        rotatedDiff[IDX_Z] = diff[IDX_Z];
        double gravity[] = new double[3];
        gravity[IDX_X] = simulatedGravity[IDX_X];
        gravity[IDX_Y] = simulatedGravity[IDX_Y];
        gravity[IDX_Z] = simulatedGravity[IDX_Z];
        double dz = Math.atan2(gravity[IDX_Y], gravity[IDX_X]);
        dz = fixAtanDegree(dz, gravity[IDX_Y], gravity[IDX_X]);
        rotz(rotatedDiff, -dz);
        rotz(gravity, -dz);
        double dy = Math.atan2(gravity[IDX_X], gravity[IDX_Z]);
        dy = fixAtanDegree(dy, gravity[IDX_X], gravity[IDX_Z]);
        roty(rotatedDiff, -dy);
        return rotatedDiff;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        process(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private DeviceManager dm;
    private OnAllarmScreenReceiver receiver;
    private DetectTap dtx = new DetectTap(1, "X");
    private DetectTap dty = new DetectTap(1, "Y");
    private DetectTap dtz = new DetectTap(2, "Z");
    private boolean isStarted = false;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private double simulatedGravity[] = new double[3];
    private int counter;
    private long previousTimeStamp;
    private long diffTimeStamp;
    private int calibratingLimit;
    private int calibratingAccelCounter;
    private double gravityAccelLen;
    private double gravityAccelHighLimit;
    private double gravityAccelLowLimit;
    private int gravityAccelLimitLen;
    private int state;
}
