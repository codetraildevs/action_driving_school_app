package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.drivingschoolrwandaapp.BuildConfig;
import com.drivingschoolrwandaapp.R;

/**
 * Shared "About us" dialog (app icon, name, version, about text) — used by
 * both the user app's drawer and the admin console's Settings tab so the
 * About/version experience is identical everywhere.
 */
public final class AboutUtils {

    private AboutUtils() {
    }

    public static void showAboutDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_about_us, null);

        TextView versionTv = dialogView.findViewById(R.id.versionTv);
        versionTv.setText(activity.getString(R.string.version_format,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnClose = dialogView.findViewById(R.id.btn_close_about);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
