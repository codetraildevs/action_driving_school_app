package com.drivingschoolrwandaapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.drivingschoolrwandaapp.database.entities.Bookmark
import com.drivingschoolrwandaapp.repository.PdfRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Unit tests for [PdfViewModel].
 *
 * [PdfViewModel] is a thin delegating wrapper over [PdfRepository] — it has no
 * business logic of its own. These tests verify the delegation contract:
 * arguments are forwarded unchanged, and the [LiveData] exposed by the
 * repository is passed straight through (including edge cases like an empty
 * list and an absent / null repository result).
 */
class PdfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: PdfRepository
    private lateinit var viewModel: PdfViewModel

    @Before
    fun setUp() {
        repository = mock(PdfRepository::class.java)
        viewModel = PdfViewModel(repository)
    }

    // ---------------------------------------------------------------------------
    // addBookmark — delegation
    // ---------------------------------------------------------------------------

    @Test
    fun `addBookmark delegates with the exact arguments`() {
        viewModel.addBookmark(5, 12, "Chapter 3")

        verify(repository).addBookmark(5, 12, "Chapter 3")
    }

    @Test
    fun `addBookmark forwards zero page number`() {
        viewModel.addBookmark(9, 0, "Cover")

        verify(repository).addBookmark(9, 0, "Cover")
    }

    @Test
    fun `addBookmark forwards null name`() {
        viewModel.addBookmark(3, 7, null)

        verify(repository).addBookmark(3, 7, null)
    }

    // ---------------------------------------------------------------------------
    // getBookmarks — passthrough
    // ---------------------------------------------------------------------------

    @Test
    fun `getBookmarks returns the repository live data instance`() {
        val liveData = MutableLiveData<List<Bookmark>>()
        `when`(repository.getBookmarks(5)).thenReturn(liveData)

        val result: LiveData<List<Bookmark>> = viewModel.getBookmarks(5)

        assertSame("ViewModel must not wrap or copy the repository LiveData", liveData, result)
    }

    @Test
    fun `getBookmarks passes the requested pdfId to the repository`() {
        val liveData = MutableLiveData<List<Bookmark>>()
        `when`(repository.getBookmarks(42)).thenReturn(liveData)

        viewModel.getBookmarks(42)

        verify(repository).getBookmarks(42)
    }

    @Test
    fun `getBookmarks surfaces empty list from repository`() {
        val liveData = MutableLiveData<List<Bookmark>>(emptyList())
        `when`(repository.getBookmarks(1)).thenReturn(liveData)

        val result = viewModel.getBookmarks(1)

        assertEquals(emptyList<Bookmark>(), result.value)
    }

    @Test
    fun `getBookmarks does not guard a null repository result`() {
        // Unstubbed mock → the repository returns null, and the ViewModel passes it
        // through untouched (no defensive null handling). Documents current behavior.
        val result = viewModel.getBookmarks(999)

        assertNull("ViewModel must pass through the repository's null result", result)
    }

    // ---------------------------------------------------------------------------
    // deleteBookmark — delegation
    // ---------------------------------------------------------------------------

    @Test
    fun `deleteBookmark delegates with the exact bookmark`() {
        val bookmark = Bookmark(id = 11, pdfId = 5, pageNumber = 12, name = "Chapter 3")

        viewModel.deleteBookmark(bookmark)

        verify(repository).deleteBookmark(bookmark)
    }

    @Test
    fun `deleteBookmark passes null bookmark through`() {
        viewModel.deleteBookmark(null)

        verify(repository).deleteBookmark(null)
    }
}
