package com.cellasoft.taptap.activity;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import com.cellasoft.taptap.R;
import com.cellasoft.taptap.receiver.AdminReceiver;
import com.cellasoft.taptap.services.ITapTapWakeupService;

/**
 * Created by Davide Vallicella on 02/02/2015.
 */
public class MainActivity extends Activity {

    static final String LOG_TAG               = "TAPTAP";
    static final String SERVICE_ACTIVATED_KEY = "isActive";
    static final int    RESULT_ENABLE         = 1;

    private boolean isActive = false;
    private ComponentName compName;
    private boolean                  isServiceRunning = false;
    private ITapTapWakeupService     sensorService    = null;
    private SensorsServiceConnection sensorConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        setContentView(R.layout.main);
        isActive = savedInstanceState != null && savedInstanceState.getBoolean(SERVICE_ACTIVATED_KEY, false);
        Log.d(LOG_TAG, "onCreate; isActive: " + isActive);
        bindService();
        init();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");
        outState.putBoolean(SERVICE_ACTIVATED_KEY, isServiceRunning);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        releaseSensorsService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("DeviceAdmin", "Admin enabled!");
                } else {
                    Log.i("DeviceAdmin", "Admin enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {
        compName = new ComponentName(this, AdminReceiver.class);

        CheckBox cb = (CheckBox) findViewById(R.id.cb_sampling);
        if (isActive) {
            cb.setChecked(true);
        } else {
            stopSensorsService();
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why this needs to be added.");
                        startActivityForResult(intent, RESULT_ENABLE);
                        Log.d(LOG_TAG, "Activated");
                        isActive = true;
                        startSensorsService();
                    } else {
                        Log.d(LOG_TAG, "Deactivated");
                        isActive = false;
                        stopSensorsService();
                    }
                }
            });
        }
    }

    private void bindService() {
        Log.d(LOG_TAG, "bindTapTapWakeupService");
        sensorConnection = new SensorsServiceConnection();
        Intent i = new Intent();
        i.setClassName("com.cellasoft.taptap", "com.cellasoft.taptap.services.TapTapWakeupService");
        bindService(i, sensorConnection, Context.BIND_AUTO_CREATE);
    }

    private void startSensorsService() {
        if (isServiceRunning) {
            stopSensorsService();
        }
        Log.d(LOG_TAG, "startTapTapWakeupService");
        Intent i = new Intent();
        i.setClassName("com.cellasoft.taptap", "com.cellasoft.taptap.services.TapTapWakeupService");
        startService(i);
        isServiceRunning = true;
    }

    private void stopSensorsService() {
        Log.d(LOG_TAG, "stopTapTapWakeupService");
        if (isServiceRunning) {
            if (sensorService == null)
                Log.e(LOG_TAG, "stopSensorService: Service not available!");
            else {
                try {
                    sensorService.stop();
                } catch (DeadObjectException ex) {
                    Log.e(LOG_TAG, "DeadObjectException", ex);
                } catch (RemoteException ex) {
                    Log.e(LOG_TAG, "RemoteException", ex);
                }
            }
            isServiceRunning = false;
        }
    }

    private void releaseSensorsService() {
        //releaseCallbackOnService();
        unbindService(sensorConnection);
        sensorConnection = null;
    }

    private void updateSensorsServiceRunning() {
        if (sensorService == null)
            Log.e(LOG_TAG, "updateTapTapWakeupServiceRunning: Service not available");
        else {
            try {
                isServiceRunning = sensorService.isRunning();
            } catch (DeadObjectException ex) {
                Log.e(LOG_TAG, "DeadObjectException", ex);
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "RemoteException", ex);
            }
        }
    }

    class SensorsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder boundService) {
            Log.d(LOG_TAG, "onServiceConnected");
            sensorService = ITapTapWakeupService.Stub.asInterface(boundService);
            updateSensorsServiceRunning();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(LOG_TAG, "onServiceDisconnected");
            sensorService = null;
        }
    }
}
