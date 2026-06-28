package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextInputEditText emailField;
    private MaterialButton loginButton;
    private ProgressBar loadingIndicator;
    private TokenManager tokenManager;
    private static final String TAG = "LoginActivity";

    @Override
    @SuppressLint("HardwareIds")
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);


        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        tokenManager = new TokenManager(this);

        emailField = findViewById(R.id.email_field);
        loginButton = findViewById(R.id.login_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        Button registerText = findViewById(R.id.register_text);
        registerText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = password;

            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            userViewModel.login(email, password, deviceId);
        });

        userViewModel.getLoginResult().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    loadingIndicator.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    loadingIndicator.setVisibility(View.GONE);
                    if (resource.data != null && resource.data.isSuccess()) {
                        tokenManager.saveTokens(resource.data.getAccessToken(), resource.data.getRefreshToken());
                        Log.d(TAG, "Login successful.");
                        Toast.makeText(this, resource.data.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, App.class));
                        finish();
                    } else {
                        String message = resource.data != null ? resource.data.getMessage() : getString(R.string.login_failed);
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

    }


}
