package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.ui.activities.SplashActivity;

import java.util.Locale;

public class LanguageUtils {

    public static void loadAppLanguage(Context context) {
        AppPreferences appPreferences = new AppPreferences(context);

        // Check if language preference is set, if not set default to Kinyarwanda
        if (!appPreferences.isLanguageSet()) {
            appPreferences.setLanguage("rw");
        }

        String language = appPreferences.getLanguage();
        if (language != null && !language.isEmpty()) {
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(language);
            AppCompatDelegate.setApplicationLocales(locales);
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Resources resources = context.getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            try {
                resources.updateConfiguration(config, resources.getDisplayMetrics());
            } catch (UnsupportedOperationException e) {
                // Some devices throw on updateConfiguration; AppCompatDelegate
                // already handled the locale change, so this is non-fatal.
                android.util.Log.w("LanguageUtils", "updateConfiguration not supported", e);
            }
        }
    }

    public static void showLanguageDialog(Context context) {
        final String[] languages = {"English", "Français", "Kinyarwanda"};
        final String[] codes = {"en", "fr", "rw"};
        AppPreferences appPreferences = new AppPreferences(context);

        String currentCode = appPreferences.getLanguage();
        int checkedItem = 2;

        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentCode)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.change_language))
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedCode = codes[which];
                    if (!selectedCode.equals(currentCode)) {
                        appPreferences.setLanguage(selectedCode);

                        Intent intent = new Intent(context, SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        ((Activity) context).finish();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
}
