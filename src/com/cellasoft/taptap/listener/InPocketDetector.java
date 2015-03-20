package com.cellasoft.taptap.listener;

/**
 * Created by netsysco on 07/03/2015.
 */
public class InPocketDetector extends GestureDetector {

    /**
     * OnInPocketListener that is called when in-pocket is detected.
     */
    private OnInPocketListener mInPocketListener;

    public void setOnInPocketListener(OnInPocketListener listener) {
        mInPocketListener = listener;
    }

    @Override
    void processMeasuring(long timeStamp, int sensorType, double[] values) {
        if (values[0] == 0) {
            mInPocketListener.onInPocket();
        }
    }
}
