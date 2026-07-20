package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailField;
    private TextInputLayout emailLayout;
    private Button sendLinkButton;
    private ProgressBar loadingIndicator;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        emailField = findViewById(R.id.email_field);
        emailLayout = findViewById(R.id.email_layout);
        sendLinkButton = findViewById(R.id.send_link_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        sendLinkButton.setOnClickListener(v -> validateAndSendLink());

        userViewModel.getForgotPasswordResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (resource.data != null && resource.data.isSuccess()) {
                        String message = resource.data.getMessage();
                        Toast.makeText(this, message != null ? message : getString(R.string.send_reset_link), Toast.LENGTH_SHORT).show();
                        if (!isFinishing()) {
                            String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";
                            Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(this, resource.message != null ? resource.message : getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    setLoadingState(false);
                    Toast.makeText(this, resource.message != null ? resource.message : getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void validateAndSendLink() {
        if (!validateEmail()) {
            return;
        }
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();
        userViewModel.forgotPassword(email);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            sendLinkButton.setEnabled(false);
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            sendLinkButton.setEnabled(true);
            loadingIndicator.setVisibility(View.GONE);
        }
    }

    private boolean validateEmail() {
        String email = Objects.requireNonNull(emailField.getText()).toString().trim();
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            return false;
        } else {
            emailLayout.setError(null);
            return true;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
