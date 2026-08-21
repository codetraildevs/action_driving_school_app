package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.utils.AdManager;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.utils.LanguageUtils;
import com.drivingschoolrwandaapp.utils.RoleUtils;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProfileFragment extends Fragment {

    private UserViewModel userViewModel;
    private TextView profileName;
    private TextView profileEmail;
    private ShapeableImageView profileImage;

    private TextView tvAccessLevel;
    private TextView tvExpiryDate;
    private TextView tvPendingMessage;
    private TextView tvSessionStatus;
    private TextView profileRoleBadge;

    private ProgressBar progressBar;
    private AppPreferences appPreferences;
    private TokenManager tokenManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        appPreferences = new AppPreferences(requireContext());
        tokenManager = new TokenManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        profileImage = view.findViewById(R.id.profile_image);

        tvAccessLevel = view.findViewById(R.id.tv_access_level);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        tvPendingMessage = view.findViewById(R.id.tv_pending_message);
        tvSessionStatus = view.findViewById(R.id.tv_session_status);
        profileRoleBadge = view.findViewById(R.id.profile_role_badge);

        progressBar = view.findViewById(R.id.progress_bar);

        setupMenu();
        observeViewModels();
        userViewModel.loadProfile();

        // Display session expiry info
        updateSessionInfo();

        // Load AdMob banner
        android.widget.FrameLayout adContainer = view.findViewById(R.id.ad_container);
        if (adContainer != null && getActivity() != null) {
            AdManager.showBanner(getActivity(), adContainer, null);
        }

        View changeLanguageButton = view.findViewById(R.id.change_language_button);
        if (changeLanguageButton != null) {
            changeLanguageButton.setOnClickListener(v -> {
                if (isAdded() && getContext() != null) {
                    LanguageUtils.showLanguageDialog(requireContext());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh session expiry display when user returns to this tab
        updateSessionInfo();
        AnalyticsUtils.logScreenView(getContext(), "profile");
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.profile_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_logout) {
                    userViewModel.logout();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void observeViewModels() {
        userViewModel.getUserLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || !isAdded()) return;
            switch (resource.getStatus()) {
                case LOADING:
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

                    if (resource.getData() != null) {
                        updateUserProfile(resource.getData());
                    }
                    break;
                case SUCCESS:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (resource.getData() != null) {
                        updateUserProfile(resource.getData());
                    }
                    break;
                case ERROR:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    // When offline (or the server is unreachable) the profile shown is the
                    // cached copy from the local database. Only bother the user with an
                    // error toast when there is genuinely nothing to display — otherwise the
                    // refresh failure is expected and the saved content is still useful.
                    if (resource.getData() == null && isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
    }

    private void updateUserProfile(User user) {
        if (!isAdded() || getContext() == null) return;
        
        profileName.setText(getString(R.string.user_name_format, user.getFirstName(), user.getLastName(), user.getLanguage()));
        profileEmail.setText(user.getPhoneNumber());

        // Show the user's role as a badge under the name (hidden for unknown roles).
        // Admins get the primary-tinted badge, everyone else the neutral one.
        if (profileRoleBadge != null) {
            if (user.getRoleId() > 0) {
                profileRoleBadge.setText(getString(RoleUtils.getRoleNameRes(user.getRoleId())));
                if (RoleUtils.isAdminRole(user.getRoleId())) {
                    profileRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.my_primary));
                    profileRoleBadge.setBackgroundResource(R.drawable.bg_badge_admin);
                } else {
                    profileRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
                    profileRoleBadge.setBackgroundResource(R.drawable.bg_badge);
                }
                profileRoleBadge.setVisibility(View.VISIBLE);
            } else {
                profileRoleBadge.setVisibility(View.GONE);
            }
        }
        Glide.with(this)
                .load(user.getProfilePicture())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImage);
                
        // Update Access Status
        int maxTest = user.getMaxTestAccess();
        String status = user.getTestAccessStatus();
        String expiry = user.getTestAccessExpiresAt();

        if (maxTest > 0) {
             tvAccessLevel.setText(getString(R.string.accass_up_to_format, maxTest));
        } else {
             tvAccessLevel.setText(getString(R.string.access_we_have));
        }

        if (status != null) {
            switch (status) {
                case "ACTIVE":
                    tvPendingMessage.setVisibility(View.GONE);
                    break;
                case "PENDING":
                    tvPendingMessage.setText(getString(R.string.pending_message));
                    tvPendingMessage.setVisibility(View.VISIBLE);
                    break;
                case "INACTIVE":
                    tvPendingMessage.setText(getString(R.string.inactive_message));
                    tvPendingMessage.setVisibility(View.VISIBLE);
                    break;
                default:
                    tvPendingMessage.setVisibility(View.GONE);
            }
        } else {
            tvPendingMessage.setVisibility(View.GONE);
        }

        if (expiry != null && !expiry.isEmpty()) {
            tvExpiryDate.setVisibility(View.VISIBLE);
            tvExpiryDate.setText(getString(R.string.expiry_date_format, formatDate(expiry)));
        } else {
            tvExpiryDate.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the session status card with the remaining token expiry time.
     */
    @android.annotation.SuppressLint("SetTextI18n")
    private void updateSessionInfo() {
        if (tokenManager == null || tvSessionStatus == null || !isAdded() || getContext() == null) return;

        long expiryTime = tokenManager.getTokenExpiryTime();
        long now = System.currentTimeMillis();

        if (expiryTime <= 0 || now >= expiryTime) {
            // No token or already expired
            tvSessionStatus.setText(getString(R.string.session_expired));
            tvSessionStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
            return;
        }

        long diffMs = expiryTime - now;
        boolean rememberMe = tokenManager.isRememberMe();

        if (rememberMe) {
            // Show remaining time in days/hours
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;

            if (days > 0) {
                tvSessionStatus.setText(getString(R.string.session_expires_in,
                    days + "d " + hours + "h"));
            } else if (hours > 0) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;
                tvSessionStatus.setText(getString(R.string.session_expires_in,
                    hours + "h " + minutes + "m"));
            } else {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
                tvSessionStatus.setText(getString(R.string.session_expires_in,
                    (minutes > 0 ? minutes + "m" : "<1m")));
            }
            tvSessionStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_green_dark));
        } else {
            // Session-only: show remaining time in minutes
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            if (minutes > 0) {
                tvSessionStatus.setText(getString(R.string.session_expires_in,
                    minutes + "m"));
            } else {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs);
                tvSessionStatus.setText(getString(R.string.session_expires_in,
                    (seconds > 0 ? seconds + "s" : "<1s")));
            }
            tvSessionStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_dark));
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e("ProfileFragment", "Error parsing date: " + dateStr, e);
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            return dateStr; 
        } catch (Exception e) {
            Log.e("ProfileFragment", "Unexpected error parsing date: " + dateStr, e);
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            return dateStr;
        }
    }
}
