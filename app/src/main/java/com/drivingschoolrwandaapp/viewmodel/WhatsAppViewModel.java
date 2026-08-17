package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.WhatsAppRepository;
import com.drivingschoolrwandaapp.utils.ErrorUtils;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class WhatsAppViewModel extends AndroidViewModel {

    private static final long LOADING_TIMEOUT_MS = 30_000;

    private final WhatsAppRepository repository;
    private final MutableLiveData<Resource<List<WhatsAppGroup>>> groups = new MutableLiveData<>();
    // Created lazily so unit tests (which have no main Looper) can still run.
    // On a real device the main Looper always exists, so the watchdog is armed.
    private Handler timeoutHandler = null;

    private Call<List<WhatsAppGroup>> inFlightCall;

    private final Runnable timeoutRunnable = () -> {
        // The server never answered within the bound — cancel the request and
        // surface an error instead of leaving the page stuck on the spinner.
        if (inFlightCall != null && !inFlightCall.isCanceled()) {
            inFlightCall.cancel();
        }
        Resource<List<WhatsAppGroup>> current = groups.getValue();
        if (current != null && current.status == Resource.Status.LOADING) {
            groups.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(
                    getApplication(), new java.io.IOException("timeout")), null));
        }
    };

    @Inject
    public WhatsAppViewModel(@NonNull Application application, WhatsAppRepository repository) {
        super(application);
        this.repository = repository;
    }

    private Handler getTimeoutHandler() {
        if (timeoutHandler == null) {
            try {
                Looper mainLooper = Looper.getMainLooper();
                if (mainLooper != null) {
                    timeoutHandler = new Handler(mainLooper);
                }
            } catch (RuntimeException e) {
                // Unit-test environment (android.jar stubs throw here): the
                // watchdog simply stays disarmed. On a real device the main
                // Looper always exists, so the 30s bound is always armed.
                timeoutHandler = null;
            }
        }
        return timeoutHandler;
    }

    @Override
    protected void onCleared() {
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        if (inFlightCall != null) {
            inFlightCall.cancel();
        }
        super.onCleared();
    }

    /**
     * Single LiveData owned by the ViewModel — the activity observes it once.
     */
    public LiveData<Resource<List<WhatsAppGroup>>> getWhatsAppGroups() {
        return groups;
    }

    /**
     * Fetch (or re-fetch) the groups. Re-fetching cancels any in-flight request
     * so repeated taps on Retry never pile up concurrent network calls.
     */
    public void fetchWhatsAppGroups() {
        if (inFlightCall != null && !inFlightCall.isCanceled()) {
            inFlightCall.cancel();
        }
        Handler handler = getTimeoutHandler();
        if (handler != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

        groups.setValue(Resource.loading(null));
        inFlightCall = repository.getWhatsAppGroups();
        inFlightCall.enqueue(new Callback<List<WhatsAppGroup>>() {
            @Override
            public void onResponse(@NonNull Call<List<WhatsAppGroup>> call,
                                   @NonNull Response<List<WhatsAppGroup>> response) {
                if (call.isCanceled()) return;
                if (handler != null) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        groups.setValue(Resource.success(response.body()));
                    } else {
                        groups.setValue(Resource.error(getApplication().getString(R.string.no_whatsapp_groups), null));
                    }
                } else {
                    groups.setValue(Resource.error(getApplication().getString(R.string.whatsapp_fetch_failed), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WhatsAppGroup>> call, @NonNull Throwable t) {
                if (call.isCanceled()) return;
                if (handler != null) {
                    handler.removeCallbacks(timeoutRunnable);
                }
                groups.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(getApplication(), t), null));
            }
        });

        if (handler != null) {
            handler.postDelayed(timeoutRunnable, LOADING_TIMEOUT_MS);
        }
    }
}
