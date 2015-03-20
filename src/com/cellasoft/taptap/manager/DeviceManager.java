package com.cellasoft.taptap.manager;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;

import java.util.List;

/**
 * Created by Davide Vallicella on 14/02/2015.
 * <p/>
 * <p/>
 * Flag Value                CPU        Screen               Keyboard
 * PARTIAL_WAKE_LOCK          On*        Off                   Off
 * <p/>
 * SCREEN_DIM_WAKE_LOCK       On         Dim                   Off
 * <p/>
 * SCREEN_BRIGHT_WAKE_LOCK    On         Bright                Off
 * <p/>
 * FULL_WAKE_LOCK             On         Bright                Bright
 * <p/>
 * ACQUIRE_CAUSES_WAKEUP      NoEffect   turn On immediately   NoEffect
 */
public class DeviceManager {
    private static DeviceManager dm;
    private static PowerManager.WakeLock cpuLook;
    private static PowerManager.WakeLock screenLook;
    private PowerManager pm;
    private DevicePolicyManager dpm;
    private SensorManager sm;

    private DeviceManager(Context context) {
        this.pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public static DeviceManager getInstance(Context context) {
        if (dm == null) {
            dm = new DeviceManager(context);
        }
        return dm;
    }

    public boolean isScreenOn() {
        return pm.isScreenOn();
    }

    public void turnScreenON(long timeout) {
        if (isScreenOn()) {
            return;
        }
        releaseScreen();
        acquireScreen(timeout);
    }

    public void turnScreenOFF() {
        if (isScreenOn()) {
            releaseScreen();
            dpm.lockNow();
        }
    }

    private void acquireScreen(long timeout) {
        // Create a bright wake lock
        screenLook = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screenLock");
        screenLook.acquire(timeout);
    }

    private void releaseScreen() {
        if (screenLook != null && screenLook.isHeld()) screenLook.release();
        screenLook = null;
    }

    public void acquireCPU() {
        // Create a bright wake lock
        cpuLook = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpuLock");
        cpuLook.acquire();
    }

    public void releaseCPU() {
        if (cpuLook != null && cpuLook.isHeld()) cpuLook.release();
        cpuLook = null;
    }

    public void registerListener(SensorEventListener listener, Sensor sensor) {
        if (listener == null) {
            return;
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterListener(SensorEventListener listener) {
        if (listener == null) {
            return;
        }
        sm.unregisterListener(listener);
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor) {
        if (listener == null) {
            return;
        }
        sm.unregisterListener(listener, sensor);
    }

    public Sensor getAccelSensor() {
        return getSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public Sensor getGyroSensor() {
        return getSensor(Sensor.TYPE_GYROSCOPE);
    }

    public Sensor getProxSensor() {
        return getSensor(Sensor.TYPE_PROXIMITY);
    }


    private Sensor getSensor(int type) {
        List<Sensor> sensors = sm.getSensorList(type);
        return sensors.size() == 0 ? null : sensors.get(0);
    }

}
