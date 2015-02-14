package com.cellasoft.taptap.services;

interface ISensorService {
    void setCallback( in IBinder binder );
    void removeCallback();
    void stop();
    boolean isRunning();
    int getState();
}
