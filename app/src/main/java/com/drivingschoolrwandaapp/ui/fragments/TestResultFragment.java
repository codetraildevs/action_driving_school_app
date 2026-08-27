package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.utils.AdManager;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.drivingschoolrwandaapp.utils.TimeFormatUtils;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestResultFragment extends Fragment {

    private TestViewModel testViewModel;
    private TextView resultTitleTextView;
    private TextView resultScoreTextView;
    private TextView resultScorePercentage;
    private TextView resultStatusText;
    private TextView resultStatusScore;
    private ImageView resultIconImageView;
    private com.google.android.material.card.MaterialCardView scoreCircleContainer;
    private Button finishButton;
    private com.google.android.material.card.MaterialCardView timeCard;
    private com.google.android.material.card.MaterialCardView breakdownCard;
    private TextView resultTimeText;
    private TextView resultCorrectCount;
    private TextView resultWrongCount;
    private TextView resultSkippedCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testViewModel = new ViewModelProvider(requireActivity()).get(TestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resultTitleTextView = view.findViewById(R.id.result_title_text_view);
        resultScoreTextView = view.findViewById(R.id.result_score_text_view);
        resultScorePercentage = view.findViewById(R.id.result_score_percentage);
        resultStatusText = view.findViewById(R.id.result_status_text);
        resultStatusScore = view.findViewById(R.id.result_status_score);
        resultIconImageView = view.findViewById(R.id.result_icon_image_view);
        scoreCircleContainer = view.findViewById(R.id.score_circle_container);
        finishButton = view.findViewById(R.id.finish_button);
        timeCard = view.findViewById(R.id.time_card);
        breakdownCard = view.findViewById(R.id.breakdown_card);
        resultTimeText = view.findViewById(R.id.result_time_text);
        resultCorrectCount = view.findViewById(R.id.result_correct_count);
        resultWrongCount = view.findViewById(R.id.result_wrong_count);
        resultSkippedCount = view.findViewById(R.id.result_skipped_count);

        observeViewModel();

        // Load AdMob banner
        android.widget.FrameLayout adContainer = view.findViewById(R.id.ad_container);
        if (adContainer != null && getActivity() != null) {
            AdManager.showBanner(getActivity(), adContainer, null);
        }

        // Show interstitial after exam completion (natural transition point)
        if (getActivity() != null) {
            AdManager.loadInterstitial(requireContext());
            AdManager.showInterstitialIfReady(getActivity());
        }

        finishButton.setOnClickListener(v -> {
            // Show rewarded ad before returning to tests (free retake opportunity)
            if (getActivity() != null && AdManager.isRewardedAdReady()) {
                AdManager.showRewardedAdIfReady(getActivity(), new AdManager.RewardedAdCallback() {
                    @Override
                    public void onRewardEarned(@NonNull com.google.android.gms.ads.rewarded.RewardItem reward) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), getString(R.string.dsrw_ad_reward_msg), Toast.LENGTH_LONG).show();
                                NavHostFragment.findNavController(TestResultFragment.this)
                                        .popBackStack(R.id.testsFragment, false);
                            });
                        }
                    }

                    @Override
                    public void onAdFailedToShow() {
                        if (isAdded()) {
                            NavHostFragment.findNavController(TestResultFragment.this)
                                    .popBackStack(R.id.testsFragment, false);
                        }
                    }
                });
            } else {
                NavHostFragment.findNavController(this).popBackStack(R.id.testsFragment, false);
            }
        });
    }

    private void observeViewModel() {
        // Show exam name in action bar
        if (!isAdded() || getActivity() == null) return;
        int testNumber = testViewModel.getCurrentTestNumber();
        if (testNumber > 0) {
            try {
                String examTitle = getString(R.string.exam_number_format, testNumber);
                if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                    androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setTitle(examTitle);
                    }
                }
            } catch (Exception e) {
                Log.e("TestResultFragment", "Failed to set action bar title", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        
        testViewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                AnalyticsUtils.logExamCompleted(
                        getContext(),
                        testViewModel.getTestId() != null ? testViewModel.getTestId() : 0,
                        testViewModel.getCurrentTestNumber(),
                        testViewModel.getCurrentTestName(),
                        result.getScore(),
                        result.getTotalMarks(),
                        result.isPassed(),
                        result.getCorrectCount(),
                        result.getWrongCount(),
                        result.getSkippedCount(),
                        result.getElapsedSeconds());

                int percentage = (result.getTotalMarks() > 0)
                        ? (result.getScore() * 100) / result.getTotalMarks()
                        : 0;

                if (result.isPassed()) {
                    resultTitleTextView.setText(getString(R.string.test_passed));
                    resultIconImageView.setImageResource(R.drawable.ic_check_circle);
                    resultIconImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.correct_answer_green));
                    scoreCircleContainer.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.correct_answer_green));
                    resultStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_answer_green));
                } else {
                    resultTitleTextView.setText(getString(R.string.test_failed));
                    resultIconImageView.setImageResource(R.drawable.ic_error);
                    resultIconImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.incorrect_answer_red));
                    scoreCircleContainer.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.incorrect_answer_red));
                    resultStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.incorrect_answer_red));
                }

                resultScorePercentage.setText(getString(R.string.percentage_score, percentage));
                resultScoreTextView.setText(getString(R.string.score_details, result.getScore(), result.getTotalMarks()));
                resultStatusText.setText(result.isPassed() ? getString(R.string.result_passed) : getString(R.string.result_failed));
                resultStatusScore.setText(String.valueOf(result.getScore()));

                // Time taken vs the test's duration limit
                int elapsed = result.getElapsedSeconds();
                int duration = result.getDuration();
                if (elapsed > 0 && duration > 0) {
                    resultTimeText.setText(getString(R.string.result_time_format,
                            TimeFormatUtils.formatElapsed(elapsed), duration));
                    timeCard.setVisibility(View.VISIBLE);
                } else if (elapsed > 0) {
                    resultTimeText.setText(TimeFormatUtils.formatElapsed(elapsed));
                    timeCard.setVisibility(View.VISIBLE);
                } else if (duration > 0) {
                    resultTimeText.setText(getString(R.string.result_duration_format, duration));
                    timeCard.setVisibility(View.VISIBLE);
                } else {
                    timeCard.setVisibility(View.GONE);
                }

                // Score breakdown: correct / wrong / skipped
                resultCorrectCount.setText(String.valueOf(result.getCorrectCount()));
                resultWrongCount.setText(String.valueOf(result.getWrongCount()));
                resultSkippedCount.setText(String.valueOf(result.getSkippedCount()));
                breakdownCard.setVisibility(View.VISIBLE);
            } else {
                // Fallback: if no result yet, show placeholder
                resultTitleTextView.setText(getString(R.string.title_test_result));
                resultScorePercentage.setText(R.string.result_placeholder_percentage);
                resultScoreTextView.setText(R.string.result_placeholder_score);
                resultStatusText.setText(R.string.result_placeholder_status);
                resultStatusScore.setText(R.string.result_placeholder_zero);
                timeCard.setVisibility(View.GONE);
                breakdownCard.setVisibility(View.GONE);
            }
        });
    }
}
