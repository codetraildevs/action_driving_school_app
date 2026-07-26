package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboService;
import java.util.List;

public class IremboServiceAdapter extends RecyclerView.Adapter<IremboServiceAdapter.ViewHolder> {

    private List<IremboService> services;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(IremboService service);
    }

    public IremboServiceAdapter(List<IremboService> services, OnItemClickListener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_irembo_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IremboService service = services.get(position);
        holder.name.setText(service.getName());
        Drawable icon = ContextCompat.getDrawable(holder.itemView.getContext(), service.getIconResId());
        if (icon != null) {
            icon = icon.mutate();
            DrawableCompat.setTint(
                icon,
                ContextCompat.getColor(holder.itemView.getContext(), R.color.my_primary)
            );
        }
        holder.name.setCompoundDrawablesWithIntrinsicBounds(
            null, /* start */
            icon, /* top */
            null, /* end */
            null  /* bottom */
        );
        holder.itemView.setOnClickListener(v -> listener.onItemClick(service));
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_service_name);
        }
    }
}
