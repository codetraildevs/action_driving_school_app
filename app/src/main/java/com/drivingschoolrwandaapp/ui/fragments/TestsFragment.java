package com.drivingschoolrwandaapp.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.ui.adapters.TestAdapter;
import com.drivingschoolrwandaapp.utils.GridSpacingItemDecoration;
import com.drivingschoolrwandaapp.utils.PaymentUtils;
import com.drivingschoolrwandaapp.utils.SafetyUtils;
import com.drivingschoolrwandaapp.viewmodel.SubscriptionViewModel;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.drivingschoolrwandaapp.repository.Resource;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestsFragment extends Fragment {

    private TestViewModel testViewModel;
    private UserViewModel userViewModel;
    private SubscriptionViewModel subscriptionViewModel;
    private RecyclerView testsRecyclerView;
    private TestAdapter testAdapter;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private LinearLayout errorLayout;
    private com.google.android.material.button.MaterialButton retryButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AppPreferences appPreferences;
    private boolean isGridLayout = false;

    private BottomSheetDialog requestAccessDialog;
    private User currentUser;
    private final Set<Integer> downloadingTests = new HashSet<>();
    private final Set<Integer> completedTests = new HashSet<>();

    private int selectedDays = -1;
    private PlanOption selectedPlan;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        testViewModel = new ViewModelProvider(this).get(TestViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        subscriptionViewModel = new ViewModelProvider(requireActivity()).get(SubscriptionViewModel.class);
        appPreferences = new AppPreferences(requireContext());
        isGridLayout = appPreferences.isGridLayout();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        testsRecyclerView = view.findViewById(R.id.tests_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        errorTextView = view.findViewById(R.id.error_text_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        errorLayout = view.findViewById(R.id.error_layout);
        retryButton = view.findViewById(R.id.retry_button);

        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        observeViewModels();

        testViewModel.refreshTests();
        userViewModel.loadProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestAccessDialog != null && requestAccessDialog.isShowing()) {
            requestAccessDialog.dismiss();
        }
    }

    private void setupRecyclerView() {
        testAdapter = new TestAdapter(isGridLayout);
        updateLayoutManager();
        testsRecyclerView.setAdapter(testAdapter);
        String languageCode = appPreferences.getLanguage();
        testAdapter.setLanguage(languageCode);

        testAdapter.setOnTestClickListener((test, isLocked, title) -> {
            if (isLocked) {
                showRequestAccessDialog(test);
            } else {
                Bundle args = new Bundle();
                args.putInt("testId", test.getId());
                args.putString("title", title);
                args.putBoolean("isRealTimeFeedback", true);
                args.putBoolean("isFree", test.isFree());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_global_testQuestionsFragment, args);
            }
        });
    }

    private void updateLayoutManager() {
        while (testsRecyclerView.getItemDecorationCount() > 0) {
            testsRecyclerView.removeItemDecorationAt(0);
        }

        if (isGridLayout) {
            testsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            int spacing = (int) getResources().getDimension(R.dimen.grid_spacing);
            testsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
        } else {
            testsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        testAdapter.setGridLayout(isGridLayout);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            testViewModel.refreshTests();
            userViewModel.loadProfile();
        });
    }

    private void setupRetryButton() {
        retryButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            testViewModel.refreshTests();
            userViewModel.loadProfile();
        });
    }

    private void observeViewModels() {
        testViewModel.getTests().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            boolean hasData = resource.data != null && !resource.data.isEmpty();

            if (!swipeRefreshLayout.isRefreshing()) {
                progressBar.setVisibility(resource.status == Resource.Status.LOADING && !hasData ? View.VISIBLE : View.GONE);
            }

            if (resource.status != Resource.Status.LOADING) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (resource.data != null) {
                testAdapter.setTests(resource.data);
                downloadUnlockedTests();
            }

            if (resource.status == Resource.Status.ERROR) {
                if (!hasData) {
                    errorLayout.setVisibility(View.VISIBLE);
                    errorTextView.setText(resource.message);
                    testsRecyclerView.setVisibility(View.GONE);
                } else {
                    errorLayout.setVisibility(View.GONE);
                    testsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            } else if (resource.status == Resource.Status.SUCCESS) {
                if (hasData) {
                    errorLayout.setVisibility(View.GONE);
                    testsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        userViewModel.getUserLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.data != null) {
                currentUser = resource.data;
                testAdapter.setCurrentUser(currentUser);
                downloadUnlockedTests();
            }
        });

        subscriptionViewModel.getRequestAccessSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                if (requestAccessDialog != null && requestAccessDialog.isShowing()) {
                    requestAccessDialog.dismiss();
                }
                if (isAdded()) {
                    userViewModel.loadProfile();
                    subscriptionViewModel.doneShowingRequestAccessDialog();
                    showPaymentInstructionsDialog(selectedPlan);
                }
            }
        });

        subscriptionViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                 if (requestAccessDialog != null && requestAccessDialog.isShowing()) {
                    requestAccessDialog.dismiss();
                }
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void downloadUnlockedTests() {
        if (currentUser == null || testAdapter == null || !isAdded()) return;
        Resource<List<TestEntity>> testsResource = testViewModel.getTests().getValue();
        if (testsResource == null || testsResource.data == null) return;

        for (TestEntity test : testsResource.data) {
            if (!testAdapter.isTestLocked(test)) {
                final int testId = test.getId();
                if (completedTests.contains(testId) || downloadingTests.contains(testId)) {
                    continue;
                }

                downloadingTests.add(testId);
                final LiveData<Resource<TestWithQuestions>> downloadLiveData = testViewModel.downloadQuestions(testId);
                downloadLiveData.observe(getViewLifecycleOwner(), new Observer<Resource<TestWithQuestions>>() {
                    @Override
                    public void onChanged(Resource<TestWithQuestions> resource) {
                        if (resource == null) return;

                        boolean hasQuestions = resource.data != null && resource.data.questions != null && !resource.data.questions.isEmpty();

                        if (hasQuestions) {
                            testAdapter.setDownloadStatus(testId, Resource.Status.SUCCESS);
                            completedTests.add(testId);
                            downloadingTests.remove(testId);
                            downloadLiveData.removeObserver(this);
                        } else if (resource.status == Resource.Status.ERROR) {
                            testAdapter.setDownloadStatus(testId, Resource.Status.ERROR);
                            downloadingTests.remove(testId);
                            downloadLiveData.removeObserver(this);
                        } else if (resource.status == Resource.Status.LOADING) {
                            testAdapter.setDownloadStatus(testId, Resource.Status.LOADING);
                        } else if (resource.status == Resource.Status.SUCCESS) {
                            testAdapter.setDownloadStatus(testId, Resource.Status.SUCCESS);
                            completedTests.add(testId);
                            downloadingTests.remove(testId);
                            downloadLiveData.removeObserver(this);
                        }
                    }
                });
            }
        }
    }

    private void showRequestAccessDialog(TestEntity test) {
        requestAccessDialog = new BottomSheetDialog(requireContext());
        requestAccessDialog.setContentView(R.layout.dialog_subscription);

        Button btnConfirm = requestAccessDialog.findViewById(R.id.btn_confirm);
        ProgressBar dialogProgressBar = requestAccessDialog.findViewById(R.id.dialog_progress_bar);
        RecyclerView rvOptions = requestAccessDialog.findViewById(R.id.rv_subscription_options);

        selectedDays = -1;
        selectedPlan = null;
        btnConfirm.setEnabled(false);

        String instructions = getString(R.string.instructions_4);
        List<PlanOption> plans = parsePlans(instructions);

        SubscriptionPlanAdapter adapter = new SubscriptionPlanAdapter(plans, plan -> {
            selectedDays = plan.days;
            selectedPlan = plan;
            btnConfirm.setEnabled(true);
        });

        rvOptions.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvOptions.setAdapter(adapter);

        btnConfirm.setOnClickListener(v -> {
            if (selectedDays != -1) {
                dialogProgressBar.setVisibility(View.VISIBLE);
                btnConfirm.setEnabled(false);
                subscriptionViewModel.requestTestAccess(test.getTestNumber(), selectedDays, testAdapter.currentLanguageId);
            }
        });

        requestAccessDialog.show();
    }

    private List<PlanOption> parsePlans(String instructions) {
        List<PlanOption> planList = new ArrayList<>();
        if (instructions == null || instructions.isEmpty()) return planList;

        String[] rows = instructions.split(",");
        for (String row : rows) {
            String trimmedRow = row.trim();
            if (trimmedRow.isEmpty()) continue;

            String[] cells = trimmedRow.split("[=-]");
            if (cells.length >= 2) {
                String price = cells[0].trim();
                String duration = cells[1].trim();
                int days = 1;
                if (duration.toLowerCase(Locale.ROOT).contains("10")) days = 10;
                else if (duration.toLowerCase(Locale.ROOT).contains("25")) days = 25;
                else if (duration.toLowerCase(Locale.ROOT).contains("6") || duration.toLowerCase(Locale.ROOT).contains("amezi")) days = 180;
                
                planList.add(new PlanOption(price, duration, days));
            }
        }
        return planList;
    }

    private void showPaymentInstructionsDialog(PlanOption plan) {
        if (!isAdded() || getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_payment_instructions, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String price = "2000"; // default price
        if (plan != null) {
            String extractedPrice = plan.price.replaceAll("[^\\d]", "");
            if (!extractedPrice.isEmpty()) {
                price = extractedPrice;
            }
        }

        PaymentUtils.setupPaymentMethods(dialogView, this, price);

        // Show the selected plan's price prominently in the dialog header.
        TextView tvPaymentAmount = dialogView.findViewById(R.id.tv_payment_amount);
        if (tvPaymentAmount != null) {
            String formatted;
            try {
                formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(Long.parseLong(price));
            } catch (NumberFormatException e) {
                formatted = price;
            }
            tvPaymentAmount.setText(getString(R.string.amount_with_currency_format, formatted, "RWF"));
        }

        Button btnDone = dialogView.findViewById(R.id.btn_done);
        btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        // Keep the dialog within the screen so every method stays reachable.
        PaymentUtils.capDialogHeight(dialog, 0.8f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PaymentUtils.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tests, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_toggle_layout);
        if (item == null) return;
        Drawable icon;
        if (isGridLayout) {
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_list);
        } else {
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_grid);
        }

        if (icon != null) {
            // Use the theme's toolbar icon color so the toggle stays visible in dark mode
            Drawable mutableIcon = icon.mutate();
            TypedValue typedValue = new TypedValue();
            if (requireContext().getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true)) {
                mutableIcon.setColorFilter(typedValue.data, PorterDuff.Mode.SRC_ATOP);
                item.setIcon(mutableIcon);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_layout) {
            isGridLayout = !isGridLayout;
            appPreferences.setGridLayout(isGridLayout);
            SafetyUtils.runIfActivityAttached(this, "onOptionsItemSelected",
                    () -> getActivity().invalidateOptionsMenu());
            updateLayoutManager();
            if (testAdapter.getItemCount() > 0) {
                testAdapter.notifyItemRangeChanged(0, testAdapter.getItemCount());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class PlanOption {
        String price;
        String duration;
        int days;

        PlanOption(String price, String duration, int days) {
            this.price = price;
            this.duration = duration;
            this.days = days;
        }
    }

    private class SubscriptionPlanAdapter extends RecyclerView.Adapter<SubscriptionPlanAdapter.ViewHolder> {
        private final List<PlanOption> plans;
        private final OnPlanSelectedListener listener;
        private int selectedPosition = -1;

        SubscriptionPlanAdapter(List<PlanOption> plans, OnPlanSelectedListener listener) {
            this.plans = plans;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subscription_plan, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlanOption plan = plans.get(position);
            holder.tvPrice.setText(plan.price);
            holder.tvDuration.setText(plan.duration);

            boolean isSelected = selectedPosition == position;
            holder.cardView.setStrokeColor(isSelected ? ContextCompat.getColor(getContext(), R.color.my_primary) : Color.TRANSPARENT);
            holder.cardView.setCardElevation(isSelected ? 8 : 2);

            holder.itemView.setOnClickListener(v -> {
                int clickedPosition = holder.getBindingAdapterPosition();
                // Guard against detached/animating rows: getBindingAdapterPosition()
                // returns NO_POSITION (-1), and notifyItemChanged(-1) would throw
                // IndexOutOfBoundsException ("Inconsistency detected. Invalid item
                // position -1") — a real crash Google Play flagged in this adapter.
                if (clickedPosition == RecyclerView.NO_POSITION) return;

                int previousSelected = selectedPosition;
                selectedPosition = clickedPosition;
                // On the first selection previousSelected is NO_POSITION (-1); only
                // rebind the previously selected row when there actually was one.
                if (previousSelected != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelected);
                }
                notifyItemChanged(selectedPosition);
                listener.onPlanSelected(plans.get(clickedPosition));
            });
        }

        @Override
        public int getItemCount() {
            return plans.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPrice, tvDuration;
            MaterialCardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                cardView = (MaterialCardView) itemView;
            }
        }
    }

    interface OnPlanSelectedListener {
        void onPlanSelected(PlanOption plan);
    }
}
