package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.RequestViewHolder> {

    private final List<AdminRequest> requests = new ArrayList<>();

    public void submitList(List<AdminRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        AdminRequest request = requests.get(position);

        String type = request.getType() != null ? request.getType().replace('_', ' ') : "";
        holder.typeLabel.setText(type.isEmpty() ? "—" : type.toUpperCase(Locale.ROOT));
        holder.title.setText(request.getTitle() != null ? request.getTitle() : "");

        int progress = Math.max(0, Math.min(100, request.getCompletionPercentage()));
        holder.progress.setProgressCompat(progress, true);
        holder.progressText.setText(progress + "%");

        String status = request.getStatus() != null ? request.getStatus().toUpperCase(Locale.ROOT) : "";
        holder.statusChip.setText(status.isEmpty() ? "—" : status);
        styleStatusChip(holder.statusChip, status);
    }

    /**
     * Applies the shared status-chip styling (used by the Requests tab and the
     * admin user detail dialog so both stay visually consistent).
     */
    public static void styleStatusChip(TextView chip, String upperStatus) {
        if (upperStatus.contains("REJECT")) {
            chip.setBackgroundResource(R.drawable.status_rejected_background);
            chip.setTextColor(android.graphics.Color.parseColor("#C62828"));
        } else if (upperStatus.contains("APPROVE") || upperStatus.contains("COMPLETE")) {
            chip.setBackgroundResource(R.drawable.status_done_background);
            chip.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            chip.setBackgroundResource(R.drawable.status_pending_background);
            chip.setTextColor(android.graphics.Color.parseColor("#A05A00"));
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        final TextView typeLabel;
        final TextView title;
        final TextView statusChip;
        final LinearProgressIndicator progress;
        final TextView progressText;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            typeLabel = itemView.findViewById(R.id.request_type);
            title = itemView.findViewById(R.id.request_title);
            statusChip = itemView.findViewById(R.id.request_status);
            progress = itemView.findViewById(R.id.request_progress);
            progressText = itemView.findViewById(R.id.request_progress_text);
        }
    }
}
