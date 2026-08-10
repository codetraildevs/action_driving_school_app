package com.drivingschoolrwandaapp.ui.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drivingschoolrwandaapp.BuildConfig;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.ui.activities.LoginActivity;
import com.drivingschoolrwandaapp.ui.activities.WebViewActivity;
import com.drivingschoolrwandaapp.utils.AboutUtils;
import com.drivingschoolrwandaapp.utils.LanguageUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminSettingsFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.version_text)).setText(BuildConfig.VERSION_NAME);

        view.findViewById(R.id.open_console_button).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WebViewActivity.class);
            intent.putExtra("url", ApiClient.SITE_URL);
            intent.putExtra("title", getString(R.string.admin_title));
            startActivity(intent);
        });

        // Language + About are shared with the user app — the same picker
        // dialog and the same About dialog, so the experience is identical.
        view.findViewById(R.id.change_language_button).setOnClickListener(v ->
                LanguageUtils.showLanguageDialog(requireContext()));

        view.findViewById(R.id.about_button).setOnClickListener(v -> {
            if (getActivity() != null) {
                AboutUtils.showAboutDialog(getActivity());
            }
        });

        view.findViewById(R.id.logout_button).setOnClickListener(v -> {
            tokenManager.clearTokens();
            // Same destination as every other logout path (Profile tab,
            // UserRepository, token expiry) — the shared login screen.
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
