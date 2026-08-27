package com.drivingschoolrwandaapp.ui.activities;

import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.drivingschoolrwandaapp.models.entities.AdminUser;
import com.drivingschoolrwandaapp.models.entities.AdminUserDetail;
import com.drivingschoolrwandaapp.models.entities.AdminUserSubscription;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.utils.AdminTimeUtils;
import com.drivingschoolrwandaapp.utils.RoleUtils;
import com.drivingschoolrwandaapp.viewmodel.AdminViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Detail dialog for a single admin user: profile info, current subscription
 * and their Irembo requests. Loads from the activity-scoped
 * {@link AdminViewModel} (same instance as the Users tab) so the two fetch
 * calls are shared and cancelled/refreshed together.
 */
public final class AdminUserDetailDialog {

    private AdminUserDetailDialog() {
    }

    /**
     * Shows the dialog for {@code user}. The caller must call
     * {@code viewModel.refreshUserDetail(user.getId())} first so the dialog
     * opens on the loading state for the right user.
     */
    public static void show(FragmentActivity activity, AdminViewModel viewModel, AdminUser user) {
        if (activity == null || viewModel == null || user == null) return;

        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_admin_user_detail, null);

        ProgressBar progress = dialogView.findViewById(R.id.detail_progress);
        LinearLayout errorContainer = dialogView.findViewById(R.id.detail_error_container);
        TextView errorText = dialogView.findViewById(R.id.detail_error_text);
        LinearLayout contentContainer = dialogView.findViewById(R.id.detail_content_container);

        TextView avatar = dialogView.findViewById(R.id.detail_avatar);
        TextView name = dialogView.findViewById(R.id.detail_user_name);
        TextView phone = dialogView.findViewById(R.id.detail_user_phone);
        TextView roleBadge = dialogView.findViewById(R.id.detail_role_badge);
        TextView statusBadge = dialogView.findViewById(R.id.detail_status_badge);

        TextView planBadge = dialogView.findViewById(R.id.sub_plan_badge);
        TextView subStatusBadge = dialogView.findViewById(R.id.sub_status_badge);
        TextView startValue = dialogView.findViewById(R.id.sub_start_value);
        TextView endValue = dialogView.findViewById(R.id.sub_end_value);

        LinearLayout requestsContainer = dialogView.findViewById(R.id.requests_container);
        TextView requestsEmpty = dialogView.findViewById(R.id.requests_empty);

        SwipeRefreshLayout swipeRefresh = dialogView.findViewById(R.id.detail_swipe_refresh);
        // Counts in-flight pull-to-refresh fetches (detail + requests) so the
        // swipe indicator stops only after BOTH have settled.
        final int[] pendingRefreshes = {0};

        TextView lastUpdated = dialogView.findViewById(R.id.detail_last_updated);
        // Both streams must have delivered fresh data in the current load cycle
        // before the "last updated" footer is stamped (reset on every load).
        final boolean[] detailOk = {false};
        final boolean[] requestsOk = {false};

        // Render the header immediately from the row data for a snappy open.
        avatar.setText(user.getInitials());
        name.setText(TextUtils.isEmpty(user.getFullName()) ? "—" : user.getFullName());
        phone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        bindRoleBadge(activity, roleBadge, user.isAdmin(),
                user.getRole() != null ? user.getRole().getRoleName() : null);
        bindStatusBadge(activity, statusBadge, user.isActive());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setNegativeButton(R.string.admin_close, null)
                .create();
        dialog.show();

