package com.drivingschoolrwandaapp.ui.fragments;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.data.models.LearningMaterial;
import com.drivingschoolrwandaapp.database.AppDatabase;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.ui.activities.PdfViewerActivity;
import com.drivingschoolrwandaapp.ui.adapters.LearningMaterialAdapter;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.utils.FileUtils;
import com.drivingschoolrwandaapp.utils.NotificationHelper;
import com.drivingschoolrwandaapp.viewmodel.LearningMaterialViewModel;
import com.drivingschoolrwandaapp.viewmodel.DownloadState;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MaterialsFragment extends Fragment implements LearningMaterialAdapter.OnItemClickListener, LearningMaterialAdapter.OnDownloadButtonClickListener {

    private LearningMaterialViewModel viewModel;
    private RecyclerView recyclerView;
    private LearningMaterialAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private UserDao userDao;
    private NotificationHelper notificationHelper;
    // Cache the user from LiveData observation to avoid synchronous DB reads
    // on the main thread. Populated by observeUser().
    private com.drivingschoolrwandaapp.database.entities.User cachedUser;

    private List<LearningMaterial> allMaterials = new ArrayList<>();

    @Inject
    AppDatabase appDatabase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        userDao = appDatabase.userDao();
        notificationHelper = new NotificationHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_materials, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsUtils.logScreenView(getContext(), "materials");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LearningMaterialViewModel.class);

        recyclerView = view.findViewById(R.id.materials_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        errorTextView = view.findViewById(R.id.error_text_view);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_placeholder);

        setupRecyclerView();
        observeViewModel();
        observeUser();

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.fetchLearningMaterials(1, 10));

        viewModel.fetchLearningMaterials(1, 10);
    }

    private void setupRecyclerView() {
        adapter = new LearningMaterialAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnDownloadButtonClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void filterMaterials(String query) {
        if (allMaterials == null) return;

        List<LearningMaterial> filteredList = new ArrayList<>();
        for (LearningMaterial material : allMaterials) {
            if (material.getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                filteredList.add(material);
            }
        }
        adapter.setMaterials(filteredList);
    }

    private void observeUser() {
        userDao.getUser().observe(getViewLifecycleOwner(), user -> {
            cachedUser = user;
            if (adapter != null) {
                adapter.setCurrentUser(user);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getMaterials().observe(getViewLifecycleOwner(), materials -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            // Hide shimmer immediately when data arrives to prevent flicker
            hideShimmer();
            if (materials != null && !materials.isEmpty()) {
                allMaterials = materials; // Save full list
                adapter.setMaterials(materials);
                errorTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                // Preload thumbnails for visible items to improve scrolling smoothness
                preloadThumbnails(materials);
            } else {
                errorTextView.setText(getString(R.string.no_materials_found));
                errorTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                // Show shimmer skeleton on initial load (not swipe-to-refresh)
                if (!swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                    if (shimmerFrameLayout != null && (allMaterials == null || allMaterials.isEmpty())) {
                        shimmerFrameLayout.setVisibility(View.VISIBLE);
                        shimmerFrameLayout.startShimmer();
                        recyclerView.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.GONE);
                    }
                }
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                hideShimmer();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                hideShimmer();
                errorTextView.setText(error);
                errorTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getDownloadStatus().observe(getViewLifecycleOwner(), downloadState -> {
            if (downloadState == null) return;

            adapter.updateDownloadState(downloadState);
            LearningMaterial material = findMaterialById(String.valueOf(downloadState.getMaterialId()));
            if (material == null) return;

            int notificationId = material.getId();

            String title = material.getTitle() != null ? material.getTitle() : "";

            switch (downloadState.getStatus()) {
                case DOWNLOADING:
                    notificationHelper.showProgressNotification(notificationId,
                            getString(R.string.downloading),
                            getString(R.string.downloading_material, title));
                    break;
                case SUCCESS:
                    notificationHelper.showDownloadCompleteNotification(notificationId,
                            getString(R.string.download_complete),
                            getString(R.string.download_complete_message, title));
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.download_success), Toast.LENGTH_LONG).show();
                    }
                    break;
                case FAILURE:
                    // Prefer the real server/network reason (e.g. "File not found on
                    // server") so a failed download isn't a mystery to the user.
                    String reason = downloadState.getMessage();
                    String failureMessage = (reason != null && !reason.isEmpty())
                            ? getString(R.string.download_failure_reason, reason)
                            : getString(R.string.download_failure);
                    notificationHelper.showDownloadFailedNotification(notificationId,
                            getString(R.string.download_failed_title), failureMessage);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    /**
     * Safely hide the shimmer skeleton and stop its animation.
     * Safe to call even if shimmer is not visible or is null.
     */
    private void hideShimmer() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop shimmer animation to prevent leaks when the fragment view is destroyed
        hideShimmer();
    }

    private void preloadThumbnails(List<LearningMaterial> materials) {
        if (!isAdded() || materials == null) return;
        int preloadCount = Math.min(materials.size(), 10); // Preload first 10 thumbnails
        for (int i = 0; i < preloadCount; i++) {
            LearningMaterial material = materials.get(i);
            String thumbnailUrl = material.getThumbnailUrl();
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                String fullUrl = com.drivingschoolrwandaapp.api.ApiClient.SITE_URL + thumbnailUrl;
                if (!fullUrl.toLowerCase(Locale.ROOT).endsWith(".svg")) {
                    Glide.with(this)
                            .load(fullUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload();
                }
            }
        }
    }

    private LearningMaterial findMaterialById(String materialId) {
        if (allMaterials == null) {
            return null;
        }
        for (LearningMaterial material : allMaterials) {
            if (String.valueOf(material.getId()).equals(materialId)) {
                return material;
            }
        }
        return null;
    }

    private boolean canAccessPaidContent(String action) {
        com.drivingschoolrwandaapp.database.entities.User user = cachedUser;
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getTestAccessStatus())) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.need_active_subscription, action), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        boolean isExpired = true;
        if (user.getTestAccessExpiresAt() != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date expirationDate = sdf.parse(user.getTestAccessExpiresAt());
                if (expirationDate != null && !expirationDate.before(new Date())) {
                    isExpired = false;
                }
            } catch (ParseException e) {
                Log.e("MaterialsFragment", "Failed to parse expiration date (ISO): " + user.getTestAccessExpiresAt(), e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                try {
                    SimpleDateFormat sdfFallback = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date expirationDate = sdfFallback.parse(user.getTestAccessExpiresAt());
                    if (expirationDate != null && !expirationDate.before(new Date())) {
                        isExpired = false;
                    }
                } catch (ParseException e2) {
                    Log.e("MaterialsFragment", "Failed to parse expiration date (fallback): " + user.getTestAccessExpiresAt(), e2);
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e2);
                }
            }
        }

        if (isExpired) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.test_access_expired_msg, action), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        return true;
    }

    @Override
    public void onDownloadButtonClick(LearningMaterial material) {
        if (!material.isPublic() && !canAccessPaidContent("download")) {
            return;
        }

        // Permission is not required for writing to app's internal storage
        viewModel.downloadLearningMaterial(material);
    }

    @Override
    public void onItemClick(LearningMaterial material) {
        if (!material.isPublic() && !canAccessPaidContent("view")) {
            return;
        }

        if (material.isDownloaded()) {
            openFile(material);
        } else {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.download_first), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFile(LearningMaterial material) {
        File internalStroageDir = requireContext().getFilesDir();
        String fileName = FileUtils.getSafeFileName(material);
        File file = new File(internalStroageDir, fileName);

        if (file.exists()) {
            String authority = requireContext().getPackageName() + ".provider";
            Uri fileUri = FileProvider.getUriForFile(requireContext(), authority, file);
            
            if ("application/pdf".equals(material.getFileType())) {
                Intent intent = new Intent(getContext(), PdfViewerActivity.class);
                intent.setData(fileUri);
                intent.putExtra("pdf_title", material.getTitle());
                intent.putExtra("pdf_id", String.valueOf(material.getId()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else if (material.getFileType().startsWith("image/")) {
                showImageDialog(material, fileUri);
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, material.getFileType());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e("MaterialsFragment", "No app found to open file: " + material.getFileType(), e);
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.no_app_found), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("MaterialsFragment", "Error opening file: " + material.getTitle(), e);
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.error_opening_file), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImageDialog(LearningMaterial material, Uri imageUri) {
        if (!isAdded() || getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_material_image, null);
        builder.setView(dialogView);

        ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogDescription = dialogView.findViewById(R.id.dialog_description);

        RequestOptions dialogOptions = new RequestOptions()
                .placeholder(R.drawable.ic_materials)
                .error(R.drawable.ic_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(this)
                .load(imageUri)
                .apply(dialogOptions)
                .into(dialogImage);

        dialogTitle.setText(material.getTitle());
        dialogDescription.setText(material.getDescription());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_materials, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterMaterials(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterMaterials(newText);
                        return true;
                    }
                });
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavHostFragment.findNavController(this).popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
