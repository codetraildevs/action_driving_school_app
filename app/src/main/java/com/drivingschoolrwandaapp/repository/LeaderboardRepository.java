package com.drivingschoolrwandaapp.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.entities.LeaderboardEntry;
import com.drivingschoolrwandaapp.models.response.ApiResponse;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches the global leaderboard from the backend API.
 */
@Singleton
public class LeaderboardRepository {

    private static final String TAG = "LeaderboardRepository";
    private static final int DEFAULT_LIMIT = 50;

    private final ApiService apiService;

    @Inject
    public LeaderboardRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Fetches the leaderboard. Returns a {@link LiveData} that emits
     * {@link Resource#loading()} immediately, then either success or error.
     */
    public LiveData<Resource<List<LeaderboardEntry>>> getLeaderboard() {
        return getLeaderboard(DEFAULT_LIMIT);
    }

    public LiveData<Resource<List<LeaderboardEntry>>> getLeaderboard(int limit) {
        MutableLiveData<Resource<List<LeaderboardEntry>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        apiService.getLeaderboard(limit).enqueue(new Callback<ApiResponse<List<LeaderboardEntry>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<LeaderboardEntry>>> call,
                                   @NonNull Response<ApiResponse<List<LeaderboardEntry>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LeaderboardEntry>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.setValue(Resource.success(body.getData()));
                    } else {
                        String msg = body.getMessage() != null ? body.getMessage() : "Failed to load leaderboard";
                        result.setValue(Resource.error(msg, null));
                    }
                } else {
                    result.setValue(Resource.error("Server error: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<LeaderboardEntry>>> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Failed to fetch leaderboard", t);
                result.setValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }
}
