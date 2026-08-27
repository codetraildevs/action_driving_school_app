package com.drivingschoolrwandaapp.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.activities.ApplicationDetailsActivity;
import com.drivingschoolrwandaapp.ui.activities.IremboActivity;
import com.drivingschoolrwandaapp.ui.activities.WhatsAppGroupsActivity;
import com.drivingschoolrwandaapp.utils.AdManager;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.utils.LanguageUtils;

import android.widget.FrameLayout;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private UserViewModel userViewModel;
    private IremboViewModel iremboViewModel;


    private AlertDialog loadingDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsUtils.logScreenView(getContext(), "dashboard");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Quick Access Cards
        MaterialCardView startExamCard = view.findViewById(R.id.start_exam_card);
        if (startExamCard != null) {
            startExamCard.setOnClickListener(v -> {
                // Show rewarded ad before starting exam for bonus access
                if (getActivity() != null && AdManager.isRewardedAdReady()) {
                    AdManager.showRewardedAdIfReady(getActivity(), new AdManager.RewardedAdCallback() {
                        @Override
                        public void onRewardEarned(@NonNull com.google.android.gms.ads.rewarded.RewardItem reward) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), getString(R.string.dsrw_ad_reward_msg), Toast.LENGTH_LONG).show();
                                    NavHostFragment.findNavController(DashboardFragment.this)
                                            .navigate(R.id.action_dashboardFragment_to_testsFragment);
                                });
                            }
                        }

                        @Override
                        public void onAdFailedToShow() {
                            if (isAdded()) {
                                NavHostFragment.findNavController(DashboardFragment.this)
                                        .navigate(R.id.action_dashboardFragment_to_testsFragment);
                            }
                        }
                    });
                } else {
                    NavHostFragment.findNavController(this).navigate(R.id.action_dashboardFragment_to_testsFragment);
                }
            });
        }

        // The "Tests effectués" subtitle inside the Exams card opens the test history.
        android.widget.TextView previousTestsSubtitle = view.findViewById(R.id.previous_tests_subtitle);
        if (previousTestsSubtitle != null) {
            previousTestsSubtitle.setOnClickListener(v -> {
                if (isAdded()) {
                    NavHostFragment.findNavController(DashboardFragment.this)
                            .navigate(R.id.action_dashboardFragment_to_resultsFragment);
                }
            });
        }

        MaterialCardView learningMaterialsCard = view.findViewById(R.id.learning_materials_card);
        if (learningMaterialsCard != null) {
            learningMaterialsCard.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigate(R.id.action_dashboardFragment_to_materialsFragment)
            );
        }

        MaterialCardView iremboServiceCard = view.findViewById(R.id.irembo_service_card);
        if (iremboServiceCard != null) {
            iremboServiceCard.setOnClickListener(v -> {
                if (!isAdded() || getActivity() == null) return;
                // Show interstitial before Irembo if ready
                if (AdManager.showInterstitialIfReady(getActivity())) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getActivity() != null) {
                            startActivity(new Intent(getActivity(), IremboActivity.class));
                        }
                    }, 500);
                } else {
                    startActivity(new Intent(getActivity(), IremboActivity.class));
                }
            });
        }

        MaterialCardView whatsappGroupCard = view.findViewById(R.id.whatsapp_group_card);
        if (whatsappGroupCard != null) {
            whatsappGroupCard.setOnClickListener(v -> {
                if (!isAdded() || getActivity() == null) return;
                // Show interstitial before WhatsApp groups if ready
                if (AdManager.showInterstitialIfReady(getActivity())) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getContext() != null) {
                            startActivity(new Intent(getContext(), WhatsAppGroupsActivity.class));
                        }
                    }, 500);
                } else {
                    startActivity(new Intent(requireContext(), WhatsAppGroupsActivity.class));
                }
            });
        }

        MaterialCardView instructionsCard = view.findViewById(R.id.instructions_card);
        if (instructionsCard != null) {
            instructionsCard.setOnClickListener(v -> showInstructionsDialog());
        }

        MaterialCardView profileCard = view.findViewById(R.id.profile_card);
        if (profileCard != null) {
            profileCard.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigate(R.id.action_dashboardFragment_to_profileFragment)
            );
        }

        setupMenu();
        observeViewModels();
        userViewModel.loadProfile();

        // Load AdMob banner
        FrameLayout adContainer = view.findViewById(R.id.ad_container);
        if (adContainer != null && getActivity() != null) {
            AdManager.showBanner(getActivity(), adContainer, null);
            // Pre-load interstitial for exam submission
            AdManager.loadInterstitial(getActivity());
            // Pre-load rewarded ad for free exam access
            AdManager.loadRewardedAd(getActivity());
        }
    }

    private void observeViewModels() {

        iremboViewModel.getApplicationDetails().observe(getViewLifecycleOwner(), resource -> {
             if (resource.status == Resource.Status.LOADING) {
                 showLoadingDialog();
             } else if (resource.status == Resource.Status.SUCCESS) {
                 hideLoadingDialog();
                 if (resource.data != null && isAdded()) {
                     Intent intent = new Intent(requireContext(), ApplicationDetailsActivity.class);
                     intent.putExtra("application_details", resource.data);
                     startActivity(intent);
                 }
             } else if (resource.status == Resource.Status.ERROR) {
                 hideLoadingDialog();
                 if (isAdded()) {
                     Toast.makeText(requireContext(), getString(R.string.error_format, resource.message), Toast.LENGTH_LONG).show();
                 }
             }
         });
    }


    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.dashboard_menu, menu);
                MenuItem instructionsItem = menu.findItem(R.id.action_instructions);
                if (instructionsItem != null && instructionsItem.getActionView() instanceof MaterialButton) {
                    MaterialButton instructionsButton = (MaterialButton) instructionsItem.getActionView();
                    instructionsButton.setOnClickListener(v -> showInstructionsDialog());
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_instructions) {
                    showInstructionsDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showInstructionsDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_instructions, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnGotIt = dialogView.findViewById(R.id.btn_got_it);
        if (btnGotIt != null) {
            btnGotIt.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
    
    private void showLoadingDialog() {
        if (loadingDialog == null && getContext() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(getContext()));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        if (loadingDialog != null) {
            loadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
