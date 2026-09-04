package com.drivingschoolrwandaapp.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.util.ArrayList;
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

    private static final String[] OPTION_LABELS = {"A", "B", "C", "D", "E", "F"};

    public interface OnOptionSelectedListener {
        void onOptionSelected(int optionId);
    }

    public QuestionOptionAdapter(List<QuestionOption> options, OnOptionSelectedListener listener, boolean isReviewMode, Integer selectedAnswerId, boolean isRealTimeFeedback, int correctOptionId) {
        this.options = options != null ? options : new ArrayList<>();
        this.listener = listener;
        this.isReviewMode = isReviewMode;
        this.selectedAnswerId = selectedAnswerId;
        this.isRealTimeFeedback = isRealTimeFeedback;

        int realCorrectOptionId = correctOptionId;
        if (this.options != null) {
            for (QuestionOption option : this.options) {
                if (option.isCorrect()) {
                    realCorrectOptionId = option.getId();
                    break;
                }
            }
        }
        this.correctOptionId = realCorrectOptionId;

        if (this.selectedAnswerId != null) {
            for (int i = 0; i < this.options.size(); i++) {
                if (this.options.get(i).getId() == this.selectedAnswerId) {
                    this.selectedPosition = i;
                    break;
                }
            }
        }
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
        if (!options.isEmpty()) {
            notifyItemRangeChanged(0, options.size());
        }
    }

    public void setRealTimeFeedback(boolean realTimeFeedback) {
        this.isRealTimeFeedback = realTimeFeedback;
        if (!options.isEmpty()) {
            notifyItemRangeChanged(0, options.size());
        }
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
        TextView optionIndicator;
        TextView optionText;
        ImageView optionImage;
        ImageView checkIcon;

        public QuestionOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            optionIndicator = itemView.findViewById(R.id.option_indicator);
            optionText = itemView.findViewById(R.id.option_text_view);
            optionImage = itemView.findViewById(R.id.option_image_view);
            checkIcon = itemView.findViewById(R.id.option_check_icon);

            itemView.setOnClickListener(v -> {
                if (isReviewMode) return;
                int position = getAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                int previousSelectedPosition = selectedPosition;
                selectedPosition = position;

                if (listener != null && position < options.size()) {
                    listener.onOptionSelected(options.get(position).getId());
                }

                if (isRealTimeFeedback) {
                    if (!options.isEmpty()) {
                        notifyItemRangeChanged(0, options.size());
                    }
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
                optionText.setText(null);
            }

            // Set option indicator letter
            if (position < OPTION_LABELS.length) {
                optionIndicator.setText(OPTION_LABELS[position]);
            }

            // Load option image
            String imageUrl = option.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                optionImage.setVisibility(View.VISIBLE);
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

            boolean isSelected = position == selectedPosition;

            if (isReviewMode) {
                boolean wasSelectedByUser = selectedAnswerId != null && selectedAnswerId == option.getId();
                
                // Reset to default state
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorSurface));
                cardView.setStrokeColor(Color.TRANSPARENT);
                optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.colorSurfaceVariant));
                optionIndicator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnSurface));
                checkIcon.setVisibility(View.GONE);

                if (option.isCorrect()) {
                    // Correct answer - green highlight
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryContainer));
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                    optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.correct_answer_green));
                    optionIndicator.setTextColor(Color.WHITE);
                } else if (wasSelectedByUser) {
                    // User's wrong selection - red highlight
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                    optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.incorrect_answer_red));
                    optionIndicator.setTextColor(Color.WHITE);
                }
            } else {
                // Normal mode
                if (isSelected) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryContainer));
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.my_primary));
                    optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.my_primary));
                    optionIndicator.setTextColor(Color.WHITE);
                    checkIcon.setVisibility(View.VISIBLE);
                    checkIcon.setImageResource(R.drawable.ic_check_circle_small);
                    checkIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.my_primary));
                } else {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorSurface));
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOutlineVariant));
                    optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.colorSurfaceVariant));
                    optionIndicator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorOnSurfaceVariant));
                    checkIcon.setVisibility(View.GONE);
                }

                // Real-time feedback overrides
                if (isRealTimeFeedback && selectedPosition != -1) {
                    boolean selectionIsCorrect = options.get(selectedPosition).getId() == correctOptionId;
                    if (selectionIsCorrect) {
                        if (isSelected) {
                            cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryContainer));
                            cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                            optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.correct_answer_green));
                            optionIndicator.setTextColor(Color.WHITE);
                            checkIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                        }
                    } else {
                        if (isSelected) {
                            cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorErrorContainer));
                            cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                            optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.incorrect_answer_red));
                            optionIndicator.setTextColor(Color.WHITE);
                            checkIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                        }
                        if (option.getId() == correctOptionId) {
                            cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                            optionIndicator.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.correct_answer_green));
                            optionIndicator.setTextColor(Color.WHITE);
                        }
                    }
                }
            }
        }
    }
}
