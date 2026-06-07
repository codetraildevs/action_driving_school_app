package com.drivingschoolrwandaapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.drivingschoolrwandaapp.database.entities.Bookmark;
import com.drivingschoolrwandaapp.repository.PdfRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PdfViewModel extends ViewModel {

    private final PdfRepository pdfRepository;

    @Inject
    public PdfViewModel(PdfRepository pdfRepository) {
        this.pdfRepository = pdfRepository;
    }

    public void addBookmark(int pdfId, int page, String name) {
        pdfRepository.addBookmark(pdfId, page, name);
    }

    public LiveData<List<Bookmark>> getBookmarks(int pdfId) {
        return pdfRepository.getBookmarks(pdfId);
    }

    public void deleteBookmark(Bookmark bookmark) {
        pdfRepository.deleteBookmark(bookmark);
    }
}
