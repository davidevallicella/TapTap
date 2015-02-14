package com.cellasoft.taptap.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.cellasoft.taptap.manager.DeviceManager;

public class OnAllarmScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Waking up mobile if it is sleeping
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            DeviceManager.getInstance(context).acquireCPU();
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            DeviceManager.getInstance(context).releaseCPU();
        }
    }
}