package com.drivingschoolrwandaapp.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse;
import com.drivingschoolrwandaapp.utils.IntegrityHelper;
import com.drivingschoolrwandaapp.utils.PaymentUtils;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Shared plumbing for the full-page Irembo request forms (license and
 * special service). Handles the toolbar, loading/payment dialogs, user
 * profile prefill and location data, so the concrete form activities only
 * implement their fields and submit logic.
 */
@AndroidEntryPoint
public abstract class BaseIremboFormActivity extends AppCompatActivity {

    private static final String TAG = "BaseIremboForm";

    protected IremboViewModel iremboViewModel;
    protected UserViewModel userViewModel;
    protected User currentUser;
    protected JSONObject locationData;

    private AlertDialog loadingDialog;
    private final ExecutorService locationExecutor = Executors.newSingleThreadExecutor();

    /** The layout id of the concrete form page. */
    protected abstract int getFormLayoutId();

    /** Called after the form views are available (post-setContentView). */
    protected abstract void onFormViewsReady();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdgeUtils.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(getFormLayoutId());

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        setupToolbar();
        setupUserObserver();
        loadLocationData();
        onFormViewsReady();

        userViewModel.loadProfile();
        // Keep the cached applications fresh so the duplicate-request
        // pre-check below reflects the latest server state.
        iremboViewModel.fetchRecentApplications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationExecutor.shutdownNow();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupUserObserver() {
        userViewModel.getUserLiveData().observe(this, resource -> {
            if (resource != null && resource.data != null) {
                currentUser = resource.data;
                onUserLoaded(resource.data);
            }
        });
    }

    /** Hook for prefilling applicant fields once the profile arrives. */
    protected void onUserLoaded(User user) {
    }

    protected void executeSafely(Runnable runnable) {
        try {
            if (!locationExecutor.isShutdown() && !locationExecutor.isTerminated()) {
                locationExecutor.execute(runnable);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            Log.w("IremboForm", "Task rejected, executor is shutting down", e);
        }
    }

    protected void loadLocationData() {
        executeSafely(() -> {
            try {
                InputStream is = getAssets().open("location.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);
                JSONObject result = new JSONObject(json);
                runOnUiThread(() -> locationData = result);
            } catch (IOException | JSONException e) {
                Log.e("IremboForm", "Error loading location data", e);
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, getString(R.string.error_loading_location), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    protected void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(this));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        if (!isFinishing() && !loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    protected void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /** Shows a clear message when the user already has an active request for this service. */
    protected void showAlreadyRequestedDialog() {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.request_already_sent_title)
                .setMessage(R.string.request_already_sent_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    protected void showPaymentConfirmationDialog(IremboPaymentResponse paymentDetails) {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_payment_confirmation, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Fixed 500 RWF service fee for Irembo processing
        int serviceFee = 500;
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        tvAmount.setText(getString(R.string.amount_with_currency_format, format.format(serviceFee), "RWF"));

        PaymentUtils.setupPaymentMethods(dialogView, this, String.valueOf(serviceFee));

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            iremboViewModel.fetchRecentApplications();
            finish();
        });

        dialog.show();
        // Keep the dialog within the screen so every method stays reachable.
        PaymentUtils.capDialogHeight(dialog, 0.8f);
    }

    /**
     * Run a Play Integrity check in the background before submitting an Irembo request.
     * Does NOT block the submission — this is fail-open for anti-fraud logging only.
     * Call from {@code submit()} in concrete form activities.
     */
    protected void verifyIntegrityBeforeSubmit() {
        String token = null;
        try {
            token = new com.drivingschoolrwandaapp.data.local.preferences.TokenManager(this)
                    .getAccessToken();
        } catch (Exception e) {
            Log.w(TAG, "Could not read access token for integrity check", e);
        }
        IntegrityHelper.attest(this, token, (verified, requestId, error) -> {
            if (verified) {
                Log.d(TAG, "Play Integrity verified before Irembo submit (requestId=" + requestId + ")");
            } else {
                Log.w(TAG, "Play Integrity check failed before Irembo submit: " + error
    + " — request allowed (fail-open)");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PaymentUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
