package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.data.models.LearningMaterial;
import com.drivingschoolrwandaapp.data.models.LearningMaterialResponse;
import com.drivingschoolrwandaapp.repository.LearningMaterialRepository;
import com.drivingschoolrwandaapp.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class LearningMaterialViewModel extends AndroidViewModel {

    private final LearningMaterialRepository repository;
    private final MutableLiveData<List<LearningMaterial>> materials = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<DownloadState> downloadStatus = new MutableLiveData<>();
    private final SharedPreferences sharedPreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final File cacheFile;

    private static final long EXPIRATION_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    @Inject
    public LearningMaterialViewModel(@NonNull Application application, @NonNull LearningMaterialRepository repository) {
        super(application);
        this.repository = repository;
        this.sharedPreferences = application.getSharedPreferences("DownloadPrefs", Context.MODE_PRIVATE);
        this.cacheFile = new File(application.getCacheDir(), "materials_cache.json");
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }

    public LiveData<List<LearningMaterial>> getMaterials() {
        return materials;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<DownloadState> getDownloadStatus() {
        return downloadStatus;
    }

    public void fetchLearningMaterials(int page, int limit) {
        isLoading.setValue(true);
        repository.getLearningMaterials(page, limit).enqueue(new Callback<LearningMaterialResponse>() {
            @Override
            public void onResponse(@NonNull Call<LearningMaterialResponse> call, @NonNull Response<LearningMaterialResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LearningMaterial> fetchedMaterials = response.body().getMaterials();
                    checkDownloadedStatus(fetchedMaterials);
                    materials.setValue(fetchedMaterials);
                    cacheMaterialsAsync(fetchedMaterials);
                } else {
                    loadFromCacheAsync("Couldn't refresh from server.");
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<LearningMaterialResponse> call, @NonNull Throwable t) {
                loadFromCacheAsync("You are offline. Showing downloaded content.");
                isLoading.setValue(false);
            }
        });
    }

    private void executeSafely(Runnable runnable) {
        try {
            if (!executor.isShutdown() && !executor.isTerminated()) {
                executor.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("LearningMaterialVM", "Task rejected, executor is shutting down", e);
        }
    }

    private void loadFromCacheAsync(String message) {
        executeSafely(() -> {
            if (cacheFile.exists()) {
                try (FileReader reader = new FileReader(cacheFile)) {
                    Type listType = new TypeToken<List<LearningMaterial>>() {}.getType();
                    List<LearningMaterial> cachedMaterials = gson.fromJson(reader, listType);
                    if (cachedMaterials != null && !cachedMaterials.isEmpty()) {
                        checkDownloadedStatus(cachedMaterials);
                        materials.postValue(cachedMaterials);
                        toastMessage.postValue(message);
                    } else {
                        error.postValue("No offline content available.");
                    }
                } catch (IOException e) {
                    Log.e("LearningMaterialVM", "Could not load offline content from cache", e);
                    error.postValue("Could not load offline content.");
                }
            } else {
                error.postValue("No internet and no offline content available.");
            }
        });
    }

    private void cacheMaterialsAsync(List<LearningMaterial> materialsToCache) {
        executeSafely(() -> {
            try (FileWriter writer = new FileWriter(cacheFile)) {
                gson.toJson(materialsToCache, writer);
            } catch (IOException e) {
                Log.e("LearningMaterialVM", "Failed to cache materials to disk", e);
            }
        });
    }

    public void downloadLearningMaterial(LearningMaterial material) {
        if (material == null) return;
        if (material.isDownloaded()) return;

        downloadStatus.postValue(new DownloadState(DownloadState.Status.DOWNLOADING, material.getId()));
        repository.downloadLearningMaterial(material.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executeSafely(() -> {
                        try {
                            File destinationFile = getDestinationFile(material);
                            boolean success = writeResponseBodyToDisk(response.body(), destinationFile);

                            if (success) {
                                long downloadTime = System.currentTimeMillis();
                                sharedPreferences.edit().putLong("download_" + material.getId(), downloadTime).apply();
                                material.setDownloaded(true);
                                material.setFileSize(destinationFile.length());
                                long remainingHours = TimeUnit.MILLISECONDS.toHours(EXPIRATION_DURATION_MS);
                                material.setHoursUntilExpiration(remainingHours);
                                downloadStatus.postValue(new DownloadState(DownloadState.Status.SUCCESS, material.getId()));
                            } else {
                                downloadStatus.postValue(new DownloadState(DownloadState.Status.FAILURE, material.getId()));
                            }
                        } catch (Exception e) {
                            Log.e("LearningMaterialVM", "Failed to process downloaded file", e);
                            downloadStatus.postValue(new DownloadState(DownloadState.Status.FAILURE, material.getId()));
                        }
                    });
                } else {
                    downloadStatus.postValue(new DownloadState(DownloadState.Status.FAILURE, material.getId()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                downloadStatus.postValue(new DownloadState(DownloadState.Status.FAILURE, material.getId()));
            }
        });
    }

    private void checkDownloadedStatus(List<LearningMaterial> materialsList) {
        if (materialsList == null) return;
        // File I/O (exists, length, delete) and SharedPreferences reads are fast operations
        // safe to run on the main thread, as they were originally. The callers handle
        // background threading when needed (e.g., loadFromCacheAsync) and post the result.
        long currentTime = System.currentTimeMillis();

        for (LearningMaterial material : materialsList) {
            File file = getDestinationFile(material);
            long downloadTime = sharedPreferences.getLong("download_" + material.getId(), -1);

            if (downloadTime != -1) {
                long elapsedTime = currentTime - downloadTime;
                if (elapsedTime > EXPIRATION_DURATION_MS) {
                    if (file.exists()) {
                        file.delete();
                    }
                    sharedPreferences.edit().remove("download_" + material.getId()).apply();
                    material.setDownloaded(false);
                    material.setHoursUntilExpiration(-1);
                    material.setFileSize(-1);
                } else {
                    if (file.exists()) {
                        material.setDownloaded(true);
                        material.setFileSize(file.length());
                        long remainingTimeMs = EXPIRATION_DURATION_MS - elapsedTime;
                        material.setHoursUntilExpiration(TimeUnit.MILLISECONDS.toHours(remainingTimeMs));
                    } else {
                        sharedPreferences.edit().remove("download_" + material.getId()).apply();
                        material.setDownloaded(false);
                        material.setHoursUntilExpiration(-1);
                        material.setFileSize(-1);
                    }
                }
            } else {
                if (file.exists()) {
                    file.delete();
                }
                material.setDownloaded(false);
                material.setHoursUntilExpiration(-1);
                material.setFileSize(-1);
            }
        }
    }

    private File getDestinationFile(LearningMaterial material) {
        File internalStorageDir = getApplication().getFilesDir();
        String fileName = FileUtils.getSafeFileName(material);
        return new File(internalStorageDir, fileName);
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, File destinationFile) {
        try {
            long fileSize = body.contentLength();
            long fileSizeDownloaded = 0;

            try (InputStream inputStream = body.byteStream();
                 OutputStream outputStream = new FileOutputStream(destinationFile)) {

                byte[] fileReader = new byte[4096];
                int read;

                while ((read = inputStream.read(fileReader)) != -1) {
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                }
                outputStream.flush();
            }

            // Only require an exact size match when the server actually told us the
            // expected length. When the response is gzip/compressed (OkHttp reports
            // contentLength() == -1 after transparent decompression) or sent with
            // chunked transfer-encoding, contentLength() is -1/0 even though the
            // whole file arrived. Treating that as a failure made every download
            // "fail" on cPanel-hosted servers behind compression middleware.
            boolean lengthKnown = fileSize > 0;
            if (fileSizeDownloaded > 0 && (!lengthKnown || fileSizeDownloaded == fileSize)) {
                return true;
            } else {
                if (destinationFile.exists()) {
                    destinationFile.delete();
                }
                return false;
            }
        } catch (IOException e) {
            Log.e("LearningMaterialVM", "Failed to write downloaded file to disk", e);
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            return false;
        }
    }
}
