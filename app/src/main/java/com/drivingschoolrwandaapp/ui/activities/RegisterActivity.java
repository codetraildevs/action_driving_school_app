package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.annotation.SuppressLint;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.models.entities.Device;
import com.drivingschoolrwandaapp.models.entities.User;
import com.drivingschoolrwandaapp.models.response.RegisterResponse;
import com.drivingschoolrwandaapp.utils.InsetsUtils;
import com.drivingschoolrwandaapp.utils.PhoneUtils;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText fullNameField, phoneField;
    private TextInputLayout fullNameLayout, phoneLayout;
    private Button registerButton;
    private ProgressBar loadingIndicator;
    private UserViewModel userViewModel;
    private TokenManager tokenManager;
    private AppPreferences appPreferences;
    private static final String TAG = "RegisterActivity";
    private static final long LOADING_TIMEOUT_MS = 45_000; // 45 seconds
    private boolean isRegistering = false;
    private Handler loadingTimeoutHandler;
    private String registeredPhone = ""; // Phone used in the last successful registration, for auto-login fallback
    private Call<RegisterResponse> registrationCall; // Stored to allow cancellation in onDestroy()

    @Inject
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeUtils.enable(this);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_register);

        // Keep content clear of the transparent system bars in edge-to-edge mode
        InsetsUtils.applySystemBarsPadding(findViewById(android.R.id.content), true, true);

        loadingTimeoutHandler = new Handler(Looper.getMainLooper());

        tokenManager = new TokenManager(this);
        appPreferences = new AppPreferences(this);

        // One account per device: if this device already has a registered account, redirect
        if (appPreferences.isDeviceRegistered() || tokenManager.isLoggedIn()) {
            Intent intent;
            if (tokenManager.isLoggedIn()) {
                // Already logged in - go to the right home for this user's role
                intent = com.drivingschoolrwandaapp.utils.RoleUtils.isAdminRole(tokenManager.getRoleId())
                        ? new Intent(RegisterActivity.this, AdminActivity.class)
                        : new Intent(RegisterActivity.this, App.class);
            } else {
                // Device has an account but not logged in - go to login
                Toast.makeText(this, getString(R.string.device_already_registered), Toast.LENGTH_LONG).show();
                intent = new Intent(RegisterActivity.this, LoginActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize Fields
        fullNameField = findViewById(R.id.fullName_field);
        phoneField = findViewById(R.id.phoneNumber_field);

        // Initialize Layouts
        fullNameLayout = findViewById(R.id.fullName_layout);
        phoneLayout = findViewById(R.id.phoneNumber_layout);

        // Initialize Buttons and ProgressBar
        registerButton = findViewById(R.id.register_button);
        Button loginButton = findViewById(R.id.login_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        
        // Set Listeners
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        registerButton.setOnClickListener(v -> validateAndRegister());
        
        // Register the login observer once (not inside the register callback to avoid stacking)
        setupLoginObserver();

    }

    private void setupLoginObserver() {
        userViewModel.getLoginResult().observe(RegisterActivity.this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    loadingIndicator.setVisibility(View.VISIBLE);
                    if (registerButton != null) {
                        registerButton.setEnabled(false);
                        registerButton.setText(getString(R.string.loading));
                    }
                    break;
                case SUCCESS:
                    isRegistering = false;
                    if (loadingTimeoutHandler != null) {
                        loadingTimeoutHandler.removeCallbacksAndMessages(null);
                    }
                    loadingIndicator.setVisibility(View.GONE);
                    if (registerButton != null) {
                        registerButton.setEnabled(true);
                        registerButton.setText(getString(R.string.confirm));
                    }
                    if (resource.data != null && resource.data.isSuccess()) {
                        // Registration auto-login always uses Remember Me (30 days)
                        tokenManager.saveTokens(
                            resource.data.getAccessToken(),
                            resource.data.getRefreshToken(),
                            true
                        );
                        // There is NO admin registration: the backend always creates a
                        // Student (role 5), so a freshly registered account always lands
                        // in the user app. The role is still persisted for consistency,
                        // but it can never route here to the admin console.
                        int roleId = resource.data.getUser() != null ? resource.data.getUser().getRole() : 0;
                        tokenManager.saveRole(roleId);
                        Log.d(TAG, "Login after registration successful (role=" + roleId + ").");
                        Toast.makeText(RegisterActivity.this, getString(R.string.login_successful_redirect), Toast.LENGTH_SHORT).show();
                        Intent destination = new Intent(RegisterActivity.this, App.class);
                        destination.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(destination);
                        finish();
                    } else {
                        String message = resource.data != null ? resource.data.getMessage() : getString(R.string.login_failed);
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    isRegistering = false;
                    if (loadingTimeoutHandler != null) {
                        loadingTimeoutHandler.removeCallbacksAndMessages(null);
                    }
                    loadingIndicator.setVisibility(View.GONE);
                    if (registerButton != null) {
                        registerButton.setEnabled(true);
                        registerButton.setText(getString(R.string.confirm));
                    }
                    // Always show error feedback — Toast is harmless even if finishing
                    String errorText = resource.message != null ? resource.message : getString(R.string.login_failed);
                    Toast.makeText(RegisterActivity.this, errorText, Toast.LENGTH_LONG).show();
                    if (!registeredPhone.isEmpty() && !isFinishing()) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!RegisterActivity.this.isFinishing()) {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.putExtra("phone", registeredPhone);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }, 1500);
                    }
                    break;
            }
        });
    }

    private void validateAndRegister() {
        if (!validateFullName() | !validatePhone()) {
            return; // Validation failed
        }

        isRegistering = true;
        setLoadingState(true);

        // Set a timeout for the registration request
        loadingTimeoutHandler.postDelayed(() -> {
            if (isRegistering) {
                Toast.makeText(RegisterActivity.this, getString(R.string.request_timeout), Toast.LENGTH_LONG).show();
            }
        }, LOADING_TIMEOUT_MS);

        CharSequence fullNameText = fullNameField.getText();
        String fullName = fullNameText != null ? fullNameText.toString().trim() : "";
        String[] names = fullName.split("\\s+");
        String firstName = "";
        String middleName = "";
        String lastName = "";

        if (names.length > 0) {
            firstName = names[0];
        }
        if (names.length > 2) {
            lastName = names[names.length - 1];
            for (int i = 1; i < names.length - 1; i++) {
                middleName += names[i] + " ";
            }
            middleName = middleName.trim();
        } else if (names.length > 1) {
            lastName = names[names.length - 1];
        }

        CharSequence phoneText = phoneField.getText();
        String rawPhone = phoneText != null ? phoneText.toString().trim() : "";
        String phone = PhoneUtils.normalize(rawPhone);
        Log.d(TAG, "Normalised phone for registration: " + rawPhone + " → " + phone);
        @SuppressLint("HardwareIds")
        String password = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        String deviceId = password;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String deviceName = Build.MODEL;
        String timezone = TimeZone.getDefault().getID();
        String language = Locale.getDefault().getLanguage();

        Device device = new Device();
        device.setPhysicalAddress(deviceId);
        device.setName(deviceName);
        device.setModel(model);
        device.setManufacturer(manufacturer);
        User user = new User();
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setEmail("");
        user.setPhoneNumber(phone);
        user.setPassword(password);
        user.setDob(null);
        user.setLanguage(language);
        user.setTimezone(timezone);
        user.setDevice(device);

        registrationCall = apiService.register(user);

        registrationCall.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                isRegistering = false;
                if (loadingTimeoutHandler != null) {
                    loadingTimeoutHandler.removeCallbacksAndMessages(null);
                }
                setLoadingState(false);

                // Always show feedback — Toast is harmless even if finishing
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Mark this device as having a registered account (one account per device)
                    if (appPreferences != null) {
                        appPreferences.setDeviceRegistered(true);
                    }
                    // Show the server's success message
                    String successMsg = response.body().getMessage();
                    if (successMsg == null || successMsg.isEmpty()) {
                        successMsg = getString(R.string.registration_success);
                    }
                    Toast.makeText(RegisterActivity.this, successMsg, Toast.LENGTH_SHORT).show();
                    
                    // Save phone for auto-login fallback
                    registeredPhone = phone;
                    // Auto-login: make a direct API call so user goes straight to home/dashboard
                    @SuppressLint("HardwareIds")
                    String loginPassword = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    String loginDeviceId = loginPassword;
                    userViewModel.login(phone, loginPassword, loginDeviceId);
                } else {
                    String errorMessage = getString(R.string.registration_failed);

                    if (response.errorBody() != null) {
                        try {
                            String errorBodyStr = response.errorBody().string();
                            if (!errorBodyStr.isEmpty()) {
                                RegisterResponse errorResponse = new Gson().fromJson(errorBodyStr, RegisterResponse.class);
                                if (errorResponse != null && errorResponse.getMessage() != null) {
                                    errorMessage = errorResponse.getMessage();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("RegisterActivity", "Could not parse error response body", e);
                            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    } else if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    }

                    if (isMessageAlreadyRegistered(errorMessage)) {
                        // Guide user to login instead
                        Toast.makeText(RegisterActivity.this, getString(R.string.account_already_exists_with_help), Toast.LENGTH_LONG).show();
                        // Navigate to login after short delay, pre-filling the phone
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!RegisterActivity.this.isFinishing()) {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.putExtra("phone", phone);
                                startActivity(intent);
                            }
                        }, 1500);
                    } else if (errorMessage != null && errorMessage.toLowerCase(Locale.ROOT).contains("device")) {
                        // Show device-not-allowed message with support numbers
                        Toast.makeText(RegisterActivity.this, getString(R.string.device_not_allowed_short), Toast.LENGTH_LONG).show();
                    } else {
                        // Show the backend's actual error message
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                isRegistering = false;
                if (loadingTimeoutHandler != null) {
                    loadingTimeoutHandler.removeCallbacksAndMessages(null);
                }
                setLoadingState(false);

                // Always show error feedback — Toast is harmless even if finishing
                String friendlyMessage = getUserFriendlyErrorMessage(t);
                Toast.makeText(RegisterActivity.this, friendlyMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Checks if the error message from the registration API indicates the user already has an account.
     */
    private boolean isMessageAlreadyRegistered(String message) {
        if (message == null) return false;
        String lowerMsg = message.toLowerCase(Locale.ROOT);
        return lowerMsg.contains("already") || lowerMsg.contains("exists") || lowerMsg.contains("registered");
    }

    /**
     * Converts a Throwable error from a network call into a user-friendly message.
     */
    /**
     * Converts a Throwable error from a network call into a user-friendly message.
     * Shows a message with support contact info for persistent network issues.
     */
    private String getUserFriendlyErrorMessage(Throwable t) {
        if (t == null) return getString(R.string.something_went_wrong);
        String message = t.getMessage();
        if (message != null) {
            String lowerMsg = message.toLowerCase(Locale.ROOT);
            if (lowerMsg.contains("unable to resolve host") || lowerMsg.contains("failed to connect") || lowerMsg.contains("network is unreachable")) {
                return getString(R.string.network_error);
            }
            if (lowerMsg.contains("timeout") || lowerMsg.contains("timed out")) {
                return getString(R.string.request_timeout);
            }
        }
        return getString(R.string.registration_failed);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            registerButton.setEnabled(false);
            registerButton.setText(getString(R.string.loading));
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            registerButton.setEnabled(true);
            registerButton.setText(getString(R.string.confirm));
            loadingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leak: remove all pending timeout callbacks
        if (loadingTimeoutHandler != null) {
            loadingTimeoutHandler.removeCallbacksAndMessages(null);
        }
        // Cancel in-flight registration request if the activity is destroyed
        if (registrationCall != null && !registrationCall.isCanceled()) {
            registrationCall.cancel();
        }
        isRegistering = false;
    }

    private boolean validateFullName() {
        CharSequence fullNameText = fullNameField.getText();
        String fullName = fullNameText != null ? fullNameText.toString().trim() : "";
        if (fullName.isEmpty()) {
            fullNameLayout.setError(getString(R.string.first_name_required));
            return false;
        } else {
            fullNameLayout.setError(null);
            return true;
        }
    }

    private boolean validatePhone() {
        CharSequence phoneText = phoneField.getText();
        String phone = phoneText != null ? phoneText.toString().trim() : "";
        String errorKey = PhoneUtils.getValidationError(phone);
        if (errorKey != null) {
            // Map the error key to a localized string resource
            int resId;
            switch (errorKey) {
                case "phone_required":
                    resId = R.string.phone_required;
                    break;
                default:
                    resId = R.string.invalid_phone;
                    break;
            }
            phoneLayout.setError(getString(resId));
            return false;
        } else {
            phoneLayout.setError(null);
            return true;
        }
    }
}
