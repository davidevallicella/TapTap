package com.cellasoft.taptap.manager;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;

import java.util.List;

/**
 * Created by netsysco on 14/02/2015.
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
        releaseScreen();
        acquireScreen(timeout);
    }

    public void turnScreenOFF() {
        releaseScreen();
        dpm.lockNow();
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
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void unregisterListener(SensorEventListener listener) {
        sm.unregisterListener(listener);
    }

    public Sensor getAccelSensor() {
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        return sensors.size() == 0 ? null : sensors.get(0);
    }

    public Sensor getGyroSensor() {
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_GYROSCOPE);
        return sensors.size() == 0 ? null : sensors.get(0);
    }

}
