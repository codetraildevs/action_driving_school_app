package com.drivingschoolrwandaapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.Bookmark;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {

    private List<Bookmark> bookmarks;
    private OnBookmarkClickListener clickListener;
    private OnBookmarkDeleteListener deleteListener;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(int pageNumber);
    }

    public interface OnBookmarkDeleteListener {
        void onBookmarkDelete(Bookmark bookmark);
    }

    public BookmarkAdapter(List<Bookmark> bookmarks, OnBookmarkClickListener clickListener, OnBookmarkDeleteListener deleteListener) {
        this.bookmarks = bookmarks;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);
        holder.bind(bookmark);
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        TextView bookmarkNameTextView;
        TextView pageNumberTextView;
        ImageButton deleteButton;

        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            bookmarkNameTextView = itemView.findViewById(R.id.bookmark_name);
            pageNumberTextView = itemView.findViewById(R.id.bookmark_page_number);
            deleteButton = itemView.findViewById(R.id.delete_bookmark_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onBookmarkClick(bookmarks.get(position).pageNumber);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    deleteListener.onBookmarkDelete(bookmarks.get(position));
                }
            });
        }

        void bind(Bookmark bookmark) {
            if (bookmark.name != null && !bookmark.name.isEmpty()) {
                bookmarkNameTextView.setText(bookmark.name);
                bookmarkNameTextView.setVisibility(View.VISIBLE);
            } else {
                bookmarkNameTextView.setVisibility(View.GONE);
            }
            pageNumberTextView.setText("Page " + (bookmark.pageNumber + 1));
        }
    }
}
