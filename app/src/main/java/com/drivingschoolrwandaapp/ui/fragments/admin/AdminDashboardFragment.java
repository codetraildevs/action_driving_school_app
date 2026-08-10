package com.drivingschoolrwandaapp.ui.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.AdminDashboardStats;
import com.drivingschoolrwandaapp.models.entities.AdminRecentActivity;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.utils.AdminTimeUtils;
import com.drivingschoolrwandaapp.viewmodel.AdminViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardFragment extends Fragment {

    private AdminViewModel adminViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout contentContainer;
    private LinearLayout statsContainer;
    private LinearLayout activityContainer;
    private TextView welcomeText;
    private TextView lastUpdated;
    private TextView errorText;
    private LinearLayout errorContainer;

    private LayoutInflater inflater;
    private LinearLayout currentStatRow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.inflater = LayoutInflater.from(getContext());

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        contentContainer = view.findViewById(R.id.content_container);
        statsContainer = view.findViewById(R.id.stats_container);
        activityContainer = view.findViewById(R.id.activity_container);
        welcomeText = view.findViewById(R.id.welcome_text);
        lastUpdated = view.findViewById(R.id.last_updated);
        errorContainer = view.findViewById(R.id.error_container);
        errorText = view.findViewById(R.id.error_text);
        view.findViewById(R.id.retry_button).setOnClickListener(v -> adminViewModel.refreshDashboard());

        swipeRefreshLayout.setColorSchemeResources(R.color.my_primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Don't stop the spinner here — it stays up until the fetch settles
            // (see the terminal branches of the observer below), so the user
            // sees progress while the dashboard reloads.
            adminViewModel.refreshDashboard();
        });

        adminViewModel.getDashboardStats().observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.LOADING) {
                // Hide the timestamp while a (re)load is in flight; it is
                // re-stamped once fresh data lands below.
                lastUpdated.setVisibility(View.GONE);
                if (!swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                errorContainer.setVisibility(View.GONE);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                contentContainer.setVisibility(View.GONE);
                errorText.setText(resource.getMessage() != null
                        ? resource.getMessage()
                        : getString(R.string.admin_load_failed));
                errorContainer.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                errorContainer.setVisibility(View.GONE);
                contentContainer.setVisibility(View.VISIBLE);
                lastUpdated.setText(getString(R.string.admin_last_updated,
                        AdminTimeUtils.formatLastUpdated(System.currentTimeMillis())));
                lastUpdated.setVisibility(View.VISIBLE);
                renderDashboard(resource.getData());
            }
        });

        if (adminViewModel.getDashboardStats().getValue() == null) {
            adminViewModel.refreshDashboard();
        }
    }

    private void renderDashboard(AdminDashboardStats stats) {
        if (stats == null) {
            // A 200 with no data is still an error state — hide the "last
            // updated" footer so it never sits above the error message.
            lastUpdated.setVisibility(View.GONE);
            errorContainer.setVisibility(View.VISIBLE);
            errorText.setText(getString(R.string.admin_load_failed));
            return;
        }

        welcomeText.setText(getString(R.string.admin_welcome));

        // 2-column stat cards
        statsContainer.removeAllViews();
        currentStatRow = null;
        addStatCard(getString(R.string.admin_total_users), stats.getTotalUsers());
        addStatCard(getString(R.string.admin_active_users), stats.getActiveUsers());
        addStatCard(getString(R.string.admin_total_subscriptions), stats.getTotalSubscriptions());
        addStatCard(getString(R.string.admin_total_content), stats.getTotalContent());

        // Recent activity
        activityContainer.removeAllViews();
        List<AdminRecentActivity> activity = stats.getRecentActivity();
        if (activity == null || activity.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(getString(R.string.admin_no_activity));
            empty.setTextSize(13);
            empty.setTextColor(0x66001F2A);
            activityContainer.addView(empty);
            return;
        }
        for (AdminRecentActivity item : activity) {
            View row = inflater.inflate(R.layout.item_admin_activity, activityContainer, false);
            ((TextView) row.findViewById(R.id.activity_title)).setText(item.getTitle() != null ? item.getTitle() : "");
            ((TextView) row.findViewById(R.id.activity_description)).setText(item.getDescription() != null ? item.getDescription() : "");
            ((TextView) row.findViewById(R.id.activity_time)).setText(item.getTimestamp() != null ? item.getTimestamp() : "");
            activityContainer.addView(row);
        }
    }

    private void addStatCard(String label, long value) {
        if (currentStatRow == null || currentStatRow.getChildCount() >= 2) {
            currentStatRow = new LinearLayout(getContext());
            currentStatRow.setOrientation(LinearLayout.HORIZONTAL);
            statsContainer.addView(currentStatRow,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        View card = inflater.inflate(R.layout.item_admin_dashboard_stat, currentStatRow, false);
        ((TextView) card.findViewById(R.id.stat_value)).setText(String.valueOf(value));
        ((TextView) card.findViewById(R.id.stat_label)).setText(label);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(margin, margin, margin, margin);
        currentStatRow.addView(card, lp);
    }
}
