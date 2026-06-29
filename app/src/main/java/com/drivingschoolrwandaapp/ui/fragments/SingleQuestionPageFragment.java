package com.drivingschoolrwandaapp.ui.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.models.entities.QuestionTranslation;
import com.drivingschoolrwandaapp.models.entities.TestQuestion;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.QuestionOptionAdapter;
import com.drivingschoolrwandaapp.utils.NetworkUtils;
import com.drivingschoolrwandaapp.viewmodel.TestViewModel;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.gson.Gson;

import java.util.Map;

public class SingleQuestionPageFragment extends Fragment implements QuestionOptionAdapter.OnOptionSelectedListener {

    private static final String ARG_QUESTION = "arg_question";
    private static final String ARG_IS_REVIEW_MODE = "is_review_mode";
    private static final String ARG_IS_REAL_TIME_FEEDBACK = "is_real_time_feedback";
    private static final String ARG_IS_FREE = "is_free";

    private TestQuestion question;
    private boolean isReviewMode;
    private boolean isRealTimeFeedback;
    private boolean isFree;
    private TestViewModel testViewModel;
    private AppPreferences appPreferences;
    private QuestionOptionAdapter adapter;

    public static SingleQuestionPageFragment newInstance(TestQuestion question, boolean isReviewMode, boolean isRealTimeFeedback, boolean isFree) {
        SingleQuestionPageFragment fragment = new SingleQuestionPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION, new Gson().toJson(question));
        args.putBoolean(ARG_IS_REVIEW_MODE, isReviewMode);
        args.putBoolean(ARG_IS_REAL_TIME_FEEDBACK, isRealTimeFeedback);
        args.putBoolean(ARG_IS_FREE, isFree);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String questionJson = getArguments().getString(ARG_QUESTION);
            question = new Gson().fromJson(questionJson, TestQuestion.class);
            isReviewMode = getArguments().getBoolean(ARG_IS_REVIEW_MODE);
            isRealTimeFeedback = getArguments().getBoolean(ARG_IS_REAL_TIME_FEEDBACK);
            isFree = getArguments().getBoolean(ARG_IS_FREE, false);
        }
        testViewModel = new ViewModelProvider(requireActivity()).get(TestViewModel.class);
        appPreferences = new AppPreferences(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_question_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final TextView questionTextView = view.findViewById(R.id.question_text_view);
        final TextView translationWarningTextView = view.findViewById(R.id.translation_warning);
        final View questionImageContainer = view.findViewById(R.id.question_image_container);
        final ImageView questionImageView = view.findViewById(R.id.question_image_view);
        final RecyclerView optionsRecyclerView = view.findViewById(R.id.options_recycler_view);

        final String languageCode = appPreferences.getLanguage();

        UserViewModel userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUserLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null) {
                // Check if data exists, regardless of status (SUCCESS, ERROR, LOADING)
                if (resource.getData() != null) {
                    int userLanguageId = resource.getData().getLanguageId();

                    int preferredLanguageId = getLanguageId(languageCode);
                    boolean languageMatch = userLanguageId == preferredLanguageId;
                    int finalLanguageId = languageMatch ? userLanguageId : 85;

                    if (!isFree && !languageMatch) {
                        // Update local db
                        resource.getData().setTestAccessStatus("PENDING");
                        userViewModel.updateUser(resource.getData());

                        // Send request to server if online
                        if (NetworkUtils.isNetworkAvailable(requireContext())) {
                            userViewModel.sleepSubscription(preferredLanguageId);
                        }

                        // Show warning and navigate back immediately
                        Toast.makeText(getContext(), R.string.translation_warning, Toast.LENGTH_LONG).show();
                        try {
                            Navigation.findNavController(view).popBackStack();
                        } catch (Exception e) {
                            if (getActivity() != null) {
                                getActivity().onBackPressed();
                            }
                        }
                        return; // <- Important: This stops the question from rendering
                    } else {
                        translationWarningTextView.setVisibility(GONE);
                    }

                    String displayQuestionText = question.getQuestionText();
                    String displayImageUrl = question.getImageUrl();

                    if (question.getQuestionTranslations() != null) {
                        for (QuestionTranslation translation : question.getQuestionTranslations()) {
                            if (translation.getLanguageId() == finalLanguageId) {
                                if (translation.getQuestionText() != null && !translation.getQuestionText().isEmpty()) {
                                    displayQuestionText = translation.getQuestionText();
                                }
                                if (translation.getImageUrl() != null && !translation.getImageUrl().isEmpty()) {
                                    displayImageUrl = translation.getImageUrl();
                                }
                                break;
                            }
                        }
                    }

                    if (displayQuestionText != null) {
                        questionTextView.setText(displayQuestionText.trim());
                    } else {
                        questionTextView.setText("");
                    }

                    if (displayImageUrl != null && !displayImageUrl.isEmpty()) {
                        questionImageContainer.setVisibility(VISIBLE);
                        questionImageView.setVisibility(VISIBLE);
                        // Load local asset URIs directly; prepend SITE_URL for relative network paths
                        String imageUrl = displayImageUrl;
                        if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file://")) {
                            imageUrl = ApiClient.SITE_URL + imageUrl;
                        }
                        Glide.with(this)
                                .load(imageUrl)
                                .error(R.drawable.ic_launcher_background)
                                .into(questionImageView);
                    } else {
                        questionImageContainer.setVisibility(GONE);
                        questionImageView.setVisibility(GONE);
                    }

                    optionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                    Integer selectedAnswerId = null;
                    Map<Integer, Integer> answers = testViewModel.getSelectedAnswers().getValue();
                    if (answers != null) {
                        selectedAnswerId = answers.get(question.getId());
                    }

                    boolean showFeedback = isRealTimeFeedback;
                    if (showFeedback && testViewModel.isQuestionFeedbackHidden(question.getId())) {
                        showFeedback = false;
                    }

                    adapter = new QuestionOptionAdapter(question.getOptions(), this, isReviewMode, selectedAnswerId, showFeedback, question.getCorrectOptionId());
                    adapter.setLanguageId(finalLanguageId);
                    optionsRecyclerView.setAdapter(adapter);
                }
                
                // Show error message if needed, but don't block rendering if we had cached data
                if (resource.getStatus() == Resource.Status.ERROR) {
                    // You might want to suppress this if you have data, or show a small "Offline" indicator instead
                     if (resource.getData() == null) {
                         Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                     } else {
                         // Optional: Log error or show a non-blocking UI indication that data might be stale
                     }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && isRealTimeFeedback) {
            // Check if we need to update the feedback state based on hidden status
            // This handles cases where we return to the fragment
            boolean shouldHideFeedback = testViewModel.isQuestionFeedbackHidden(question.getId());
            adapter.setRealTimeFeedback(!shouldHideFeedback);
        }
    }

    @Override
    public void onOptionSelected(int optionId) {
        if (!isReviewMode) {
            testViewModel.setAnswer(question.getId(), optionId);
            
            // If real-time feedback is enabled generally, show it now that the user has selected an option
            if (isRealTimeFeedback && adapter != null) {
                adapter.setRealTimeFeedback(true);
            }
        }
    }

    private int getLanguageId(String languageCode) {
        switch (languageCode) {
            case "en":
                return 41;
            case "fr":
                return 48;
            case "rw":
                return 85;
            default:
                return 85;
        }
    }
}
