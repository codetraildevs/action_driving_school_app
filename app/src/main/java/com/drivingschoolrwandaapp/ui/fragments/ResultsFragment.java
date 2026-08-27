package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.TestResult;
import com.drivingschoolrwandaapp.utils.AdManager;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.utils.TimeFormatUtils;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResultsFragment extends Fragment {

    private TestViewModel testViewModel;
    private LinearLayout summaryContainer;
    private RecyclerView resultsRecyclerView;
    private LinearLayout emptyState;
    private TextView totalTestsValue;
    private TextView averageScoreValue;
    private TextView passRateValue;
    private TextView historyHeader;
    private TestResultAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_results, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsUtils.logScreenView(getContext(), "results");

        // Show interstitial when user views results (natural transition point)
        if (getActivity() != null) {
            AdManager.loadInterstitial(requireContext());
            AdManager.showInterstitialIfReady(getActivity());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        testViewModel = new ViewModelProvider(requireActivity()).get(TestViewModel.class);

        summaryContainer = view.findViewById(R.id.summary_container);
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        totalTestsValue = view.findViewById(R.id.total_tests_value);
        averageScoreValue = view.findViewById(R.id.average_score_value);
        passRateValue = view.findViewById(R.id.pass_rate_value);
        historyHeader = view.findViewById(R.id.history_header);

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TestResultAdapter();
        resultsRecyclerView.setAdapter(adapter);

        // Wire up the Start Practice button in the empty state
        com.google.android.material.button.MaterialButton startPracticeBtn = view.findViewById(R.id.start_practice_btn);
        if (startPracticeBtn != null) {
            startPracticeBtn.setOnClickListener(v -> {
                if (getActivity() != null && isAdded()) {
                    NavHostFragment.findNavController(ResultsFragment.this)
                            .navigate(R.id.testsFragment);
                }
            });
        }

        // Observe test result history
        testViewModel.getTestResultHistory().observe(getViewLifecycleOwner(), history -> {
            if (history != null && !history.isEmpty()) {
                showResults(history);
            } else {
                showEmptyState();
            }
        });
    }

    private void showResults(List<TestResult> history) {
        emptyState.setVisibility(View.GONE);
        summaryContainer.setVisibility(View.VISIBLE);
        resultsRecyclerView.setVisibility(View.VISIBLE);

        // Load AdMob banner only when there are results (policy: no ads on empty screens)
        android.widget.FrameLayout adContainer = getView() != null ? getView().findViewById(R.id.ad_container) : null;
        if (adContainer != null && getActivity() != null) {
            AdManager.showBanner(getActivity(), adContainer, null);
        }
        if (historyHeader != null) {
            historyHeader.setVisibility(View.VISIBLE);
        }

        // Calculate summary stats
        int totalTests = history.size();
        int totalScore = 0;
        int totalMarks = 0;
        int passedCount = 0;

        for (TestResult r : history) {
            totalScore += r.getScore();
            totalMarks += r.getTotalMarks();
            if (r.isPassed()) passedCount++;
        }

        int avgPercentage = totalMarks > 0 ? (totalScore * 100) / totalMarks : 0;
        int passRatePercentage = (passedCount * 100) / totalTests;

        totalTestsValue.setText(String.valueOf(totalTests));
        averageScoreValue.setText(getString(R.string.percentage_score, avgPercentage));
        passRateValue.setText(getString(R.string.percentage_score, passRatePercentage));

        adapter.setResults(history);
    }

    private void showEmptyState() {
        summaryContainer.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Adapter for displaying test result history items with exam numbers.
     */
    private class TestResultAdapter extends RecyclerView.Adapter<TestResultAdapter.ViewHolder> {

        private List<TestResult> results = java.util.Collections.emptyList();

        public void setResults(List<TestResult> results) {
            int oldSize = this.results.size();
            this.results = results;
            if (oldSize > 0) {
                notifyItemRangeRemoved(0, oldSize);
            }
            int newSize = getItemCount();
            if (newSize > 0) {
                notifyItemRangeInserted(0, newSize);
            }
        }

        /**
         * Formats the completion timestamp. The formatter is intentionally created per call:
         * the app has an in-app language switcher, so a static field would capture a stale
         * Locale.getDefault() and trip lint's ConstantLocale check.
         */
        private String formatDate(long dateMillis) {
            return new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
                    .format(new Date(dateMillis));
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TestResult result = results.get(position);
            holder.bind(result);
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.ImageView statusIcon;
            private final com.google.android.material.card.MaterialCardView statusIconContainer;
            private final TextView testNameText;
            private final TextView scoreText;
            private final TextView dateText;
            private final TextView durationText;
            private final com.google.android.material.button.MaterialButton btnRetake;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                statusIcon = itemView.findViewById(R.id.result_status_icon);
                statusIconContainer = itemView.findViewById(R.id.status_icon_container);
                testNameText = itemView.findViewById(R.id.result_test_name);
                scoreText = itemView.findViewById(R.id.result_score_text);
                btnRetake = itemView.findViewById(R.id.btn_retake_test);
                dateText = itemView.findViewById(R.id.result_date);
                durationText = itemView.findViewById(R.id.result_duration);
            }

            void bind(TestResult result) {
                // Display exam number clearly
                String displayName;
                if (result.getTestNumber() > 0) {
                    displayName = getString(R.string.exam_number_format, result.getTestNumber());
                } else {
                    String testName = result.getTestName();
                    displayName = testName != null ? testName : "";
                }
                testNameText.setText(displayName);

                // Score
                scoreText.setText(getString(R.string.compact_score_format, result.getScore(), result.getTotalMarks()));

                // Completion date
                long dateMillis = result.getDate();
                if (dateMillis > 0) {
                    dateText.setText(formatDate(dateMillis));
                    dateText.setVisibility(View.VISIBLE);
                } else {
                    dateText.setVisibility(View.GONE);
                }

                // Time taken vs the test's duration limit
                int elapsed = result.getElapsedSeconds();
                int duration = result.getDuration();
                if (elapsed > 0 && duration > 0) {
                    durationText.setText(getString(R.string.result_time_format, TimeFormatUtils.formatElapsed(elapsed), duration));
                    durationText.setVisibility(View.VISIBLE);
                } else if (elapsed > 0) {
                    durationText.setText(TimeFormatUtils.formatElapsed(elapsed));
                    durationText.setVisibility(View.VISIBLE);
                } else if (duration > 0) {
                    durationText.setText(getString(R.string.result_duration_format, duration));
                    durationText.setVisibility(View.VISIBLE);
                } else {
                    durationText.setVisibility(View.GONE);
                }

                // Status icon and container
                if (result.isPassed()) {
                    statusIcon.setImageResource(R.drawable.ic_check_circle);
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                    statusIconContainer.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryContainer));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_error);
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                    statusIconContainer.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.colorErrorContainer));
                }

                // Show the Retake action and wire it up (Review was removed for a compact card)
                final int testId = result.getTestId();
                final String title = result.getTestName() != null && !result.getTestName().isEmpty()
                        ? result.getTestName() : getString(R.string.exam_number_format, result.getTestNumber());

                if (testId > 0 && btnRetake != null) {
                    btnRetake.setVisibility(View.VISIBLE);
                    btnRetake.setOnClickListener(v -> {
                        // Show rewarded ad before retaking test for free access
                        if (getActivity() != null && AdManager.isRewardedAdReady()) {
                            AdManager.showRewardedAdIfReady(getActivity(), new AdManager.RewardedAdCallback() {
                                @Override
                                public void onRewardEarned(@NonNull com.google.android.gms.ads.rewarded.RewardItem reward) {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            Bundle args = new Bundle();
                                            args.putInt("testId", testId);
                                            args.putString("title", title);
                                            args.putBoolean("isReviewMode", false);
                                            args.putBoolean("isRealTimeFeedback", true);
                                            NavHostFragment.findNavController(ResultsFragment.this)
                                                    .navigate(R.id.action_global_testQuestionsFragment, args);
                                        });
                                    }
                                }

                                @Override
                                public void onAdFailedToShow() {
                                    if (isAdded()) {
                                        Bundle args = new Bundle();
                                        args.putInt("testId", testId);
                                        args.putString("title", title);
                                        args.putBoolean("isReviewMode", false);
                                        args.putBoolean("isRealTimeFeedback", true);
                                        NavHostFragment.findNavController(ResultsFragment.this)
                                                .navigate(R.id.action_global_testQuestionsFragment, args);
                                    }
                                }
                            });
                        } else {
                            if (isAdded()) {
                                Bundle args = new Bundle();
                                args.putInt("testId", testId);
                                args.putString("title", title);
                                args.putBoolean("isReviewMode", false);
                                args.putBoolean("isRealTimeFeedback", true);
                                NavHostFragment.findNavController(ResultsFragment.this)
                                        .navigate(R.id.action_global_testQuestionsFragment, args);
                            }
                        }
                    });
                } else if (btnRetake != null) {
                    btnRetake.setVisibility(View.GONE);
                }
            }
        }
    }
}
