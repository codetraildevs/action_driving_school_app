package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<IremboApplication> applications;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(IremboApplication application);
    }

    public RecentActivityAdapter(List<IremboApplication> applications, OnItemClickListener listener) {
        this.applications = applications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IremboApplication application = applications.get(position);
        holder.title.setText(application.getTitle());
        holder.reference.setText("Ref: " + application.getReference());
        holder.status.setText(application.getStatus());
        holder.date.setText(formatDate(application.getDate()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(application);
            }
        });
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    public void setApplications(List<IremboApplication> applications) {
        int oldSize = this.applications.size();
        this.applications = applications;
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        int newSize = getItemCount();
        if (newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        }
    }
    
    private String formatDate(String dateString) {
        if (dateString == null) return "";
        // Input format: 2025-12-26T20:52:50.211Z
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        // Output format: Dec 26, 2025 • 8:52 PM
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
        
        try {
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Try without milliseconds if it fails
            SimpleDateFormat inputFormatNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            inputFormatNoMillis.setTimeZone(TimeZone.getTimeZone("UTC"));
             try {
                Date date = inputFormatNoMillis.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException ex) {
                return dateString; // Return original if parsing fails
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, reference, status, date;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_activity_icon);
            title = itemView.findViewById(R.id.tv_activity_title);
            reference = itemView.findViewById(R.id.tv_activity_ref);
            status = itemView.findViewById(R.id.tv_status);
            date = itemView.findViewById(R.id.tv_date);
        }
    }
}
