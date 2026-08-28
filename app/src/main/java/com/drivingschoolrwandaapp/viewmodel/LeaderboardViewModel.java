package com.drivingschoolrwandaapp.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.drivingschoolrwandaapp.models.entities.LeaderboardEntry;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.LeaderboardRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the global leaderboard screen.
 *
 * Exposes a single [LiveData] that emits loading → success/error states.
 * Call [refresh] to re-fetch the leaderboard data.
 */
@HiltViewModel
public class LeaderboardViewModel extends ViewModel {

    private final LeaderboardRepository leaderboardRepository;
    private final MutableLiveData<Resource<List<LeaderboardEntry>>> leaderboard =
            new MutableLiveData<>();

    @Inject
    public LeaderboardViewModel(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
        refresh();
    }

    public LiveData<Resource<List<LeaderboardEntry>>> getLeaderboard() {
        return leaderboard;
    }

    /**
     * Fetches the leaderboard from the API and posts the result.
     */
    public void refresh() {
        leaderboard.setValue(Resource.loading(null));
        leaderboardRepository.getLeaderboard().observeForever(resource -> {
            if (resource != null) {
                leaderboard.setValue(resource);
            }
        });
    }
}
