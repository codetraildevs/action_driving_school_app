package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;

import java.util.List;

public class WelcomeCarouselAdapter extends RecyclerView.Adapter<WelcomeCarouselAdapter.ViewHolder> {

    private final List<CarouselItem> items;

    public WelcomeCarouselAdapter(List<CarouselItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_welcome_carousel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarouselItem item = items.get(position);
        holder.imageView.setImageResource(item.imageRes);
//        holder.descriptionView.setText(item.description);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView descriptionView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carousel_image);
//            descriptionView = itemView.findViewById(R.id.carousel_description);
        }
    }

    public static class CarouselItem {
        int imageRes;
        String description;

        public CarouselItem(int imageRes, String description) {
            this.imageRes = imageRes;
            this.description = description;
        }
    }
}