package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
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
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import java.util.Locale;

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

        observeViewModel();

        finishButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack(R.id.testsFragment, false);
        });
    }

    private void observeViewModel() {
        // Show exam name in action bar
        int testNumber = testViewModel.getCurrentTestNumber();
        if (testNumber > 0) {
            try {
                String examTitle = String.format(Locale.getDefault(), getString(R.string.exam_number_format), testNumber);
                androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) requireActivity()).getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(examTitle);
                }
            } catch (Exception e) {
                Log.e("TestResultFragment", "Failed to set action bar title", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        
        testViewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
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

                resultScorePercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
                resultScoreTextView.setText(String.format(Locale.getDefault(), "%d / %d", result.getScore(), result.getTotalMarks()));
                resultStatusText.setText(result.isPassed() ? getString(R.string.result_passed) : getString(R.string.result_failed));
                resultStatusScore.setText(String.valueOf(result.getScore()));
            } else {
                // Fallback: if no result yet, show placeholder
                resultTitleTextView.setText(getString(R.string.title_test_result));
                resultScorePercentage.setText("--%");
                resultScoreTextView.setText("-- / --");
                resultStatusText.setText("--");
                resultStatusScore.setText("0");
            }
        });
    }
}
