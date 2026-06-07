package com.drivingschoolrwandaapp.ui.adapters;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.entities.TestTranslation;
import com.drivingschoolrwandaapp.repository.Resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.TestViewHolder> {

    private List<TestEntity> tests = new ArrayList<>();
    private User currentUser;
    private OnTestClickListener onTestClickListener;
    public int currentLanguageId = 1;
    private boolean isGrid = false;
    private Map<Integer, Resource.Status> downloadStatuses = new HashMap<>();

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public interface OnTestClickListener {
        void onTestClick(TestEntity test, boolean isLocked, String title);
    }

    public TestAdapter(boolean isGrid) {
        this.isGrid = isGrid;
    }

    public void setGridLayout(boolean isGrid) {
        this.isGrid = isGrid;
    }

    @Override
    public int getItemViewType(int position) {
        return isGrid ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_GRID) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_grid, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
        }
        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        TestEntity test = tests.get(position);
        holder.bind(test);
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    public void setTests(List<TestEntity> tests) {
        // Sort tests by testNumber
        Collections.sort(tests, new Comparator<TestEntity>() {
            @Override
            public int compare(TestEntity t1, TestEntity t2) {
                return Integer.compare(t1.getTestNumber(), t2.getTestNumber());
            }
        });
        this.tests = tests;
        notifyDataSetChanged();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        notifyDataSetChanged();
    }

    public void setLanguage(String languageCode) {
        switch (languageCode) {
            case "en":
                currentLanguageId = 41;
                break;
            case "fr":
                currentLanguageId = 48;
                break;
            case "rw":
            default:
                currentLanguageId = 1;
                break;
        }
        notifyDataSetChanged();
    }

    public void setOnTestClickListener(OnTestClickListener listener) {
        this.onTestClickListener = listener;
    }

    public void setDownloadStatus(int testId, Resource.Status status) {
        downloadStatuses.put(testId, status);
        for (int i = 0; i < tests.size(); i++) {
            if (tests.get(i).getId() == testId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public boolean isTestLocked(TestEntity test) {
        if (test.isFree()) {
            return false;
        }

        if (currentUser == null) {
            return true;
        }

        if (!"ACTIVE".equalsIgnoreCase(currentUser.getTestAccessStatus())) {
            return true;
        }

        boolean isExpired = true;
        if (currentUser.getTestAccessExpiresAt() != null) {
            try {
                // Try parsing ISO 8601 format (e.g., 2025-12-27T16:20:29.814Z)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date expirationDate = sdf.parse(currentUser.getTestAccessExpiresAt());
                if (expirationDate != null && !expirationDate.before(new Date())) {
                    isExpired = false; // Not expired
                }
            } catch (ParseException e) {
                Log.e("TestAdapter", "Error parsing date ISO8601: " + currentUser.getTestAccessExpiresAt(), e);
                // Fallback to simple format
                try {
                    SimpleDateFormat sdfFallback = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date expirationDate = sdfFallback.parse(currentUser.getTestAccessExpiresAt());
                    if (expirationDate != null && !expirationDate.before(new Date())) {
                        isExpired = false;
                    }
                } catch (ParseException e2) {
                    Log.e("TestAdapter", "Error parsing date Fallback: " + currentUser.getTestAccessExpiresAt(), e2);
                    // isExpired remains true, so it's locked
                }
            }
        }

        if (isExpired) {
            return true;
        }

        return test.getTestNumber() > currentUser.getMaxTestAccess();
    }

    class TestViewHolder extends RecyclerView.ViewHolder {
        private ImageView testImage;
        private TextView badgeFree;
        private ImageView lockIcon;
        private TextView testTitle;
        private ProgressBar downloadProgressBar;

        public TestViewHolder(@NonNull View itemView) {
            super(itemView);
            testImage = itemView.findViewById(R.id.test_image);
            badgeFree = itemView.findViewById(R.id.badge_free);
            lockIcon = itemView.findViewById(R.id.lock_icon);
            testTitle = itemView.findViewById(R.id.test_title);
            downloadProgressBar = itemView.findViewById(R.id.download_progress_bar);

            itemView.setOnClickListener(v -> handleTestClick());
        }

        private void handleTestClick() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && onTestClickListener != null) {
                TestEntity test = tests.get(position);
                boolean isLocked = isTestLocked(test);
                onTestClickListener.onTestClick(test, isLocked, testTitle.getText().toString());
            }
        }

        void bind(TestEntity test) {
            String displayTitle = test.getTitle();
            String displayDescription = test.getDescription();
            String displayImageUrl = test.getImageUrl();

            // Check for translations
            if (test.getTestTranslations() != null) {
                for (TestTranslation translation : test.getTestTranslations()) {
                    if (translation.getLanguageId() == currentLanguageId) {
                        if (translation.getTitle() != null && !translation.getTitle().isEmpty()) {
                            displayTitle = translation.getTitle();
                        }
                        if (translation.getDescription() != null && !translation.getDescription().isEmpty()) {
                            displayDescription = translation.getDescription();
                        }
                        if (translation.getImageUrl() != null && !translation.getImageUrl().isEmpty()) {
                            displayImageUrl = translation.getImageUrl();
                        }
                        break;
                    }
                }
            }


            String prefix = test.getTestNumber() + ": ";
            String fullTitle =  displayTitle;
            SpannableString spannableTitle = new SpannableString(fullTitle);
            spannableTitle.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            testTitle.setText(spannableTitle);

            // Load Image
            if (displayImageUrl != null && !displayImageUrl.isEmpty() && !displayImageUrl.matches("\\d+")) {
                String imageUrl = displayImageUrl;
                // Skip SITE_URL prefix for local asset URIs (file://) and http/https URLs
                if (!imageUrl.startsWith("http") && !imageUrl.startsWith("file://")) {
                     if (!imageUrl.startsWith("/" + "/")) {
                         imageUrl = "/" + imageUrl;
                     }
                     imageUrl = ApiClient.SITE_URL + imageUrl;
                }
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.logo_notification)
                        .error(R.drawable.logo_notification)
                        .into(testImage);
            } else {
                testImage.setImageResource(R.drawable.logo_notification);
            }

            boolean isLocked = isTestLocked(test);

            // Badge and Lock logic
            if (test.isFree()) {
                badgeFree.setVisibility(View.VISIBLE);
                lockIcon.setVisibility(View.GONE);
            } else {
                badgeFree.setVisibility(View.GONE);
                lockIcon.setVisibility(View.VISIBLE);
                if (isLocked) {
                    lockIcon.setImageResource(R.drawable.lock_svgrepo_com);
                    lockIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.incorrect_answer_red));
                } else {
                    lockIcon.setImageResource(R.drawable.lock_unlocked_svgrepo_com);
                    lockIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.correct_answer_green));
                }
            }

            // Download Status
            Resource.Status status = downloadStatuses.get(test.getId());
            if (status == Resource.Status.LOADING) {
                downloadProgressBar.setVisibility(View.VISIBLE);
                lockIcon.setVisibility(View.GONE);
            } else {
                downloadProgressBar.setVisibility(View.GONE);
                if (!test.isFree()) {
                    lockIcon.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
