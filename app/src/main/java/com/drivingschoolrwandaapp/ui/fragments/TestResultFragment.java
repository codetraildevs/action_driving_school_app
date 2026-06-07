package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestResultFragment extends Fragment {

    private TestViewModel testViewModel;
    private TextView resultTitleTextView;
    private TextView resultScoreTextView;
    private ImageView resultIconImageView;

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
        resultIconImageView = view.findViewById(R.id.result_icon_image_view);

        finishButton = view.findViewById(R.id.finish_button);

        observeViewModel();

        finishButton.setOnClickListener(v -> {
            // Navigate back to the tests list
            NavHostFragment.findNavController(this).popBackStack(R.id.testsFragment, false);
        });
    }

    private void observeViewModel() {
        testViewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isPassed()) {
                    resultTitleTextView.setText(getString(R.string.test_passed));
                    resultIconImageView.setImageResource(R.drawable.ic_check_circle);
                    
                    TypedValue typedValue = new TypedValue();
                    requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                    resultIconImageView.setColorFilter(typedValue.data);
                } else {
                    resultTitleTextView.setText(getString(R.string.test_failed));
                    resultIconImageView.setImageResource(R.drawable.ic_error);
                    
                    TypedValue typedValue = new TypedValue();
                    requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true);
                    resultIconImageView.setColorFilter(typedValue.data);
                }
                resultScoreTextView.setText(String.format(getString(R.string.score_format), result.getScore(), result.getTotalMarks()));
            }
        });
    }
}
