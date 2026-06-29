package com.drivingschoolrwandaapp.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.drivingschoolrwandaapp.models.entities.TestQuestion;
import com.drivingschoolrwandaapp.ui.fragments.SingleQuestionPageFragment;

import java.util.ArrayList;
import java.util.List;

public class TestQuestionPagerAdapter extends FragmentStateAdapter {

    private List<TestQuestion> questions = new ArrayList<>();
    private final boolean isReviewMode;
    private final boolean isRealTimeFeedback;
    private final boolean isFree;

    public TestQuestionPagerAdapter(@NonNull Fragment fragment, boolean isReviewMode, boolean isRealTimeFeedback, boolean isFree) {
        super(fragment);
        this.isReviewMode = isReviewMode;
        this.isRealTimeFeedback = isRealTimeFeedback;
        this.isFree = isFree;
    }

    public List<TestQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<TestQuestion> questions) {
        int oldSize = getItemCount();
        this.questions = questions;
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
    public Fragment createFragment(int position) {
        return SingleQuestionPageFragment.newInstance(questions.get(position), isReviewMode, isRealTimeFeedback, isFree);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }
}
