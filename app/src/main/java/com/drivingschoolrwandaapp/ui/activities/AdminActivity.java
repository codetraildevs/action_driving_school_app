package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.ui.fragments.ProfileFragment;
import com.drivingschoolrwandaapp.ui.fragments.admin.AdminDashboardFragment;
import com.drivingschoolrwandaapp.ui.fragments.admin.AdminRequestsFragment;
import com.drivingschoolrwandaapp.ui.fragments.admin.AdminSettingsFragment;
import com.drivingschoolrwandaapp.ui.fragments.admin.AdminUsersFragment;
import com.drivingschoolrwandaapp.utils.InsetsUtils;
import com.drivingschoolrwandaapp.utils.RoleUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Native admin console shell. Only reachable by users with an admin role
 * (see {@link com.drivingschoolrwandaapp.utils.RoleUtils}); regular users are
 * routed to {@link App} instead.
 */
@AndroidEntryPoint
public class AdminActivity extends AppCompatActivity {

    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_USERS = 1;
    private static final int TAB_REQUESTS = 2;
    private static final int TAB_SETTINGS = 3;
    private static final int TAB_PROFILE = 4;

    @Inject
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdgeUtils.enable(this);
        super.onCreate(savedInstanceState);

        // Defense in depth: the console is only for admin roles. If the persisted
        // role is missing or no longer admin, send the user to the regular app.
        if (!RoleUtils.isAdminRole(tokenManager.getRoleId())) {
            Intent intent = new Intent(this, App.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        InsetsUtils.applySystemBarsPadding(findViewById(R.id.root_layout), true, true);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_users) {
                showFragment(TAB_USERS);
                return true;
            }
            if (itemId == R.id.nav_requests) {
                showFragment(TAB_REQUESTS);
                return true;
            }
            if (itemId == R.id.nav_settings) {
                showFragment(TAB_SETTINGS);
                return true;
            }
            if (itemId == R.id.nav_profile) {
                showFragment(TAB_PROFILE);
                return true;
            }
            showFragment(TAB_DASHBOARD);
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void showFragment(int tab) {
        Fragment fragment;
        int titleRes;
        switch (tab) {
            case TAB_USERS:
                fragment = new AdminUsersFragment();
                titleRes = R.string.admin_users;
                break;
            case TAB_REQUESTS:
                fragment = new AdminRequestsFragment();
                titleRes = R.string.admin_requests;
                break;
            case TAB_SETTINGS:
                fragment = new AdminSettingsFragment();
                titleRes = R.string.admin_settings;
                break;
            case TAB_PROFILE:
                // The shared user Profile — same screen as the user app. It is
                // role-agnostic: shows the signed-in user's own name, role badge,
                // subscription, session info, language switcher and logout.
                fragment = new ProfileFragment();
                titleRes = R.string.title_profile;
                break;
            default:
                fragment = new AdminDashboardFragment();
                titleRes = R.string.admin_dashboard;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleRes);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
