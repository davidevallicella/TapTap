package com.cellasoft.taptap.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.os.IBinder;
import android.util.Log;
import com.cellasoft.taptap.listener.GestureListener;
import com.cellasoft.taptap.listener.OnGestureListener;
import com.cellasoft.taptap.manager.DeviceManager;
import com.cellasoft.taptap.receiver.OnAllarmScreenReceiver;

public class TapTapWakeupService extends Service implements OnGestureListener {
    static final String LOG_TAG = "SENSORS";
    Sensor accelSensor;
    Sensor gyroSensor;
    private DeviceManager          dm;
    private GestureListener        gl;
    private OnAllarmScreenReceiver receiver;
    private boolean isStarted = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");
        dm = DeviceManager.getInstance(getApplicationContext());
        gl = new GestureListener();
        stop();        // just in case the activity-level service management fails
        start();

        receiver = new OnAllarmScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(receiver, filter);

        Log.d(LOG_TAG, "onStartCommand ends");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        stop();
        unregisterReceiver(receiver);
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
            dm.unregisterListener(gl);
        }

        isStarted = false;
        gl.setState(gl.ENGINESTATES_IDLE);
    }

    /**
     * Attiva l'ascolto dei sensori.
     */
    private void start() {
        if (isStarted) {
            return;
        }

        accelSensor = dm.getAccelSensor();
        gyroSensor = dm.getGyroSensor();
        Sensor proxSensor = dm.getProxSensor();

        gl.setOnGestureListener(this);
        gl.setState(gl.ENGINESTATES_CALIBRATING);

        if ((accelSensor != null) && (gyroSensor != null)) {
            Log.d(LOG_TAG, "registerListener/SensorService");
            dm.registerListener(gl, accelSensor);
            dm.registerListener(gl, proxSensor);
            //dm.registerListener(this, gyroSensor);
        } else {
            Log.d(LOG_TAG, "Sensore(i) assenti: accelerometro: " + accelSensor + "; gyroscopio: " + gyroSensor);
        }
        isStarted = true;
    }

    @Override
    public void onShake() {
        Log.d(LOG_TAG, "--------------SHAkE--------------");
    }

    @Override
    public void onTap() {
        Log.d(LOG_TAG, "--------------TAP--------------");
    }

    @Override
    public void onDoubleTap() {
        Log.d(LOG_TAG, "--------------DOUBLE TAP--------------");
        dm.turnScreenON(1);
    }

    @Override
    public void onInPocket() {
        Log.d(LOG_TAG, "--------------IN POCKET--------------");
        //dm.unregisterListener(gl,accelSensor);
    }
}
