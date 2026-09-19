package com.drivingschoolrwandaapp.utils;

import android.os.Build;
import android.util.Log;
import android.view.Window;

import androidx.annotation.NonNull;

/**
 * Enables edge-to-edge display without any of the Android 15-deprecated window
 * APIs that Play Console flags (Window.setStatusBarColor,
 * Window.setNavigationBarColor, LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES).
 *
 * <p>Two earlier approaches were rejected for that reason:
 *
 * <ul>
 *   <li>{@code androidx.activity.EdgeToEdge.enable()} — its Api23/26/29/35 impls
 *       call setStatusBarColor/setNavigationBarColor and its Api28/30 impls set
 *       layoutInDisplayCutoutMode; Play attributes the calls to every activity
 *       constructor that reaches it (Hilt_AdminActivity.&lt;init&gt;, o0.s, etc.).</li>
 *   <li>{@code WindowCompat.setDecorFitsSystemWindows()} — androidx.core 1.19.0's
 *       implementation still calls the deprecated color setters.</li>
 * </ul>
 *
 * <p>Instead we call {@link Window#setDecorFitsSystemWindows(boolean)} directly
 * (public API since API 30, never deprecated) and let Material 3 handle
 * background colors via the theme, which Play accepts.
 *
 * <p>On Android 9-10 (API 28-29) some OEM skins (Infinix, TECNO, etc.) throw
 * {@link UnsupportedOperationException} from inside {@code getDecorView()} for
 * cutout modes newer than their firmware supports, so edge-to-edge is skipped
 * below API 30 — the default translucent bars are acceptable there.
 */
public final class EdgeToEdgeUtils {

    private static final String TAG = "EdgeToEdgeUtils";

    private EdgeToEdgeUtils() {}

    public static void enable(@NonNull Window window) {
        // windowLayoutInDisplayCutoutMode and setDecorFitsSystemWindows are
        // API 30+; older OEM firmwares (TECNO/Infinix on API 28-29) crash on
        // newer cutout modes, so stay on the default translucent bars there.
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        try {
            // The non-deprecated way to draw behind system bars. Background
            // colors behind the bars come from the Material 3 theme.
            window.setDecorFitsSystemWindows(false);
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Edge-to-edge not supported on this device", e);
        }
    }
}
