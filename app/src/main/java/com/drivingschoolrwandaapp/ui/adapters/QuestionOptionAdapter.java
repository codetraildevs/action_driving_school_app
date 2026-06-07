package com.drivingschoolrwandaapp.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.models.entities.QuestionOption;
import com.drivingschoolrwandaapp.models.entities.QuestionOptionTranslation;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.List;

public class QuestionOptionAdapter extends RecyclerView.Adapter<QuestionOptionAdapter.QuestionOptionViewHolder> {

    private List<QuestionOption> options;
    private int selectedPosition = -1;
    private final boolean isReviewMode;
    private final Integer selectedAnswerId;
    private boolean isRealTimeFeedback;
    private final int correctOptionId;
    private OnOptionSelectedListener listener;
    private int languageId = 41;

    public interface OnOptionSelectedListener {
        void onOptionSelected(int optionId);
    }

    public QuestionOptionAdapter(List<QuestionOption> options, OnOptionSelectedListener listener, boolean isReviewMode, Integer selectedAnswerId, boolean isRealTimeFeedback, int correctOptionId) {
        this.options = options;
        this.listener = listener;
        this.isReviewMode = isReviewMode;
        this.selectedAnswerId = selectedAnswerId;
        this.isRealTimeFeedback = isRealTimeFeedback;

        int realCorrectOptionId = correctOptionId;
        if (options != null) {
            for (QuestionOption option : options) {
                if (option.isCorrect()) {
                    realCorrectOptionId = option.getId();
                    break;
                }
            }
        }
        this.correctOptionId = realCorrectOptionId;

        if (this.selectedAnswerId != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId() == this.selectedAnswerId) {
                    this.selectedPosition = i;
                    break;
                }
            }
        }
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
        notifyDataSetChanged();
    }

    public void setRealTimeFeedback(boolean realTimeFeedback) {
        this.isRealTimeFeedback = realTimeFeedback;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_option, parent, false);
        return new QuestionOptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionOptionViewHolder holder, int position) {
        holder.bind(options.get(position), position);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    class QuestionOptionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        RadioButton radioButton;
        TextView optionText;
        ImageView optionImage;

        public QuestionOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            radioButton = itemView.findViewById(R.id.option_radio_button);
            optionText = itemView.findViewById(R.id.option_text_view);
            optionImage = itemView.findViewById(R.id.option_image_view);

            itemView.setOnClickListener(v -> {
                if (isReviewMode) return;
                int position = getAdapterPosition();


                if (position == RecyclerView.NO_POSITION) {
                    return;
                }


                int previousSelectedPosition = selectedPosition;
                selectedPosition = getAdapterPosition();

                if (listener != null) {
                    listener.onOptionSelected(options.get(selectedPosition).getId());
                }

                if (isRealTimeFeedback) {
                    notifyDataSetChanged();
                } else {
                    if (previousSelectedPosition != -1) {
                        notifyItemChanged(previousSelectedPosition);
                    }
                    notifyItemChanged(selectedPosition);
                }
            });
        }

        void bind(QuestionOption option, int position) {
            String textToDisplay = option.getText();

            if (option.getQuestionOptionTranslations() != null) {
                for (QuestionOptionTranslation translation : option.getQuestionOptionTranslations()) {
                    if (translation.getLanguageId() == languageId) {
                        textToDisplay = translation.getText();
                        break;
                    }
                }
            }
            
            if (textToDisplay != null) {
                optionText.setText(textToDisplay.trim());
            } else {
                optionText.setText("");
            }

            String imageUrl = option.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                optionImage.setVisibility(View.VISIBLE);
                // Skip SITE_URL prefix for local asset URIs (file://) and http/https URLs
                if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file://")) {
                    if (!imageUrl.startsWith("/")) {
                        imageUrl = "/" + imageUrl;
                    }
                    imageUrl = ApiClient.SITE_URL + imageUrl;
                }
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(optionImage);
            } else {
                optionImage.setVisibility(View.GONE);
            }

            if (isReviewMode) {
                radioButton.setChecked(selectedAnswerId != null && selectedAnswerId == option.getId());
                cardView.setStrokeColor(Color.TRANSPARENT);
                if (option.isCorrect()) {
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                } else if (selectedAnswerId != null && selectedAnswerId == option.getId()) {
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                }
            } else {
                radioButton.setChecked(position == selectedPosition);

                if (isRealTimeFeedback) {
                    cardView.setStrokeColor(Color.TRANSPARENT);
                    if (selectedPosition != -1) { // an item is selected
                        boolean selectionIsCorrect = options.get(selectedPosition).getId() == correctOptionId;
                        if (selectionIsCorrect) {
                            if (position == selectedPosition) {
                                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                            }
                        } else { // selection is incorrect
                            if (position == selectedPosition) { // the selected, incorrect item
                                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                            }
                            if (option.getId() == correctOptionId) { // the correct item
                                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                            }
                        }
                    }
                } else { // not real time feedback, just highlight selection
                    int defaultColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOutlineVariant);
                    cardView.setStrokeColor(defaultColor);
                    radioButton.setChecked(false);
                }
            }
        }
    }
}
