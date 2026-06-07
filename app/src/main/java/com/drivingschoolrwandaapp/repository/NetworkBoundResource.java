package com.drivingschoolrwandaapp.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.drivingschoolrwandaapp.models.response.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class NetworkBoundResource<ResultType, RequestType> {

    private final MediatorLiveData<Resource<ResultType>> result = new MediatorLiveData<>();

    @MainThread
    public NetworkBoundResource() {
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

    private void fetchFromNetwork(final LiveData<ResultType> dbSource) {
        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));
        createCall().enqueue(new Callback<RequestType>() {
            @Override
            public void onResponse(@NonNull Call<RequestType> call, @NonNull Response<RequestType> response) {
                result.removeSource(dbSource);
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        saveCallResult(processResponse(response));
                        new Handler(Looper.getMainLooper()).post(() ->
                                result.addSource(loadFromDb(), newData -> result.setValue(Resource.success(newData)))
                        );
                    }).start();
                } else {
                    onFetchFailed();
                    result.addSource(dbSource, newData -> result.setValue(Resource.error(response.message(), newData)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RequestType> call, @NonNull Throwable t) {
                onFetchFailed();
                result.removeSource(dbSource);
                result.addSource(dbSource, newData -> result.setValue(Resource.error(t.getMessage(), newData)));
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
