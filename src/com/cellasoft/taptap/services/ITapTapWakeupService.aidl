package com.cellasoft.taptap.services;

interface ITapTapWakeupService {
    void setCallback( in IBinder binder );
    void removeCallback();
    void stop();
    boolean isRunning();
    int getState();
}
