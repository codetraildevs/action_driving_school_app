package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.drivingschoolrwandaapp.BuildConfig;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.ui.adapters.WelcomeCarouselAdapter;
import com.drivingschoolrwandaapp.utils.InsetsUtils;
import com.drivingschoolrwandaapp.utils.LanguageUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {
    private AppPreferences appPreferences;
    private ViewPager2 viewPager2;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    TextView versionTv;
    private int carouselSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeUtils.enable(this);

        setContentView(R.layout.activity_welcome);
        appPreferences = new AppPreferences(this);

        // Apply system bar insets to the root view to prevent content from being clipped
        // behind the transparent status bar in edge-to-edge mode.
        InsetsUtils.applySystemBarsPadding(findViewById(android.R.id.content), true, true);

        setupButtons();
        setupCarousel();

        TextView termsPolicyText = findViewById(R.id.terms_policy_text);
        versionTv = findViewById(R.id.versionTv);
        versionTv.setText(getString(R.string.version_format,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        makeTermsAndPolicyClickable(termsPolicyText);

        // Show disclaimer only on first launch
        if (!appPreferences.isDisclaimerShown()) {
            appPreferences.setDisclaimerShown(true);
            showSupportDialog();
        }
    }

    private void setupButtons() {
        Button registerButton = findViewById(R.id.register_button);
        Button loginButton = findViewById(R.id.login_button);
        Button instructionsButton = findViewById(R.id.instructions_button);
        View changeLanguageButton = findViewById(R.id.change_language_button);

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        instructionsButton.setOnClickListener(v -> showInstructionsDialog());
        changeLanguageButton.setOnClickListener(v -> LanguageUtils.showLanguageDialog(WelcomeActivity.this));
    }

    private void setupCarousel() {
        viewPager2 = findViewById(R.id.welcome_carousel);
        TabLayout tabLayout = findViewById(R.id.carousel_indicator);

        List<WelcomeCarouselAdapter.CarouselItem> items = new ArrayList<>();
        items.add(new WelcomeCarouselAdapter.CarouselItem(R.drawable.handshake, getString(R.string.comprehensive_studies)));
        items.add(new WelcomeCarouselAdapter.CarouselItem(R.drawable.learn, getString(R.string.practice_text)));
        items.add(new WelcomeCarouselAdapter.CarouselItem(R.drawable.exam, getString(R.string.review_text)));
        items.add(new WelcomeCarouselAdapter.CarouselItem(R.drawable.success, getString(R.string.review_text)));
        items.add(new WelcomeCarouselAdapter.CarouselItem(R.drawable.certificate, getString(R.string.review_text)));
        carouselSize = items.size();
        WelcomeCarouselAdapter adapter = new WelcomeCarouselAdapter(items);
        viewPager2.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {}).attach();

        // Auto-scroll logic
        runnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = viewPager2.getCurrentItem();
                int nextItem = (currentItem + 1) % carouselSize;
                viewPager2.setCurrentItem(nextItem, true);
                handler.postDelayed(this, 5000); // 3 seconds interval
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (runnable != null) {
            handler.postDelayed(runnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    private void showInstructionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_instructions, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnGotIt = dialogView.findViewById(R.id.btn_got_it);
        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showSupportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_support, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnGotIt = dialogView.findViewById(R.id.btn_got_it);
        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void makeTermsAndPolicyClickable(TextView textView) {
        final String languageCode = appPreferences.getLanguage();
        String text = getString(R.string.by_continuing_you_agree_to_our_terms_of_service_and_privacy_policy);
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openUrl(ApiClient.SITE_URL + "/terms-of-service" + "?language=" + languageCode, getString(R.string.terms_of_service));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(WelcomeActivity.this, R.color.my_primary));
                ds.setUnderlineText(false);
            }
        };

        ClickableSpan policySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openUrl(ApiClient.SITE_URL + "/privacy-policy"+ "?language=" + languageCode, getString(R.string.privacy_policy));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(WelcomeActivity.this, R.color.my_primary));
                ds.setUnderlineText(false);
            }
        };

        String termsString = getString(R.string.terms_of_service);
        String policyString = getString(R.string.privacy_policy);
        int termsStart = text.indexOf(termsString);
        int termsEnd = termsStart + termsString.length();
        int policyStart = text.indexOf(policyString);
        int policyEnd = policyStart + policyString.length();

        if(termsStart != -1) {
            spannableString.setSpan(termsSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if(policyStart != -1) {
            spannableString.setSpan(policySpan, policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void openUrl(String url, String title) {
        Intent intent = new Intent(WelcomeActivity.this, WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        startActivity(intent);
    }
}
