package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextInputEditText emailField;
    private MaterialButton loginButton;
    private MaterialCheckBox rememberMeCheckbox;
    private ProgressBar loadingIndicator;
    private TokenManager tokenManager;
    private AppPreferences appPreferences;
    private static final String TAG = "LoginActivity";
    private static final long LOADING_TIMEOUT_MS = 45_000; // 45 seconds
    private Handler loadingTimeoutHandler;
    private boolean isLoading = false;

    @Override
    @SuppressLint("HardwareIds")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        tokenManager = new TokenManager(this);
        appPreferences = new AppPreferences(this);
        loadingTimeoutHandler = new Handler(Looper.getMainLooper());

        // If already logged in, redirect to main app (one account per device)
        if (tokenManager.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, App.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        emailField = findViewById(R.id.email_field);

        // Pre-fill phone number if navigated from RegisterActivity (already registered user)
        String prefillPhone = getIntent().getStringExtra("phone");
        if (prefillPhone != null && !prefillPhone.isEmpty()) {
            emailField.setText(prefillPhone);
            emailField.setSelection(prefillPhone.length());
        }
        loginButton = findViewById(R.id.login_button);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // Restore the saved Remember Me preference
        if (rememberMeCheckbox != null) {
            rememberMeCheckbox.setChecked(appPreferences.isRememberMe());
            rememberMeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                appPreferences.setRememberMe(isChecked)
            );
        }

        Button registerText = findViewById(R.id.register_text);
        if (registerText != null) {
            registerText.setOnClickListener(v -> {
                if (!isFinishing()) {
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                }
            });
        }

        loginButton.setOnClickListener(v -> {
            // Prevent double-clicks: if a login is already in progress, ignore
            if (isLoading) {
                Log.d(TAG, "Login already in progress — ignoring duplicate click");
                return;
            }

            String phone = emailField.getText().toString().trim();
            String password = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = password;

            if (phone.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() < 10) {
                Toast.makeText(this, getString(R.string.invalid_phone_format), Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading state immediately on click — don't wait for the observer
            isLoading = true;
            loadingIndicator.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            loginButton.setText(getString(R.string.loading));

            userViewModel.login(phone, password, deviceId);
        });

        userViewModel.getLoginResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    // Only show loading if we're still expecting a response
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    }
                    if (loginButton != null) {
                        loginButton.setEnabled(false);
                    }
                    // Show a timeout message if loading takes too long
                    loadingTimeoutHandler.postDelayed(() -> {
                        if (isLoading && !isFinishing()) {
                            Toast.makeText(LoginActivity.this, getString(R.string.request_timeout), Toast.LENGTH_LONG).show();
                        }
                    }, LOADING_TIMEOUT_MS);
                    break;
                case SUCCESS:
                    Log.d(TAG, "Login observer: SUCCESS");
                    isLoading = false;
                    loadingTimeoutHandler.removeCallbacksAndMessages(null);
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(getString(R.string.confirm));
                    }
                    if (resource.data != null && resource.data.isSuccess()) {
                        boolean rememberMe = rememberMeCheckbox != null && rememberMeCheckbox.isChecked();
                        appPreferences.setRememberMe(rememberMe);
                        tokenManager.saveTokens(
                            resource.data.getAccessToken(),
                            resource.data.getRefreshToken(),
                            rememberMe
                        );
                        Log.d(TAG, "Login successful (rememberMe=" + rememberMe + ").");
                        Toast.makeText(this, getString(R.string.login_successful_redirect), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, App.class));
                        finish();
                    } else {
                        // Login API returned success:false or no user data — always show feedback
                        String message = resource.data != null ? resource.data.getMessage() : getString(R.string.login_failed);
                        if (message != null) {
                            String lowerMsg = message.toLowerCase(Locale.ROOT);
                            if (lowerMsg.contains("not found") || lowerMsg.contains("no account") || lowerMsg.contains("invalid")) {
                                message = getString(R.string.account_not_found);
                            }
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ERROR:
                    Log.d(TAG, "Login observer: ERROR — stopping loading");
                    isLoading = false;
                    loadingTimeoutHandler.removeCallbacksAndMessages(null);
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(getString(R.string.confirm));
                    }
                    // Always show error feedback — Toast is harmless even if finishing
                    String errorMsg = resource.message;
                    if (errorMsg != null) {
                        String lowerMsg = errorMsg.toLowerCase(Locale.ROOT);
                        if (lowerMsg.contains("not found") || lowerMsg.contains("no account") || lowerMsg.contains("does not exist") || lowerMsg.contains("not registered") || lowerMsg.contains("invalid")) {
                            errorMsg = getString(R.string.account_not_found);
                        } else if (lowerMsg.contains("device")) {
                            errorMsg = getString(R.string.device_not_allowed);
                        }
                    }
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = getString(R.string.login_failed);
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Login error displayed: " + errorMsg);
                    break;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingTimeoutHandler != null) {
            loadingTimeoutHandler.removeCallbacksAndMessages(null);
        }
    }

}
