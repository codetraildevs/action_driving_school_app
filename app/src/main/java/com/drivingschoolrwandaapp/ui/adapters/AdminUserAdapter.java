package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.AdminUser;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    /** Row click callback (e.g. opens the user detail dialog). */
    public interface OnUserClickListener {
        void onUserClick(AdminUser user);
    }

    private final List<AdminUser> users = new ArrayList<>();
    private OnUserClickListener onUserClickListener;

    public void submitList(List<AdminUser> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.onUserClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUser user = users.get(position);

        holder.avatar.setText(user.getInitials());
        holder.name.setText(TextUtils.isEmpty(user.getFullName()) ? "—" : user.getFullName());
        String email = user.getEmail() != null ? user.getEmail() : user.getPhoneNumber();
        holder.email.setText(email != null ? email : "");

        String roleName = user.getRole() != null && user.getRole().getRoleName() != null
                ? user.getRole().getRoleName()
                : "—";
        holder.roleBadge.setText(roleName);

        Context ctx = holder.itemView.getContext();
        if (user.isAdmin()) {
            holder.roleBadge.setTextColor(ContextCompat.getColor(ctx, R.color.my_primary));
            holder.roleBadge.setBackgroundResource(R.drawable.bg_badge_admin);
        } else {
            holder.roleBadge.setTextColor(ContextCompat.getColor(ctx, R.color.colorOnSurface));
            holder.roleBadge.setBackgroundResource(R.drawable.bg_badge);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView name;
        final TextView email;
        final TextView roleBadge;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            name = itemView.findViewById(R.id.user_name);
            email = itemView.findViewById(R.id.user_email);
            roleBadge = itemView.findViewById(R.id.user_role_badge);
        }
    }
}
