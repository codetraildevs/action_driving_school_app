package com.drivingschoolrwandaapp.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class WhatsAppGroupAdapter extends RecyclerView.Adapter<WhatsAppGroupAdapter.ViewHolder> {

    private List<WhatsAppGroup> groups;
    private final OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onOpenGroup(WhatsAppGroup group);
        void onCopyLink(WhatsAppGroup group);
    }

    public WhatsAppGroupAdapter(List<WhatsAppGroup> groups, OnGroupClickListener listener) {
        this.groups = groups != null ? groups : new ArrayList<>();
        this.listener = listener;
    }

    public void setGroups(List<WhatsAppGroup> groups) {
        int oldSize = this.groups.size();
        this.groups = groups;
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        int newSize = getItemCount();
        if (newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_whatsapp_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WhatsAppGroup group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivGroupImage;
        private final TextView tvGroupName;
        private final TextView tvGroupDescription;
        private final TextView tvStatusBadge;
        private final MaterialButton btnOpenGroup;
        private final MaterialButton btnCopyLink;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGroupImage = itemView.findViewById(R.id.iv_group_image);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvGroupDescription = itemView.findViewById(R.id.tv_group_description);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            btnOpenGroup = itemView.findViewById(R.id.btn_open_group);
            btnCopyLink = itemView.findViewById(R.id.btn_copy_link);
        }

        public void bind(WhatsAppGroup group) {
            tvGroupName.setText(group.getName());
            tvGroupDescription.setText(group.getDescription());

            if (group.isActive()) {
                tvStatusBadge.setVisibility(View.VISIBLE);
                tvStatusBadge.setText(itemView.getContext().getString(R.string.active_status));
            } else {
                tvStatusBadge.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext())
                    .load(group.getImageUrl())
                    .placeholder(R.drawable.ic_whatsapp) // Make sure you have a placeholder
                    .error(R.drawable.ic_whatsapp)
                    .into(ivGroupImage);

            btnOpenGroup.setOnClickListener(v -> {
                if (listener != null) listener.onOpenGroup(group);
            });

            btnCopyLink.setOnClickListener(v -> {
                if (listener != null) listener.onCopyLink(group);
            });
        }
    }
}
