package com.drivingschoolrwandaapp.utils;

import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;

/**
 * Safely enables edge-to-edge display. Some devices (especially older OEM skins)
 * throw UnsupportedOperationException when EdgeToEdge.enable() is called.
 * This wrapper prevents that from crashing the activity.
 */
public final class EdgeToEdgeUtils {

    private static final String TAG = "EdgeToEdgeUtils";

    private EdgeToEdgeUtils() {}

    public static void enable(ComponentActivity activity) {
        try {
            EdgeToEdge.enable(activity);
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "EdgeToEdge not supported on this device", e);
        }
    }
}
