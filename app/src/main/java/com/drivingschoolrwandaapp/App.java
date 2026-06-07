package com.drivingschoolrwandaapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.ui.activities.WhatsAppGroupsActivity;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class App extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private UserViewModel userViewModel;
    private DrawerLayout drawerLayout;
    private AppUpdateManager appUpdateManager;
    private static final int APP_UPDATE_REQUEST_CODE = 1991;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notifications will not be shown.", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_app);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        navController = navHostFragment.getNavController();

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.dashboardFragment);
        
//        topLevelDestinations.add(R.id.testsFragment);
//        topLevelDestinations.add(R.id.materialsFragment);
//        topLevelDestinations.add(R.id.profileFragment);

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavView, navController);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Explicitly handle selection to ensure Home always works and bar shows up
        bottomNavView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.dashboardFragment) {
                if (navController.getCurrentDestination().getId() != R.id.dashboardFragment) {
                    navController.popBackStack(R.id.dashboardFragment, false);
                }
                showBottomNav(bottomNavView);
                return true;
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                showBottomNav(bottomNavView);
            }
            return handled;
        });

        // Sync selection and visibility state
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();

            // Sync bottom nav checkmark
            Menu menu = bottomNavView.getMenu();
            MenuItem menuItem = menu.findItem(destId);
            if (menuItem != null) {
                menuItem.setChecked(true);
            }

            if (destId == R.id.testQuestionsFragment) {
                hideBottomNav(bottomNavView);
            } else {
                showBottomNav(bottomNavView);
            }
        });

        userViewModel.getUserLiveData().observe(this, resource -> {
            if (resource != null && resource.data != null) {
                updateNavHeader(resource.data);
            }
        });

        userViewModel.loadProfile();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_share) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, ApiClient.PLAYSTORE_LINK);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                drawerLayout.close();
                return true;
            } else if (item.getItemId() == R.id.nav_whatsapp) {
                Intent i = new Intent(App.this, WhatsAppGroupsActivity.class);
                startActivity(i);
                drawerLayout.close();
                return true;
            } else if (item.getItemId() == R.id.nav_about_us) {
                showAboutUsDialog();
                drawerLayout.close();
                return true;
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawerLayout.close();
            }
            return handled;
        });

        navigationView.getMenu().findItem(R.id.delete_account_item).setOnMenuItemClickListener(menuItem -> {
            showDeleteAccountConfirmation();
            drawerLayout.close();
            return true;
        });

        askNotificationPermission();

        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.registerListener(installStateUpdatedListener);
        checkForUpdate();
    }

    private void checkForUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            APP_UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private final InstallStateUpdatedListener installStateUpdatedListener = installState -> {
        if (installState.installStatus() == InstallStatus.DOWNLOADED) {
            showSnackbarForCompleteUpdate();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                showSnackbarForCompleteUpdate();
                            }
                        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    private void showSnackbarForCompleteUpdate() {
        Snackbar.make(
                findViewById(R.id.drawer_layout), // Use a root view of your layout
                "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("RESTART", view -> appUpdateManager.completeUpdate())
                .show();
    }

    private void showBottomNav(BottomNavigationView view) {
        view.setVisibility(View.VISIBLE);
        view.animate().cancel();
        view.setTranslationY(0f);

        // Use a slight delay to allow layout to settle and skip any initial
        // scroll events triggered by fragment view restoration.
        view.postDelayed(() -> {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
                if (behavior instanceof HideBottomViewOnScrollBehavior) {
                    ((HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior).slideUp(view);
                }
            }
        }, 100);
    }

    private void hideBottomNav(BottomNavigationView view) {
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (behavior instanceof HideBottomViewOnScrollBehavior) {
                ((HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior).slideDown(view);
            }
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Already granted
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("This app uses notifications to alert you about important updates. Please grant the permission to receive them.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        })
                        .setNegativeButton("No, thanks", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showAboutUsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_about_us, null);
        // inflate about us version with the one generated by app

        // inflate about us version with the one generated by app
        TextView versionTv = dialogView.findViewById(R.id.versionTv);
        versionTv.setText("Version " + BuildConfig.VERSION_NAME);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnClose = dialogView.findViewById(R.id.btn_close_about);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteAccountConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_delete_account, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_delete);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            userViewModel.deleteAccount();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // Log or show a toast that the update was cancelled or failed.
                Toast.makeText(this, "Update flow failed! Result code: " + resultCode, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void updateNavHeader(User user) {
        NavigationView navigationView = findViewById(R.id.navigation_view);
        View headerView = navigationView.getHeaderView(0);

        TextView tvUserName = headerView.findViewById(R.id.drawer_user_name);
        TextView tvUserEmail = headerView.findViewById(R.id.drawer_user_email);
        ShapeableImageView ivProfile = headerView.findViewById(R.id.drawer_profile_image);

        if(user != null){
            tvUserName.setText(user.getFirstName() + " " + user.getLastName() + "("+user.getLanguage()+")");
            tvUserEmail.setText(user.getPhoneNumber());

            Glide.with(this)
                    .load(user.getProfilePicture())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile);
        }
    }
}
