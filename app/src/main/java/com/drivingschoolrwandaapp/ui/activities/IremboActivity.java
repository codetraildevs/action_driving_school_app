package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.utils.AdManager;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.drivingschoolrwandaapp.models.IremboService;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.IremboServiceAdapter;
import com.drivingschoolrwandaapp.ui.adapters.RecentActivityAdapter;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IremboActivity extends AppCompatActivity implements IremboServiceAdapter.OnItemClickListener, RecentActivityAdapter.OnItemClickListener {

    private IremboViewModel iremboViewModel;
    private AlertDialog loadingDialog;
    private RecentActivityAdapter recentActivityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdgeUtils.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_irembo);

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        setupObservers();

        setupToolbar();
        setupRecentActivity();
        setupBrowseServices();
        setupTrackApplication();

        // Load AdMob banner
        FrameLayout adContainer = findViewById(R.id.ad_container);
        if (adContainer != null) {
            AdManager.showBanner(this, adContainer, null);
        }

        iremboViewModel.fetchRecentApplications();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupTrackApplication() {
        TextInputEditText etApplicationNumber = findViewById(R.id.et_application_number);
        Button btnTrack = findViewById(R.id.btn_track);

        btnTrack.setOnClickListener(v -> {
            String appNumber = etApplicationNumber.getText() != null ? etApplicationNumber.getText().toString().trim() : "";
            if (TextUtils.isEmpty(appNumber)) {
                etApplicationNumber.setError(getString(R.string.error_required_field));
                hideTrackResult();
                return;
            }
            // Show rewarded ad before tracking application
            if (AdManager.isRewardedAdReady()) {
                AdManager.showRewardedAdIfReady(this, new AdManager.RewardedAdCallback() {
                    @Override
                    public void onRewardEarned(@NonNull com.google.android.gms.ads.rewarded.RewardItem reward) {
                        runOnUiThread(() -> {
                            Toast.makeText(IremboActivity.this, getString(R.string.dsrw_ad_reward_msg), Toast.LENGTH_LONG).show();
                            iremboViewModel.fetchApplicationDetails(appNumber);
                        });
                    }

                    @Override
                    public void onAdFailedToShow() {
                        iremboViewModel.fetchApplicationDetails(appNumber);
                    }
                });
            } else {
                iremboViewModel.fetchApplicationDetails(appNumber);
            }
        });
    }

    private void showTrackLoading() {
        LinearLayout container = findViewById(R.id.track_result_container);
        ProgressBar progress = findViewById(R.id.track_result_progress);
        MaterialCardView card = findViewById(R.id.track_result_card);
        if (container == null || progress == null || card == null) return;
        container.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        card.setVisibility(View.GONE);
    }

    private void showTrackResult(boolean found, String title, String message) {
        LinearLayout container = findViewById(R.id.track_result_container);
        ProgressBar progress = findViewById(R.id.track_result_progress);
        MaterialCardView card = findViewById(R.id.track_result_card);
        ImageView icon = findViewById(R.id.track_result_icon);
        FrameLayout iconContainer = findViewById(R.id.track_result_icon_container);
        TextView tvTitle = findViewById(R.id.track_result_title);
        TextView tvMessage = findViewById(R.id.track_result_message);
        if (container == null || card == null || icon == null || tvTitle == null || tvMessage == null) return;

        container.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        card.setVisibility(View.VISIBLE);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (found) {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.correct_answer_green));
        } else {
            icon.setImageResource(R.drawable.ic_error);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.incorrect_answer_red));
        }
        if (iconContainer != null) {
            iconContainer.setBackgroundResource(R.drawable.bg_payment_logo);
        }
    }

    private void hideTrackResult() {
        LinearLayout container = findViewById(R.id.track_result_container);
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }

    private void setupRecentActivity() {
        RecyclerView recyclerView = findViewById(R.id.rv_recent_activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(recentActivityAdapter);

        findViewById(R.id.tv_view_all).setOnClickListener(v -> {
            startActivity(new Intent(this, MyApplicationsActivity.class));
        });
    }

    private void setupBrowseServices() {
        RecyclerView recyclerView = findViewById(R.id.rv_browse_services);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<IremboService> services = new ArrayList<>();
        services.add(new IremboService(getString(R.string.provisional_license), R.drawable.ic_car_side));
        services.add(new IremboService(getString(R.string.special_irembo_service), R.drawable.ic_edit_document));

        IremboServiceAdapter adapter = new IremboServiceAdapter(services, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(IremboService service) {
        // Show rewarded ad before navigating to service form
        if (AdManager.isRewardedAdReady()) {
            AdManager.showRewardedAdIfReady(this, new AdManager.RewardedAdCallback() {
                @Override
                public void onRewardEarned(@NonNull RewardItem reward) {
                    runOnUiThread(() -> {
                        Toast.makeText(IremboActivity.this, getString(R.string.dsrw_ad_reward_msg), Toast.LENGTH_LONG).show();
                        navigateToService(service);
                    });
                }

                @Override
                public void onAdFailedToShow() {
                    navigateToService(service);
                }
            });
        } else {
            // Fallback to interstitial if no rewarded ad
            AdManager.loadInterstitial(IremboActivity.this);
            AdManager.showInterstitialIfReady(IremboActivity.this);
            navigateToService(service);
        }
    }

    private void navigateToService(IremboService service) {
        if (service.getName().equals(getString(R.string.provisional_license))) {
            startActivity(new Intent(this, LicenseRequestActivity.class));
        } else if (service.getName().equals(getString(R.string.special_irembo_service))) {
            startActivity(new Intent(this, SpecialRequestActivity.class));
        }
    }

    @Override
    public void onItemClick(IremboApplication application) {
        Intent intent = new Intent(this, ApplicationDetailsActivity.class);
        intent.putExtra("application_details", application);
        startActivity(intent);
    }

    private void setupObservers() {
        iremboViewModel.getRecentApplications().observe(this, resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    recentActivityAdapter.setApplications(resource.data);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (!isFinishing()) {
                    Toast.makeText(this, resource.message != null ? resource.message : getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                }
            }
        });

        iremboViewModel.getApplicationDetails().observe(this, resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
                showTrackLoading();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null && !isFinishing()) {
                    hideTrackResult();
                    Intent intent = new Intent(this, ApplicationDetailsActivity.class);
                    intent.putExtra("application_details", resource.data);
                    startActivity(intent);
                } else if (!isFinishing()) {
                    // Server replied success but no details: treat as not found.
                    showTrackResult(false, getString(R.string.application_not_found_title),
                            getString(R.string.irembo_application_not_found));
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                if (!isFinishing()) {
                    String message = resource.message != null ? resource.message : getString(R.string.something_went_wrong);
                    if (message.contains(getString(R.string.irembo_application_not_found))
                            || message.toLowerCase(Locale.ROOT).contains("not found")) {
                        showTrackResult(false, getString(R.string.application_not_found_title), message);
                    } else {
                        showTrackResult(false, getString(R.string.track_failed_title), message);
                    }
                }
            }
        });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        com.drivingschoolrwandaapp.utils.PaymentUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
