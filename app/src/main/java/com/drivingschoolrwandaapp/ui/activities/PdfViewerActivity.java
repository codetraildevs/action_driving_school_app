package com.drivingschoolrwandaapp.ui.activities;

import android.content.SharedPreferences;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.ui.adapters.BookmarkAdapter;
import com.drivingschoolrwandaapp.ui.adapters.PdfAdapter;
import com.drivingschoolrwandaapp.viewmodel.PdfViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PdfViewerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView pageNumberText;
    private ProgressBar progressBar;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int pdfId;
    private File tempFile;
    private PdfViewModel pdfViewModel;
    private SharedPreferences prefs;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pdfViewModel = new ViewModelProvider(this).get(PdfViewModel.class);
        prefs = getSharedPreferences("PdfViewerPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.pdf_recycler_view);
        pageNumberText = findViewById(R.id.page_number_text);
        progressBar = findViewById(R.id.pdf_progress_bar);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        try {
            Uri fileUri = getIntent().getData();
            String pdfTitle = getIntent().getStringExtra("pdf_title");
            pdfId = getIntent().getIntExtra("pdf_id", -1);

            if (pdfTitle != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(pdfTitle);
            }

            if (fileUri != null) {
                setupPdfRenderer(fileUri);
            } else {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.something_went_wrong) + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupPdfRenderer(Uri uri) throws Exception {
        progressBar.setVisibility(View.VISIBLE);
        
        tempFile = new File(getCacheDir(), "temp_pdf.pdf");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        PdfAdapter adapter = new PdfAdapter(pdfRenderer, metrics.widthPixels);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int currentPos = layoutManager.findFirstVisibleItemPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    updatePageIndicator(currentPos);
                }
            }
        });

        int savedPage = prefs.getInt(String.valueOf(pdfId), 0);
        if (savedPage < pdfRenderer.getPageCount()) {
            recyclerView.scrollToPosition(savedPage);
            updatePageIndicator(savedPage);
        }

        progressBar.setVisibility(View.GONE);
    }

    private void updatePageIndicator(int position) {
        pageNumberText.setText(String.format("%d / %d", position + 1, pdfRenderer.getPageCount()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_bookmark) {
            showAddBookmarkDialog();
            return true;
        } else if (item.getItemId() == R.id.action_go_to_page) {
            showGoToPageDialog();
            return true;
        } else if (item.getItemId() == R.id.action_view_bookmarks) {
            showBookmarksDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddBookmarkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_bookmark, null);
        final EditText bookmarkNameInput = dialogView.findViewById(R.id.bookmark_name_input);

        int currentPage = layoutManager.findFirstVisibleItemPosition();

        builder.setView(dialogView)
                .setPositiveButton(R.string.add_bookmark, (dialog, which) -> {
                    String name = bookmarkNameInput.getText().toString().trim();
                    pdfViewModel.addBookmark(pdfId, currentPage, name);
                    Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showGoToPageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_go_to_page, null);
        final EditText pageNumberInput = dialogView.findViewById(R.id.page_number_input);

        builder.setView(dialogView)
                .setPositiveButton(R.string.go_to_page, (dialog, which) -> {
                    String pageString = pageNumberInput.getText().toString();
                    if (!pageString.isEmpty()) {
                        int page = Integer.parseInt(pageString) - 1;
                        if (page >= 0 && page < pdfRenderer.getPageCount()) {
                            recyclerView.scrollToPosition(page);
                        } else {
                            Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showBookmarksDialog() {
        pdfViewModel.getBookmarks(pdfId).observe(this, bookmarks -> {
            if (bookmarks == null || bookmarks.isEmpty()) {
                Toast.makeText(this, "No bookmarks yet", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_view_bookmarks, null);
            RecyclerView bookmarksRecyclerView = dialogView.findViewById(R.id.bookmarks_recycler_view);
            bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            final AlertDialog dialog = builder.setView(dialogView).create();

            BookmarkAdapter adapter = new BookmarkAdapter(bookmarks, 
                pageNumber -> {
                    recyclerView.scrollToPosition(pageNumber);
                    dialog.dismiss();
                },
                bookmark -> {
                    pdfViewModel.deleteBookmark(bookmark);
                });
            bookmarksRecyclerView.setAdapter(adapter);

            dialog.show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pdfId != -1 && layoutManager != null) {
            int currentPage = layoutManager.findFirstVisibleItemPosition();
            if (currentPage != RecyclerView.NO_POSITION) {
                prefs.edit().putInt(String.valueOf(pdfId), currentPage).apply();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
