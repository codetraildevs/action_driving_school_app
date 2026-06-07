package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.utils.LanguageUtils;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private UserViewModel userViewModel;
    private TextView profileName;
    private TextView profileEmail;
    private ShapeableImageView profileImage;

    private TextView tvAccessLevel;
    private TextView tvExpiryDate;
    private TextView tvPendingMessage;

    private ProgressBar progressBar;
    private AppPreferences appPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        appPreferences = new AppPreferences(requireContext());
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

        progressBar = view.findViewById(R.id.progress_bar);

        setupMenu();
        observeViewModels();
        userViewModel.loadProfile();

        view.findViewById(R.id.change_language_button).setOnClickListener(v -> LanguageUtils.showLanguageDialog(requireContext()));
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
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        progressBar.setVisibility(View.VISIBLE);

                        if (resource.getData() != null) {
                            updateUserProfile(resource.getData());
                        }
                        break;
                    case SUCCESS:
                        progressBar.setVisibility(View.GONE);
                        if (resource.getData() != null) {
                            updateUserProfile(resource.getData());
                        }
                        break;
                    case ERROR:
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void updateUserProfile(User user) {
        profileName.setText(user.getFirstName() + " " + user.getLastName() + "("+user.getLanguage()+")");
        profileEmail.setText(user.getPhoneNumber());
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
             tvAccessLevel.setText(getString(R.string.accass_up_to)+ maxTest);
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
            tvExpiryDate.setText(getString(R.string.expiry_date) + formatDate(expiry));
        } else {
            tvExpiryDate.setVisibility(View.GONE);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr; 
        } catch (Exception e) {
             return dateStr;
        }
    }
}
