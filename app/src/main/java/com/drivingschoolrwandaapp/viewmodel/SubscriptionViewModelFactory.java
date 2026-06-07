package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.repository.SubscriptionRepository;

public class SubscriptionViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final SubscriptionRepository repository;

    public SubscriptionViewModelFactory(@NonNull Application application, @NonNull SubscriptionRepository repository) {
        this.application = application;
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SubscriptionViewModel.class)) {
            return (T) new SubscriptionViewModel(application, repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
