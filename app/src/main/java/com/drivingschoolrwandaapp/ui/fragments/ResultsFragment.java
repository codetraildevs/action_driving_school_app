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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.TestResult;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;

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
    private TestResultAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_results, container, false);
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

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TestResultAdapter();
        resultsRecyclerView.setAdapter(adapter);

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
        averageScoreValue.setText(String.format(Locale.getDefault(), "%d%%", avgPercentage));
        passRateValue.setText(String.format(Locale.getDefault(), "%d%%", passRatePercentage));

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
            private final TextView testNameText;
            private final TextView scoreText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                statusIcon = itemView.findViewById(R.id.result_status_icon);
                testNameText = itemView.findViewById(R.id.result_test_name);
                scoreText = itemView.findViewById(R.id.result_score_text);
                // Hide date field if it exists (not in our layout)
                TextView dateText = itemView.findViewById(R.id.result_date);
                if (dateText != null) dateText.setVisibility(View.GONE);
            }

            void bind(TestResult result) {
                // Display exam number clearly
                String displayName;
                if (result.getTestNumber() > 0) {
                    displayName = getString(R.string.exam_number_format, result.getTestNumber());
                } else {
                    displayName = result.getTestName();
                }
                testNameText.setText(displayName);

                // Score
                scoreText.setText(String.format(Locale.getDefault(), "%d/%d", result.getScore(), result.getTotalMarks()));

                // Status icon
                if (result.isPassed()) {
                    statusIcon.setImageResource(R.drawable.ic_check_circle);
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_error);
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                }
            }
        }
    }
}
