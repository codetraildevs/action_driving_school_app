package com.drivingschoolrwandaapp.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyApplicationsAdapter extends RecyclerView.Adapter<MyApplicationsAdapter.ViewHolder> {

    private List<IremboApplication> applications;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(IremboApplication application);
    }

    public MyApplicationsAdapter(List<IremboApplication> applications, Context context, OnItemClickListener listener) {
        this.applications = applications;
        this.context = context;
        this.listener = listener;
    }

    public void setApplications(List<IremboApplication> applications) {
        this.applications = applications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IremboApplication app = applications.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return applications == null ? 0 : applications.size();
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "N/A";
        try {
            // Try parsing ISO 8601 format
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(dateString);
            if (date != null) {
                SimpleDateFormat readableFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                return readableFormat.format(date);
            }
        } catch (ParseException e) {
            // Try alternative ISO format with Z or milliseconds
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = isoFormat.parse(dateString);
                if (date != null) {
                    SimpleDateFormat readableFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    return readableFormat.format(date);
                }
            } catch (ParseException ex) {
                // Return original string if parsing fails
            }
        }
        return dateString;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvRef, tvDate, tvStatus, tvStatusMessage, tvActionMessage;
        ProgressBar progressBar;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_app_title);
            tvRef = itemView.findViewById(R.id.tv_app_ref);
            tvDate = itemView.findViewById(R.id.tv_app_date);
            tvStatus = itemView.findViewById(R.id.tv_app_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvStatusMessage = itemView.findViewById(R.id.tv_status_message);
            tvActionMessage = itemView.findViewById(R.id.tv_action_message);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(applications.get(position));
                }
            });
        }

        void bind(IremboApplication app) {
            tvTitle.setText(app.getTitle());
            tvRef.setText("Ref: " + app.getReference());
            tvDate.setText("Applied: " + formatDate(app.getDate()));
            tvStatus.setText(app.getStatus());

            // Reset visibility
            progressBar.setVisibility(View.GONE);
            tvStatusMessage.setVisibility(View.GONE);
            tvActionMessage.setVisibility(View.GONE);



            int bgDrawable;
            
            String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "";

            switch (status) {
                case "PENDING":
                case "PROCESSING":

                    bgDrawable = R.drawable.status_pending_background;
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(app.getCompletionPercentage());
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText("Waiting for approval");
                    break;

                case "ACTION":
                     bgDrawable = R.drawable.status_action_background;
                    tvActionMessage.setVisibility(View.VISIBLE);
                    tvActionMessage.setText("Action Required");
                    break;

                case "APPROVED":
                     bgDrawable = R.drawable.status_done_background;
                    break;
                case "REJECTED":
                     bgDrawable = R.drawable.status_rejected_background;
                    break;
                default:
                     bgDrawable = R.drawable.status_pending_background;
            }

             tvStatus.setBackgroundResource(bgDrawable);

         }
    }
}
