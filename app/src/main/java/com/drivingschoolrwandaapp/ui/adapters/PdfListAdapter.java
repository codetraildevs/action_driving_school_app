package com.drivingschoolrwandaapp.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.PdfFile;
import com.drivingschoolrwandaapp.ui.activities.PdfViewerActivity;

import java.util.List;

public class PdfListAdapter extends RecyclerView.Adapter<PdfListAdapter.PdfViewHolder> {

    private List<PdfFile> pdfFiles;
    private final Context context;

    public PdfListAdapter(Context context, List<PdfFile> pdfFiles) {
        this.context = context;
        this.pdfFiles = pdfFiles;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        PdfFile pdfFile = pdfFiles.get(position);
        holder.pdfTitle.setText(pdfFile.getTitle());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PdfViewerActivity.class);
            intent.setData(Uri.parse(pdfFile.getFilePath()));
            intent.putExtra("pdf_title", pdfFile.getTitle());
            intent.putExtra("pdf_id", pdfFile.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles != null ? pdfFiles.size() : 0;
    }

    public void setPdfFiles(List<PdfFile> pdfFiles) {
        int oldSize = getItemCount();
        this.pdfFiles = pdfFiles;
        int newSize = getItemCount();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        }
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView pdfTitle;

        PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
