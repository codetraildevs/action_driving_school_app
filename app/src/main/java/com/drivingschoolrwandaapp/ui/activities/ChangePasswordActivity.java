package com.drivingschoolrwandaapp.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChangePasswordActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private TextInputEditText currentPasswordField, newPasswordField, confirmPasswordField;
    private MaterialButton savePasswordButton;
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        currentPasswordField = findViewById(R.id.current_password_field);
        newPasswordField = findViewById(R.id.new_password_field);
        confirmPasswordField = findViewById(R.id.confirm_password_field);
        savePasswordButton = findViewById(R.id.save_password_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        savePasswordButton.setOnClickListener(v -> changePassword());

        userViewModel.getChangePasswordResult().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    loadingIndicator.setVisibility(View.VISIBLE);
                    savePasswordButton.setEnabled(false);
                    break;
                case SUCCESS:
                    loadingIndicator.setVisibility(View.GONE);
                    savePasswordButton.setEnabled(true);
                    Toast.makeText(this, resource.data.getMessage(), Toast.LENGTH_SHORT).show();
                    if (resource.data.isSuccess()) {
                        finish();
                    }
                    break;
                case ERROR:
                    loadingIndicator.setVisibility(View.GONE);
                    savePasswordButton.setEnabled(true);
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void changePassword() {
        String currentPassword = currentPasswordField.getText().toString().trim();
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show();
            return;
        }

        userViewModel.changePassword(currentPassword, newPassword, confirmPassword);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
