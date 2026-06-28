package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.models.entities.Device;
import com.drivingschoolrwandaapp.models.entities.User;
import com.drivingschoolrwandaapp.models.response.RegisterResponse;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import java.util.Locale;
import java.util.Objects;
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
    private static final String TAG = "RegisterActivity";

    @Inject
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_register);

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
        tokenManager = new TokenManager(this);
        // Set Listeners
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        registerButton.setOnClickListener(v -> validateAndRegister());

    }

    private void validateAndRegister() {
        if (!validateFullName() | !validatePhone()) {
            return; // Validation failed
        }

        setLoadingState(true);

        String fullName = Objects.requireNonNull(fullNameField.getText()).toString().trim();
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

        String phone = Objects.requireNonNull(phoneField.getText()).toString().trim();
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

        Call<RegisterResponse> call = apiService.register(user);

        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(RegisterActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    String email = phoneField.getText().toString().trim();
                    @SuppressLint("HardwareIds")
                    String password = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    String deviceId = password;
                    userViewModel.login(email, password, deviceId);
                    userViewModel.getLoginResult().observe(RegisterActivity.this, resource -> {
                        switch (resource.status) {
                            case LOADING:
                                loadingIndicator.setVisibility(View.VISIBLE);
                                break;
                            case SUCCESS:
                                loadingIndicator.setVisibility(View.GONE);
                                if (resource.data != null && resource.data.isSuccess()) {
                                    tokenManager.saveTokens(resource.data.getAccessToken(), resource.data.getRefreshToken());
                                    Log.d(TAG, "Login successful.");
                                    Toast.makeText(RegisterActivity.this, resource.data.getMessage(), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, App.class));
                                    finish();
                                } else {
                                    String message = resource.data != null ? resource.data.getMessage() : getString(R.string.login_failed);
                                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case ERROR:
                                loadingIndicator.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, resource.message, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    });
                } else {
                    String errorMessage = getString(R.string.registration_failed);
                    if (response.errorBody() != null) {
                        try {
                            RegisterResponse errorResponse = new Gson().fromJson(
                                    response.errorBody().charStream(),
                                    RegisterResponse.class
                            );
                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                errorMessage = errorResponse.getMessage();
                            }
                        } catch (Exception e) {
                            // Could not parse error body
                        }
                    } else if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed) + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            registerButton.setEnabled(false);
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            registerButton.setEnabled(true);
            loadingIndicator.setVisibility(View.GONE);
        }
    }

    private boolean validateFullName() {
        String fullName = Objects.requireNonNull(fullNameField.getText()).toString().trim();
        if (fullName.isEmpty()) {
            fullNameLayout.setError(getString(R.string.first_name_required));
            return false;
        } else {
            fullNameLayout.setError(null);
            return true;
        }
    }

    private boolean validatePhone() {
        String phone = Objects.requireNonNull(phoneField.getText()).toString().trim();
        if (phone.isEmpty()) {
            phoneLayout.setError(getString(R.string.phone_required));
            return false;
        } else if (phone.length() < 10) {
            phoneLayout.setError(getString(R.string.invalid_phone));
            return false;
        } else {
            phoneLayout.setError(null);
            return true;
        }
    }
}
