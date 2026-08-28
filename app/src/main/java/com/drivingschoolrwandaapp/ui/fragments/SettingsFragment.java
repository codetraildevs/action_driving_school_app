package com.drivingschoolrwandaapp.ui.fragments;

import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private AppPreferences appPreferences;
    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appPreferences = new AppPreferences(requireContext());
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        setupDarkModeToggle(view);
        setupLanguageSetting(view);
        setupAppVersion(view);
        setupDeleteAccount(view);
    }

    private void setupDarkModeToggle(View view) {
        RadioGroup darkModeRadioGroup = view.findViewById(R.id.dark_mode_radio_group);
        RadioButton radioFollowSystem = view.findViewById(R.id.radio_follow_system);
        RadioButton radioLight = view.findViewById(R.id.radio_light);
        RadioButton radioDark = view.findViewById(R.id.radio_dark);

        // Set current selection
        int currentMode = appPreferences.getDarkMode();
        switch (currentMode) {
            case 1:
                radioLight.setChecked(true);
                break;
            case 2:
                radioDark.setChecked(true);
                break;
            default:
                radioFollowSystem.setChecked(true);
                break;
        }

        darkModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radio_light) {
                mode = 1;
            } else if (checkedId == R.id.radio_dark) {
                mode = 2;
            } else {
                mode = 0; // Follow system
            }

            appPreferences.setDarkMode(mode);
            applyTheme(mode);

            // Recreate activity to apply theme immediately
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }

    private void applyTheme(int mode) {
        switch (mode) {
            case 1: // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2: // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // Follow system
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void setupLanguageSetting(View view) {
        LinearLayout languageSetting = view.findViewById(R.id.language_setting);
        TextView languageValue = view.findViewById(R.id.language_value);

        // Show current language
        String currentLang = appPreferences.getLanguage();
        switch (currentLang) {
            case "en":
                languageValue.setText(getString(R.string.english));
                break;
            case "fr":
                languageValue.setText(getString(R.string.french));
                break;
            case "rw":
            default:
                languageValue.setText(getString(R.string.kinyarwanda));
                break;
        }

        languageSetting.setOnClickListener(v -> {
            com.drivingschoolrwandaapp.utils.LanguageUtils.showLanguageDialog(requireContext());
        });
    }

    private void setupAppVersion(View view) {
        TextView versionText = view.findViewById(R.id.app_version_text);
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(
                    requireContext().getPackageName(), 0);
            versionText.setText("v" + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("v1.5.7");
        }
    }

    private void setupDeleteAccount(View view) {
        LinearLayout deleteAccountSetting = view.findViewById(R.id.delete_account_setting);
        deleteAccountSetting.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_text))
                .setPositiveButton(getString(R.string.delete_account_delete), (dialog, which) -> {
                    // Allow this device to register again if account is deleted
                    appPreferences.setDeviceRegistered(false);
                    userViewModel.deleteAccount();
                })
                .setNegativeButton(getString(R.string.delete_keep), null)
                .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-apply theme when returning to settings
        if (appPreferences != null) {
            applyTheme(appPreferences.getDarkMode());
        }
    }
}
