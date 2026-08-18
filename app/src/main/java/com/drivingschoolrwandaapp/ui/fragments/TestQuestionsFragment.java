package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.core.content.ContextCompat;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.models.entities.Test;
import com.drivingschoolrwandaapp.models.entities.TestQuestion;
import com.drivingschoolrwandaapp.models.mappers.TestMapper;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.TestQuestionPagerAdapter;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestQuestionsFragment extends Fragment {

    private TestViewModel testViewModel;
    private ViewPager2 viewPager;
    private TestQuestionPagerAdapter pagerAdapter;
    private TextView timerTextView;
    private com.google.android.material.card.MaterialCardView timerContainer;
    private TextView questionCounterTextView;
    private TextView progressTextView;
    private ProgressBar progressBar;
    private ProgressBar progressBarLoading;
    private Button previousButton, nextButton, submitButton;
    private LinearLayout errorLayout;
    private Button retryButton;
    private TextView errorTextView;
    private CountDownTimer timer;
    private int testId;
    private boolean isReviewMode;
    private boolean isRealTimeFeedback;
    private boolean isFree;
    private String title;
    private AppPreferences appPreferences;
    private int totalQuestions = 0;
    // Guards against duplicate submission: the countdown timer's onFinish() and a
    // user tap on the Submit button can race at the same moment. Without this,
    // calculateResult() would run twice — saving two history entries and
    // navigating to the result screen twice.
    private boolean isSubmitting = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        testViewModel = new ViewModelProvider(requireActivity()).get(TestViewModel.class);
        if (getArguments() != null) {
            testId = getArguments().getInt("testId");
            isReviewMode = getArguments().getBoolean("isReviewMode");
            isRealTimeFeedback = getArguments().getBoolean("isRealTimeFeedback", false);
            isFree = getArguments().getBoolean("isFree", false);
            title = getArguments().getString("title");
        }
        appPreferences = new AppPreferences(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test_questions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null && title != null) {
                actionBar.setTitle(title);
            }
        }

        viewPager = view.findViewById(R.id.question_view_pager);
        timerTextView = view.findViewById(R.id.timer_text_view);
        timerContainer = view.findViewById(R.id.timer_container);
        questionCounterTextView = view.findViewById(R.id.question_counter_text_view);
        progressTextView = view.findViewById(R.id.progress_text_view);
        progressBar = view.findViewById(R.id.progress_bar);
        progressBarLoading = view.findViewById(R.id.progress_bar_loading);
        previousButton = view.findViewById(R.id.previous_button);
        nextButton = view.findViewById(R.id.next_button);
        submitButton = view.findViewById(R.id.submit_button);
        errorLayout = view.findViewById(R.id.error_layout);
        retryButton = view.findViewById(R.id.retry_button);
        errorTextView = view.findViewById(R.id.error_text_view);

        pagerAdapter = new TestQuestionPagerAdapter(this, isReviewMode, isRealTimeFeedback, isFree);
        viewPager.setAdapter(pagerAdapter);

        if (isReviewMode) {
            previousButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            timerTextView.setVisibility(View.GONE);
            if (timerContainer != null) timerContainer.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);

        } else {
            submitButton.setVisibility(View.VISIBLE);
            setupClickListeners();
            submitButton.setOnClickListener(v -> confirmSubmission());
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int lastPosition = 0;

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Hide feedback for the previous question when swiping
                if (position != lastPosition) {
                    if (pagerAdapter != null && pagerAdapter.getQuestions() != null && lastPosition < pagerAdapter.getQuestions().size()) {
                        TestQuestion previousQuestion = pagerAdapter.getQuestions().get(lastPosition);
                        if (previousQuestion != null) {
                            testViewModel.markQuestionFeedbackHidden(previousQuestion.getId());
                        }
                    }
                }
                lastPosition = position;

                updateQuestionCounter(position);
                updateProgress(position);
                updateNavigationButtons(position);
            }
        });
        
        setupRetryButton();

        observeViewModel();
        testViewModel.loadQuestionsForTest(testId);
    }
    
    private void setupRetryButton() {
        retryButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            progressBarLoading.setVisibility(View.VISIBLE);
            testViewModel.loadQuestionsForTest(testId);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavHostFragment.findNavController(this).popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateQuestionCounter(int position) {
        if (pagerAdapter != null && pagerAdapter.getItemCount() > 0) {
            int current = position + 1;
            int total = pagerAdapter.getItemCount();
            questionCounterTextView.setText(getString(R.string.question_counter_format, current, total));
        }
    }

    private void updateProgress(int position) {
        if (pagerAdapter != null && pagerAdapter.getItemCount() > 0) {
            int current = position + 1;
            int total = pagerAdapter.getItemCount();
            int percentage = (current * 100) / total;
            progressBar.setProgress(percentage);
            progressTextView.setText(getString(R.string.percent_complete_format, percentage));
        }
    }

    private void updateNavigationButtons(int position) {
        // No longer need to invalidate menu
    }

    private void setupClickListeners() {
        previousButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1, true);
            }
        });

        nextButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (pagerAdapter.getQuestions() != null && !pagerAdapter.getQuestions().isEmpty()) {
                TestQuestion currentQuestion = pagerAdapter.getQuestions().get(currentPosition);
                testViewModel.markQuestionFeedbackHidden(currentQuestion.getId());
            }

            if (currentPosition < pagerAdapter.getItemCount() - 1) {
                viewPager.setCurrentItem(currentPosition + 1, true);
            }
        });
    }

    private void observeViewModel() {
        testViewModel.getQuestionsForTest().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.status == Resource.Status.LOADING) {
                progressBarLoading.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
            } else {
                progressBarLoading.setVisibility(View.GONE);
            }

            if (resource.data != null && resource.data.questions != null && !resource.data.questions.isEmpty()) {
                errorLayout.setVisibility(View.GONE);
                // Store test data for reliable calculateResult()
                testViewModel.storeTestData(resource.data);
                Test test = TestMapper.INSTANCE.toModel(resource.data);
                if (test != null && test.getQuestions() != null) {
                    pagerAdapter.setQuestions(test.getQuestions());
                    totalQuestions = test.getQuestions().size();

                    updateQuestionCounter(viewPager.getCurrentItem());
                    updateProgress(viewPager.getCurrentItem());
                    updateNavigationButtons(viewPager.getCurrentItem());

                    if (isReviewMode) {
                        // Restore the user's previously selected answers for review
                        testViewModel.restoreReviewAnswers();
                    } else {
                        // Start the elapsed-time clock together with the countdown timer
                        testViewModel.markTestStarted();
                        AnalyticsUtils.logExamStarted(getContext(),
                                testViewModel.getTestId() != null ? testViewModel.getTestId() : 0,
                                testViewModel.getCurrentTestNumber(),
                                testViewModel.getCurrentTestName());
                        startTimer(test.getDuration());
                    }
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (resource.data == null || resource.data.questions == null || resource.data.questions.isEmpty()) {
                    errorLayout.setVisibility(View.VISIBLE);
                    errorTextView.setText(resource.message);
                } else {
                    Toast.makeText(getContext(), resource.message, Toast.LENGTH_LONG).show();
                }
            } else if (resource.status != Resource.Status.LOADING) {
                if (resource.data == null || resource.data.questions == null || resource.data.questions.isEmpty()) {
                     errorLayout.setVisibility(View.VISIBLE);
                     errorTextView.setText(getString(R.string.no_questions_found));
                }
            }
        });
    }

    private void startTimer(int durationMinutes) {
        if (timer != null) {
            timer.cancel();
        }
        if (durationMinutes <= 0) {
            // No time limit configured: never run a countdown (a CountDownTimer with 0
            // millis fires onFinish immediately and would auto-submit the test) and hide
            // the timer so the header doesn't show an empty pill.
            if (timerContainer != null) timerContainer.setVisibility(View.GONE);
            timerTextView.setText(R.string.timer_placeholder);
            return;
        }
        if (timerContainer != null) timerContainer.setVisibility(View.VISIBLE);
        long durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes);
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded()) return;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                timerTextView.setText(getString(R.string.timer_format, minutes, seconds));

                // Warn when less than 5 minutes remain
                if (millisUntilFinished < TimeUnit.MINUTES.toMillis(5)) {
                    timerTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFinish() {
                if (!isAdded()) return;
                timerTextView.setText(R.string.timer_placeholder);
                Toast.makeText(getContext(), getString(R.string.time_up), Toast.LENGTH_SHORT).show();
                submitTest();
            }
        }.start();
    }

    private void confirmSubmission() {
        if (isSubmitting) return;
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_submission))
                .setMessage(getString(R.string.confirm_submission_message))
                .setPositiveButton(getString(R.string.submit), (dialog, which) -> submitTest())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void submitTest() {
        // First call wins; the timer finishing at the same moment the user taps
        // Submit must not calculate/persist/navigate twice.
        if (isSubmitting) return;
        isSubmitting = true;
        if (timer != null) {
            timer.cancel();
        }
        testViewModel.calculateResult();
        if (isAdded()) {
            NavHostFragment.findNavController(this).navigate(R.id.action_testQuestionsFragment_to_testResultFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
        }
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.title_tests);
            }
        }
    }
}
