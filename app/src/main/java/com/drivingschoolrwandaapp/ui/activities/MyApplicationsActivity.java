package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.MyApplicationsAdapter;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MyApplicationsActivity extends AppCompatActivity {

    private IremboViewModel iremboViewModel;
    private MyApplicationsAdapter adapter;
    private AlertDialog loadingDialog;
    private List<IremboApplication> allApplications = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_my_applications);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        RecyclerView recyclerView = findViewById(R.id.rv_my_applications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new MyApplicationsAdapter(new ArrayList<>(), this, new MyApplicationsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(IremboApplication application) {
                 Intent intent = new Intent(MyApplicationsActivity.this, ApplicationDetailsActivity.class);
                 intent.putExtra("application_details", application);
                 startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        setupFilters();
        setupSearch();

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        setupObservers();
        
        iremboViewModel.fetchRecentApplications();
    }
    
    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_filters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_all) {
                currentStatusFilter = "ALL";
            } else if (checkedId == R.id.chip_pending) {
                currentStatusFilter = "PENDING";
            } else if (checkedId == R.id.chip_processing) {
                currentStatusFilter = "PROCESSING";
            } else if (checkedId == R.id.chip_action) {
                currentStatusFilter = "ACTION";
            } else if (checkedId == R.id.chip_approved) {
                currentStatusFilter = "APPROVED";
            } else if (checkedId == R.id.chip_rejected) {
                currentStatusFilter = "REJECTED";
            }
            
            filterList();
        });
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                filterList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList() {
        List<IremboApplication> filteredList = new ArrayList<>();
        
        for (IremboApplication app : allApplications) {
            boolean matchesStatus = currentStatusFilter.equals("ALL") || 
                                   (app.getStatus() != null && app.getStatus().toUpperCase().contains(currentStatusFilter));
            
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                   (app.getReference() != null && app.getReference().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                                   (app.getTitle() != null && app.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesStatus && matchesSearch) {
                filteredList.add(app);
            }
        }
        
        adapter.setApplications(filteredList);
    }
    
    private void setupObservers() {
        iremboViewModel.getRecentApplications().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    allApplications = resource.data;
                    filterList();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(this));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
