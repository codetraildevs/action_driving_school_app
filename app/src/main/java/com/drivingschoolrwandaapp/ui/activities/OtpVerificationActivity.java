package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OtpVerificationActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextInputEditText otpField;
    private MaterialButton verifyButton;
    private ProgressBar loadingIndicator;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        email = getIntent().getStringExtra("email");

        otpField = findViewById(R.id.otp_field);
        verifyButton = findViewById(R.id.verify_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        verifyButton.setOnClickListener(v -> {
            String otp = otpField.getText().toString().trim();
            if (otp.length() != 6) {
                Toast.makeText(this, getString(R.string.invalid_otp), Toast.LENGTH_SHORT).show();
                return;
            }
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            userViewModel.verifyOtp(email, otp);
        });

        userViewModel.getVerifyOtpResult().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    loadingIndicator.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    loadingIndicator.setVisibility(View.GONE);
                    if (resource.data != null && resource.data.isSuccess()) {
                        Toast.makeText(this, resource.data.getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("token", resource.data.getData());
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, resource.message != null ? resource.message : getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
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
