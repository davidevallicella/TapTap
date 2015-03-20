package com.cellasoft.taptap.listener;

/**
 * Interface for double tap gesture.
 */
public interface OnTapListener {

    /**
     * Called when single tap gesture is detected.
     */
    void onTap();

    /**
     * Called when double tap gesture is detected.
     */
    void onDoubleTap();
}