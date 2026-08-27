package com.drivingschoolrwandaapp.utils;

import android.os.Build;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;

/**
 * Safely enables edge-to-edge display.
 *
 * <p>On Android 9-10 (API 28-29) some OEM skins (Infinix, TECNO, etc.) do not
 * support {@code LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES} (mode 3) and throw
 * {@link UnsupportedOperationException} from inside
 * {@code PhoneWindow.generateLayout}. The exception propagates through
 * {@code getDecorView()} which is called by {@code EdgeToEdge.enable()}, so it
 * cannot be reliably caught at the call site.
 *
 * <p>To prevent the crash we skip {@code EdgeToEdge.enable()} entirely on
 * API < 30 and fall back to a simple translucent status/navigation bar.
 */
public final class EdgeToEdgeUtils {

    private static final String TAG = "EdgeToEdgeUtils";

    private EdgeToEdgeUtils() {}

    public static void enable(ComponentActivity activity) {
        // LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES (value 3) was introduced
        // in API 30. Older devices crash with UnsupportedOperationException.
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                EdgeToEdge.enable(activity);
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "EdgeToEdge not supported on this device", e);
            }
        }
        // On API < 30, skip EdgeToEdge entirely — the default translucent bars
        // are acceptable and avoid the crash on broken OEM firmwares.
    }
}
