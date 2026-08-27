package com.drivingschoolrwandaapp.ui.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.AdminUser;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.activities.AdminUserDetailDialog;
import com.drivingschoolrwandaapp.ui.adapters.AdminUserAdapter;
import com.drivingschoolrwandaapp.utils.AdminListFilter;
import com.drivingschoolrwandaapp.viewmodel.AdminViewModel;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminUsersFragment extends Fragment {

    private AdminViewModel adminViewModel;
    private AdminUserAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView emptyStateText;
    private LinearLayout errorContainer;
    private TextView errorText;

    private List<AdminUser> allUsers = Collections.emptyList();
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        errorContainer = view.findViewById(R.id.error_container);
        errorText = view.findViewById(R.id.error_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter();
        // Tapping a user opens the detail dialog (profile + subscription + requests).
        adapter.setOnUserClickListener(user -> {
            adminViewModel.refreshUserDetail(user.getId());
            if (getActivity() != null) {
                AdminUserDetailDialog.show(getActivity(), adminViewModel, user);
            }
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.retry_button).setOnClickListener(v -> adminViewModel.refreshUsers());

        swipeRefreshLayout.setColorSchemeResources(R.color.my_primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Don't stop the spinner here — it stays up until the fetch settles
            // (see the terminal branches of the observer below), so the user
            // sees progress while the list reloads.
            adminViewModel.refreshUsers();
        });

        // Search filters the already-loaded list in memory.
        view.<com.google.android.material.textfield.TextInputEditText>findViewById(R.id.search_input)
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        query = s != null ? s.toString() : "";
                        applyFilter();
                    }
                });

        adminViewModel.getUsers().observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.LOADING) {
                if (!swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                errorContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                errorText.setText(resource.getMessage() != null
                        ? resource.getMessage()
                        : getString(R.string.admin_load_failed));
                errorContainer.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                errorContainer.setVisibility(View.GONE);
                allUsers = resource.getData() != null ? resource.getData() : Collections.emptyList();
                applyFilter();
            }
        });

        if (adminViewModel.getUsers().getValue() == null) {
            adminViewModel.refreshUsers();
        }
    }

    private void applyFilter() {
        List<AdminUser> filtered = AdminListFilter.filterUsers(allUsers, query);
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyStateText.setText(query.trim().isEmpty()
                    ? R.string.admin_empty_users
                    : R.string.admin_no_matching_users);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter.submitList(filtered);
        }
    }
}
