package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;

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

    public static AlertDialog showLanguageDialog(Context context) {
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

        return new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.change_language))
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedCode = codes[which];
                    // Close the old-language dialog before AppCompat recreates its host.
                    dialog.dismiss();
                    if (!selectedCode.equals(currentCode)) {
                        appPreferences.setLanguage(selectedCode);
                        // Avoid an OEM activity transition exposing a black frame
                        // while the localized activity is recreated.
                        Activity host = findActivity(context);
                        if (host != null) {
                            host.getWindow().setWindowAnimations(0);
                            host.overridePendingTransition(0, 0);
                        }
                        // Configuration recreation preserves the NavHost back stack and
                        // saved view state. Relaunching SplashActivity loses both and
                        // causes an unrelated page transition through startup.
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(selectedCode));
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return null;
    }
}
