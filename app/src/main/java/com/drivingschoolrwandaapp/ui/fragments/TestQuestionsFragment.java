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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
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
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestQuestionsFragment extends Fragment {

    private TestViewModel testViewModel;
    private ViewPager2 viewPager;
    private TestQuestionPagerAdapter pagerAdapter;
    private TextView timerTextView;
    private TextView questionCounterTextView;
    private Button previousButton, nextButton,  submitButton;
    private ProgressBar progressBar;
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
        questionCounterTextView = view.findViewById(R.id.question_counter_text_view);
        previousButton = view.findViewById(R.id.previous_button);
        nextButton = view.findViewById(R.id.next_button);
        submitButton = view.findViewById(R.id.submit_button);
        errorLayout = view.findViewById(R.id.error_layout);
        retryButton = view.findViewById(R.id.retry_button);
        errorTextView = view.findViewById(R.id.error_text_view);

        progressBar = view.findViewById(R.id.progress_bar);

        pagerAdapter = new TestQuestionPagerAdapter(this, isReviewMode, isRealTimeFeedback, isFree);
        viewPager.setAdapter(pagerAdapter);

        if (isReviewMode) {
            previousButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            timerTextView.setVisibility(View.GONE);

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
            progressBar.setVisibility(View.VISIBLE);
            testViewModel.loadQuestionsForTest(testId);
        });
    }



    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Clear the menu first, in case the previous fragment had items
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
            questionCounterTextView.setText(String.format(Locale.getDefault(), "%d/%d", current, total));
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
                progressBar.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }

            if (resource.data != null && resource.data.questions != null && !resource.data.questions.isEmpty()) {
                errorLayout.setVisibility(View.GONE);
                Test test = TestMapper.INSTANCE.toModel(resource.data);
                if (test != null && test.getQuestions() != null) {
                    pagerAdapter.setQuestions(test.getQuestions());

                    updateQuestionCounter(viewPager.getCurrentItem());
                    updateNavigationButtons(viewPager.getCurrentItem());

                    if (!isReviewMode) {
                        startTimer(test.getDuration());
                    }
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (resource.data == null || resource.data.questions == null || resource.data.questions.isEmpty()) {
                    // Show full screen error with retry
                    errorLayout.setVisibility(View.VISIBLE);
                    errorTextView.setText(resource.message);
                } else {
                    // Show toast if we have data but still got an error (e.g. background refresh failed)
                    Toast.makeText(getContext(), resource.message, Toast.LENGTH_LONG).show();
                }
            } else if (resource.status != Resource.Status.LOADING) {
                if (resource.data == null || resource.data.questions == null || resource.data.questions.isEmpty()) {
                     // Empty state could also use error layout or a specific empty state view
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
        long durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes);
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("00:00");
                if (isAdded()) { // Check if fragment is still attached
                    Toast.makeText(getContext(), getString(R.string.time_up), Toast.LENGTH_SHORT).show();
                    submitTest();
                }
            }
        }.start();
    }

    private void confirmSubmission() {
        submitTest();
    }

    private void submitTest() {
        if (timer != null) {
            timer.cancel();
        }
        testViewModel.calculateResult();
        if (isAdded()) { // Check if fragment is still attached
            NavHostFragment.findNavController(this).navigate(R.id.action_testQuestionsFragment_to_testResultFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
        }
        // Restore the original title when leaving the fragment
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.title_tests);
            }
        }
    }
}
