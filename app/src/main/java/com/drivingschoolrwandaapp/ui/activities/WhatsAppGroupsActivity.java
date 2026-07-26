package com.drivingschoolrwandaapp.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.WhatsAppGroupAdapter;
import com.drivingschoolrwandaapp.viewmodel.WhatsAppViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WhatsAppGroupsActivity extends AppCompatActivity implements WhatsAppGroupAdapter.OnGroupClickListener {

    private WhatsAppViewModel viewModel;
    private WhatsAppGroupAdapter adapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;
    
    private List<WhatsAppGroup> allGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whatsapp_groups);

        initViews();
        setupRecyclerView();
        setupViewModel();
        
        fetchGroups();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.rv_whatsapp_groups);
        layoutError = findViewById(R.id.layout_error);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnRetry = findViewById(R.id.btn_retry);

        btnRetry.setOnClickListener(v -> fetchGroups());
    }

    private void setupRecyclerView() {
        adapter = new WhatsAppGroupAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(WhatsAppViewModel.class);
    }

    private void fetchGroups() {
        viewModel.getWhatsAppGroups().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    showLoading();
                    break;
                case SUCCESS:
                    showContent();
                    if (resource.data != null && !resource.data.isEmpty()) {
                        allGroups = resource.data;
                        adapter.setGroups(allGroups);
                    } else {
                        showError("No active WhatsApp groups found.");
                    }
                    break;
                case ERROR:
                    showError(resource.message);
                    break;
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message != null ? message : "An unknown error occurred");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });
        return true;
    }

    private void filter(String text) {
        List<WhatsAppGroup> filteredList = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(allGroups);
        } else {
            String lowerText = text.toLowerCase(Locale.ROOT);
            for (WhatsAppGroup group : allGroups) {
                String name = group.getName();
                String description = group.getDescription();
                boolean nameMatch = name != null && name.toLowerCase(Locale.ROOT).contains(lowerText);
                boolean descMatch = description != null && description.toLowerCase(Locale.ROOT).contains(lowerText);
                if (nameMatch || descMatch) {
                    filteredList.add(group);
                }
            }
        }
        adapter.setGroups(filteredList);
    }

    @Override
    public void onOpenGroup(WhatsAppGroup group) {
        if (group.getWhatsappLink() != null && !group.getWhatsappLink().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(group.getWhatsappLink()));
                startActivity(intent);
            } catch (Exception e) {
                Log.e("WhatsAppGroups", "Could not open WhatsApp link: " + group.getWhatsappLink(), e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                Toast.makeText(this, getString(R.string.could_not_open_link), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.invalid_link), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyLink(WhatsAppGroup group) {
        if (group.getWhatsappLink() != null && !group.getWhatsappLink().isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("WhatsApp Group Link", group.getWhatsappLink());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.no_link_to_copy), Toast.LENGTH_SHORT).show();
        }
    }
}
