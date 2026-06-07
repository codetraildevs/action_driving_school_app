package com.drivingschoolrwandaapp.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ApplicationDetailsActivity extends AppCompatActivity {

    private IremboViewModel iremboViewModel;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        setupObservers();

        if (getIntent().hasExtra("application_details")) {
            IremboApplication application = (IremboApplication) getIntent().getSerializableExtra("application_details");
            if (application != null) {
                setupViews(application);
            }
        } else if (getIntent().hasExtra("application_ref")) {
            // Handle case where we only have the reference number
            String ref = getIntent().getStringExtra("application_ref");
            if (ref != null) {
                iremboViewModel.fetchApplicationDetails(ref);
            }
        }
    }

    private void setupObservers() {
        iremboViewModel.getApplicationDetails().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    setupViews(resource.data);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupViews(IremboApplication app) {
        TextView tvServiceName = findViewById(R.id.tv_service_name);
        TextView tvReference = findViewById(R.id.tv_reference);
        TextView tvStatus = findViewById(R.id.tv_status_badge);
        TextView tvDate = findViewById(R.id.tv_submitted_date);
        
        TextView tvCurrentStep = findViewById(R.id.tv_current_step);
        ProgressBar progressCompletion = findViewById(R.id.progress_completion);
        TextView tvCompletionPercentage = findViewById(R.id.tv_completion_percentage);
        TextView tvMessage = findViewById(R.id.tv_message);

        // Header
        tvServiceName.setText(app.getTitle());
        tvReference.setText("Ref: " + app.getReference());
        tvStatus.setText(app.getStatus() != null ? app.getStatus().toUpperCase() : "UNKNOWN");
        tvDate.setText(formatDate(app.getDate()));
        
        // Progress
        String currentStep = app.getCurrentStep();
        if (TextUtils.isEmpty(currentStep)) {
            tvCurrentStep.setText("Processing");
        } else {
            tvCurrentStep.setText(currentStep);
        }
        
        progressCompletion.setProgress(app.getCompletionPercentage());
        tvCompletionPercentage.setText(app.getCompletionPercentage() + "%");
        
        // Message
        String message = app.getMessage();
        if (TextUtils.isEmpty(message)) {
            tvMessage.setText("No additional details available.");
        } else {
            tvMessage.setText(message);
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null) return "";
        try {
            String standardDate = dateString.replace("Z", "+0000");
            
            SimpleDateFormat inputFormat;
            // Check for milliseconds (contains dot before timezone)
            if (standardDate.matches(".*\\.\\d+\\+0000")) {
                 inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            } else {
                 inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            }
            
            Date date = inputFormat.parse(standardDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            // Try one more time with the original approach just in case
             try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(dateString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
                return outputFormat.format(date);
            } catch (Exception ex) {
                return dateString;
            }
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(this));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
