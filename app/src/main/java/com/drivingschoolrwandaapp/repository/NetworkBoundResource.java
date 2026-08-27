package com.drivingschoolrwandaapp.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.utils.ErrorUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class NetworkBoundResource<ResultType, RequestType> {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

    private final MediatorLiveData<Resource<ResultType>> result = new MediatorLiveData<>();
    private final Context context;

    @MainThread
    public NetworkBoundResource(Context context) {
        this.context = context.getApplicationContext();
        result.setValue(Resource.loading(null));
        LiveData<ResultType> dbSource = loadFromDb();
        result.addSource(dbSource, data -> {
            result.removeSource(dbSource);
            if (shouldFetch(data)) {
                fetchFromNetwork(dbSource);
            } else {
                result.addSource(dbSource, newData -> result.setValue(Resource.success(newData)));
            }
        });
    }

    private static void executeSafely(Runnable runnable) {
        try {
            if (!IO_EXECUTOR.isShutdown() && !IO_EXECUTOR.isTerminated()) {
                IO_EXECUTOR.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("NetworkBoundResource", "Task rejected, executor is shutting down", e);
        }
    }

    private void fetchFromNetwork(final LiveData<ResultType> dbSource) {
        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));
        createCall().enqueue(new Callback<RequestType>() {
            @Override
            public void onResponse(@NonNull Call<RequestType> call, @NonNull Response<RequestType> response) {
                result.removeSource(dbSource);
                if (response.isSuccessful()) {
                    executeSafely(() -> {
                        saveCallResult(processResponse(response));
                        new Handler(Looper.getMainLooper()).post(() ->
                                result.addSource(loadFromDb(), newData -> result.setValue(Resource.success(newData)))
                        );
                    });
                } else {
                    onFetchFailed();
                    // HTTP status text (e.g. "Unauthorized") is not localized — show a
                    // translated generic message instead.
                    result.addSource(dbSource, newData -> result.setValue(
                            Resource.error(context.getString(com.drivingschoolrwandaapp.R.string.something_went_wrong), newData)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RequestType> call, @NonNull Throwable t) {
                onFetchFailed();
                result.removeSource(dbSource);
                result.addSource(dbSource, newData -> result.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(context, t), newData)));
            }
        });
    }

    @WorkerThread
    protected RequestType processResponse(Response<RequestType> response) {
        return response.body();
    }

    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestType item);

    @MainThread
    protected boolean shouldFetch(ResultType data) {
        return true;
    }

    @NonNull
    @MainThread
    protected abstract LiveData<ResultType> loadFromDb();

    @NonNull
    @MainThread
    protected abstract Call<RequestType> createCall();

    @MainThread
    protected void onFetchFailed() {
    }

    public final LiveData<Resource<ResultType>> getAsLiveData() {
        return result;
    }
}