        // Detail (profile + subscription) — shared LiveData with the tab. The
        // observers are activity-scoped but MUST be removed when the dialog
        // closes, otherwise they pile up (one pair per opened dialog) and re-fire
        // into dead dialog views whenever a new user is loaded.
        Observer<Resource<AdminUserDetail>> detailObserver = resource -> {
            if (resource.getStatus() == Resource.Status.LOADING) {
                // During a pull-to-refresh keep the loaded content on screen and
                // let the swipe indicator convey progress; only the first load
                // (or a retry after error) shows the full-screen progress bar.
                // isRefreshing() is true exactly while a pull-triggered reload
                // is in flight (onRefresh fires before the synchronous LOADING
                // delivery), matching the AdminUsersFragment pattern.
                detailOk[0] = false;
                requestsOk[0] = false;
                if (!swipeRefresh.isRefreshing()) {
                    progress.setVisibility(View.VISIBLE);
                    errorContainer.setVisibility(View.GONE);
                    contentContainer.setVisibility(View.GONE);
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                progress.setVisibility(View.GONE);
                contentContainer.setVisibility(View.GONE);
                errorText.setText(resource.getMessage() != null
                        ? resource.getMessage()
                        : activity.getString(R.string.admin_load_failed));
                errorContainer.setVisibility(View.VISIBLE);
                settleRefresh(swipeRefresh, pendingRefreshes);
            } else {
                progress.setVisibility(View.GONE);
                errorContainer.setVisibility(View.GONE);
                contentContainer.setVisibility(View.VISIBLE);
                renderDetail(activity, resource.getData(), avatar, name, phone, roleBadge,
                        statusBadge, planBadge, subStatusBadge, startValue, endValue);
                detailOk[0] = true;
                stampLastUpdated(activity, lastUpdated, detailOk, requestsOk);
                settleRefresh(swipeRefresh, pendingRefreshes);
            }
        };
        viewModel.getUserDetail().observe(activity, detailObserver);

        // User's Irembo requests — bare list.
        Observer<Resource<List<AdminRequest>>> requestsObserver = resource -> {
            if (resource.getStatus() == Resource.Status.LOADING) {
                // Keep the current request list during a pull-to-refresh; only
                // the first load (or a retry) clears the placeholders.
                detailOk[0] = false;
                requestsOk[0] = false;
                if (!swipeRefresh.isRefreshing()) {
                    requestsContainer.removeAllViews();
                    requestsEmpty.setVisibility(View.GONE);
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                requestsContainer.removeAllViews();
                requestsEmpty.setText(resource.getMessage() != null
                        ? resource.getMessage()
                        : activity.getString(R.string.admin_load_failed));
                requestsEmpty.setVisibility(View.VISIBLE);
                settleRefresh(swipeRefresh, pendingRefreshes);
            } else {
                List<AdminRequest> requests = resource.getData();
                requestsContainer.removeAllViews();
                if (requests == null || requests.isEmpty()) {
                    requestsEmpty.setText(activity.getString(R.string.admin_no_requests));
                    requestsEmpty.setVisibility(View.VISIBLE);
                } else {
                    requestsEmpty.setVisibility(View.GONE);
                    for (AdminRequest request : requests) {
                        requestsContainer.addView(buildRequestRow(activity, request));
                    }
                }
                requestsOk[0] = true;
                stampLastUpdated(activity, lastUpdated, detailOk, requestsOk);
                settleRefresh(swipeRefresh, pendingRefreshes);
            }
        };
        viewModel.getUserRequests().observe(activity, requestsObserver);

        // Clean up both observers once the dialog is gone (covers the Close
        // button, tapping outside, back press, and programmatic dismiss).
        dialog.setOnDismissListener(d -> {
            viewModel.getUserDetail().removeObserver(detailObserver);
            viewModel.getUserRequests().removeObserver(requestsObserver);
        });
        dialog.setOnCancelListener(d -> {
            viewModel.getUserDetail().removeObserver(detailObserver);
            viewModel.getUserRequests().removeObserver(requestsObserver);
        });

        dialogView.findViewById(R.id.detail_retry_button)
                .setOnClickListener(v -> viewModel.refreshUserDetail(user.getId()));

        // Pull-to-refresh reloads detail + requests together. The indicator is
        // stopped by settleRefresh once both fetches reach a terminal state.
        swipeRefresh.setColorSchemeResources(R.color.my_primary);
        swipeRefresh.setOnRefreshListener(() -> {
            pendingRefreshes[0] = 2; // detail + requests
            viewModel.refreshUserDetail(user.getId());
        });
    }

    /**
     * Stops the pull-to-refresh spinner once both in-flight fetches (detail +
     * requests) have reached a terminal state.
     */
    private static void settleRefresh(SwipeRefreshLayout swipeRefresh, int[] pending) {
        if (swipeRefresh == null || pending[0] <= 0) return;
        if (--pending[0] == 0) {
            swipeRefresh.setRefreshing(false);
        }
    }

    /**
     * Stamps the footer "last updated" line once BOTH streams have delivered
     * fresh data for the current load cycle. The flags are reset on the next
     * LOADING, so each initial load and pull-to-refresh re-stamps the time.
     */
    private static void stampLastUpdated(FragmentActivity activity, TextView lastUpdated,
                                         boolean[] detailOk, boolean[] requestsOk) {
        if (lastUpdated == null || !(detailOk[0] && requestsOk[0])) return;
        lastUpdated.setText(activity.getString(R.string.admin_last_updated,
                AdminTimeUtils.formatLastUpdated(System.currentTimeMillis())));
        lastUpdated.setVisibility(View.VISIBLE);
        // Clear so a subsequent success within the same cycle can't re-stamp.
        detailOk[0] = false;
        requestsOk[0] = false;
    }

    private static void renderDetail(FragmentActivity activity, AdminUserDetail detail,
                                     TextView avatar, TextView name, TextView phone,
                                     TextView roleBadge, TextView statusBadge,
                                     TextView planBadge, TextView subStatusBadge,
                                     TextView startValue, TextView endValue) {
        if (detail == null) return;

        avatar.setText(detail.getInitials());
        name.setText(TextUtils.isEmpty(detail.getFullName()) ? "—" : detail.getFullName());
        phone.setText(detail.getPhoneNumber() != null ? detail.getPhoneNumber() : "");
        bindRoleBadge(activity, roleBadge, detail.isAdmin(),
                detail.getRole() != null ? detail.getRole().getRoleName() : null);
        bindStatusBadge(activity, statusBadge, detail.isActive());

        AdminUserSubscription subscription = detail.getUserSubscription();
        if (subscription == null || subscription.getSubscriptionPlan() == null) {
            planBadge.setText(activity.getString(R.string.admin_no_subscription));
            planBadge.setTextColor(ContextCompat.getColor(activity, R.color.colorOnSurface));
            planBadge.setBackgroundResource(R.drawable.bg_badge);
            subStatusBadge.setVisibility(View.GONE);
            startValue.setText("—");
            endValue.setText("—");
            return;
        }

        String planName = subscription.getSubscriptionPlan().getPlanName();
        planBadge.setText(planName != null ? planName : "—");
        planBadge.setTextColor(ContextCompat.getColor(activity, R.color.colorOnSurface));
        planBadge.setBackgroundResource(R.drawable.bg_badge);

        boolean active = isSubscriptionActive(subscription.getEndDate());
        subStatusBadge.setVisibility(View.VISIBLE);
        if (active) {
            subStatusBadge.setText(activity.getString(R.string.admin_sub_active));
            subStatusBadge.setTextColor(ContextCompat.getColor(activity, R.color.status_green));
            subStatusBadge.setBackgroundResource(R.drawable.status_done_background);
        } else {
            subStatusBadge.setText(activity.getString(R.string.admin_sub_expired));
            subStatusBadge.setTextColor(ContextCompat.getColor(activity, R.color.status_red));
            subStatusBadge.setBackgroundResource(R.drawable.status_rejected_background);
        }

        startValue.setText(formatDate(subscription.getStartDate()));
        endValue.setText(formatDate(subscription.getEndDate()));
    }

    private static View buildRequestRow(FragmentActivity activity, AdminRequest request) {
        View row = LayoutInflater.from(activity)
                .inflate(R.layout.item_admin_request, null);

        String type = request.getType() != null ? request.getType().replace('_', ' ') : "";
        ((TextView) row.findViewById(R.id.request_type))
                .setText(type.isEmpty() ? "—" : type.toUpperCase(Locale.ROOT));
        ((TextView) row.findViewById(R.id.request_title))
                .setText(request.getTitle() != null ? request.getTitle() : "");

        int progress = Math.max(0, Math.min(100, request.getCompletionPercentage()));
        ((LinearProgressIndicator) row.findViewById(R.id.request_progress))
                .setProgressCompat(progress, true);
        ((TextView) row.findViewById(R.id.request_progress_text)).setText(progress + "%");

        String status = request.getStatus() != null
                ? request.getStatus().toUpperCase(Locale.ROOT) : "";
        TextView statusChip = row.findViewById(R.id.request_status);
        statusChip.setText(status.isEmpty() ? "—" : status);
        com.drivingschoolrwandaapp.ui.adapters.AdminRequestAdapter.styleStatusChip(statusChip, status);

        // Give each row breathing room between cards.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * activity.getResources().getDisplayMetrics().density);
        row.setLayoutParams(lp);
        return row;
    }

    private static void bindRoleBadge(FragmentActivity activity, TextView badge, boolean admin, String roleName) {
        badge.setText(roleName != null ? roleName : "—");
        if (admin) {
            badge.setTextColor(ContextCompat.getColor(activity, R.color.my_primary));
            badge.setBackgroundResource(R.drawable.bg_badge_admin);
        } else {
            badge.setTextColor(ContextCompat.getColor(activity, R.color.colorOnSurface));
            badge.setBackgroundResource(R.drawable.bg_badge);
        }
    }

    private static void bindStatusBadge(FragmentActivity activity, TextView badge, boolean active) {
        if (active) {
            badge.setText(activity.getString(R.string.admin_user_active));
            badge.setTextColor(ContextCompat.getColor(activity, R.color.status_green));
            badge.setBackgroundResource(R.drawable.status_done_background);
        } else {
            badge.setText(activity.getString(R.string.admin_user_inactive));
            badge.setTextColor(ContextCompat.getColor(activity, R.color.status_red));
            badge.setBackgroundResource(R.drawable.status_rejected_background);
        }
    }

    private static boolean isSubscriptionActive(String endDate) {
        if (endDate == null) return false;
        try {
            return Instant.parse(endDate).isAfter(Instant.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Formats an ISO-8601 timestamp as yyyy-MM-dd, falling back to the raw string. */
    private static String formatDate(String iso) {
        if (TextUtils.isEmpty(iso)) return "—";
        return iso.length() >= 10 ? iso.substring(0, 10) : iso;
    }
}
