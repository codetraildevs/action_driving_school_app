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
public class ResetPasswordActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextInputEditText newPasswordField;
    private TextInputEditText confirmPasswordField;
    private MaterialButton resetPasswordButton;
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        newPasswordField = findViewById(R.id.new_password_field);
        confirmPasswordField = findViewById(R.id.confirm_password_field);
        resetPasswordButton = findViewById(R.id.reset_password_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        resetPasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();
            String token = getIntent().getStringExtra("token"); // Assuming token is passed via intent

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.password_empty_error), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }
            if (token != null) {
                userViewModel.resetPassword(token, newPassword, confirmPassword);
            } else {
                Toast.makeText(this, getString(R.string.token_not_found), Toast.LENGTH_LONG).show();
            }
        });

        userViewModel.getResetPasswordResult().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    loadingIndicator.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.password_reset_success), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    break;
                case ERROR:
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
