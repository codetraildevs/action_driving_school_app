package com.drivingschoolrwandaapp.ui.adapters;

import static com.drivingschoolrwandaapp.utils.SvgGlideLoader.loadSvg;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.data.models.LearningMaterial;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.utils.FileUtils;
import com.drivingschoolrwandaapp.viewmodel.DownloadState;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class LearningMaterialAdapter extends RecyclerView.Adapter<LearningMaterialAdapter.LearningMaterialViewHolder> {

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    private final List<LearningMaterial> materials = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnDownloadButtonClickListener onDownloadButtonClickListener;
    private int viewType = VIEW_TYPE_LIST;
    private User currentUser;

    public interface OnItemClickListener {
        void onItemClick(LearningMaterial material);
    }

    public interface OnDownloadButtonClickListener {
        void onDownloadButtonClick(LearningMaterial material);
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
        if (!materials.isEmpty()) {
            notifyItemRangeChanged(0, materials.size());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (!materials.isEmpty()) {
            notifyItemRangeChanged(0, materials.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public LearningMaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_GRID) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material_grid, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material_list, parent, false);
        }
        return new LearningMaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LearningMaterialViewHolder holder, int position) {
        LearningMaterial material = materials.get(position);
        holder.bind(material);
    }

    @Override
    public void onBindViewHolder(@NonNull LearningMaterialViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            DownloadState downloadState = (DownloadState) payloads.get(0);
            boolean isDownloading = downloadState.getStatus() == DownloadState.Status.DOWNLOADING;
            boolean isFailed = downloadState.getStatus() == DownloadState.Status.FAILURE;
            boolean isDownloaded = downloadState.getStatus() == DownloadState.Status.SUCCESS;

            holder.updateDownloadIcon(isDownloaded, isDownloading, isFailed);
        }
    }

    @Override
    public int getItemCount() {
        return materials.size();
    }

    public void setMaterials(List<LearningMaterial> newMaterials) {
        int oldSize = materials.size();
        materials.clear();
        if (newMaterials != null) {
            materials.addAll(newMaterials);
        }
        int newSize = materials.size();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDownloadButtonClickListener(OnDownloadButtonClickListener listener) {
        this.onDownloadButtonClickListener = listener;
    }

    public void updateMaterial(LearningMaterial material) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId() == material.getId()) {
                materials.set(i, material);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void updateDownloadState(DownloadState downloadState) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getId() == downloadState.getMaterialId()) {
                notifyItemChanged(i, downloadState);
                return;
            }
        }
    }

    class LearningMaterialViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnail;
        private TextView title;
        private TextView fileTypeOrSize;
        private TextView expirationText;
        private ImageButton downloadButton;
        private ProgressBar progressBar;

        public LearningMaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.material_thumbnail);
            title = itemView.findViewById(R.id.material_title);
            fileTypeOrSize = itemView.findViewById(R.id.material_file_type_or_size);
            downloadButton = itemView.findViewById(R.id.download_button);
            progressBar = itemView.findViewById(R.id.download_progress_bar);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(materials.get(position));
                }
            });

            downloadButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDownloadButtonClickListener != null) {
                    onDownloadButtonClickListener.onDownloadButtonClick(materials.get(position));
                }
            });
        }

        void bind(LearningMaterial material) {
            title.setText(material.getTitle());

            if (material.isDownloaded() && material.getFileSize() > 0) {
                double sizeInMb = material.getFileSize() / (1024.0 * 1024.0);
                fileTypeOrSize.setText(String.format(Locale.ROOT, "%.2f MB", sizeInMb));
            } else {
                fileTypeOrSize.setText(material.getFileType());
            }

            updateDownloadIcon(material.isDownloaded(), false, false);

            boolean isExpirationSet = false;

            if (!material.isPublic() && currentUser != null && currentUser.getTestAccessExpiresAt() != null) {
                long remainingMillis = 0;
                boolean parseSuccess = false;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date expirationDate = sdf.parse(currentUser.getTestAccessExpiresAt());
                    if (expirationDate != null) {
                        remainingMillis = expirationDate.getTime() - System.currentTimeMillis();
                        parseSuccess = true;
                    }
                } catch (ParseException e) {
                    Log.e("MaterialAdapter", "Failed to parse date (ISO), trying fallback: " + currentUser.getTestAccessExpiresAt(), e);
                    try {
                        SimpleDateFormat sdfFallback = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date expirationDate = sdfFallback.parse(currentUser.getTestAccessExpiresAt());
                        if (expirationDate != null) {
                            remainingMillis = expirationDate.getTime() - System.currentTimeMillis();
                            parseSuccess = true;
                        }
                    } catch (ParseException e2) {
                        Log.e("MaterialAdapter", "Error parsing date with fallback: " + currentUser.getTestAccessExpiresAt(), e2);
                    }
                }

                if (parseSuccess) {

                    if (remainingMillis <= 0) {
                        {
                            expirationText.setText(itemView.getContext().getString(R.string.access_expired));
                            expirationText.setVisibility(View.VISIBLE);
                            expirationText.setTextColor(Color.RED);
                        }
                    }
                }
            }



            if (material.isDownloaded() && material.getFileType().startsWith("image/")) {
                File internalStroageDir = itemView.getContext().getFilesDir();
                String fileName = FileUtils.getSafeFileName(material);
                File file = new File(internalStroageDir, fileName);
                if (file.exists()) {
                    Uri fileUri = FileProvider.getUriForFile(itemView.getContext(), itemView.getContext().getPackageName() + ".provider", file);
                    Glide.with(itemView.getContext())
                            .load(fileUri)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_error)
                            .into(thumbnail);
                } else {
                    loadThumbnail(material.getThumbnailUrl());
                }
            } else {
                loadThumbnail(material.getThumbnailUrl());
            }
        }
        
        private void loadThumbnail(String thumbnailUrl) {
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                String fullThumbnailUrl = ApiClient.SITE_URL + thumbnailUrl;
                if (fullThumbnailUrl.toLowerCase(Locale.ROOT).endsWith(".svg")) {
                    loadSvg(thumbnail.getContext(), fullThumbnailUrl, thumbnail);
                } else {
                    Glide.with(itemView.getContext())
                            .load(fullThumbnailUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_error)
                            .into(thumbnail);
                }
            } else {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_launcher_background)
                        .into(thumbnail);
            }
        }

        void updateDownloadIcon(boolean isDownloaded, boolean isDownloading, boolean isFailed) {
            if (isDownloading) {
                progressBar.setVisibility(View.VISIBLE);
                downloadButton.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                downloadButton.setVisibility(View.VISIBLE);
                if (isFailed) {
                    downloadButton.setImageResource(R.drawable.ic_error);
                    downloadButton.setEnabled(true);
                } else if (isDownloaded) {
                    downloadButton.setImageResource(R.drawable.ic_check_circle);
                    downloadButton.setEnabled(false);
                } else {
                    downloadButton.setImageResource(R.drawable.ic_download);
                    downloadButton.setEnabled(true);
                }
            }
        }
    }
}
