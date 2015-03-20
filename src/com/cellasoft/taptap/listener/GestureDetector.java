package com.cellasoft.taptap.listener;

/**
 * Created by netsysco on 01/03/2015.
 */
public abstract class GestureDetector {
    public static final int ENGINESTATES_IDLE        = 0;
    public static final int ENGINESTATES_CALIBRATING = 1;
    public static final int ENGINESTATES_MEASURING   = 2;

    public static final int DATA_X = 0;
    public static final int DATA_Y = 1;
    public static final int DATA_Z = 2;

    abstract void processMeasuring(long timeStamp, int sensorType, double values[]);
}
