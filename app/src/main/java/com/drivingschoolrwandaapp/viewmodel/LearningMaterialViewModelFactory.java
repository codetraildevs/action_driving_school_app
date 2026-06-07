package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.drivingschoolrwandaapp.repository.LearningMaterialRepository;

/**
 * @deprecated Use {@link LearningMaterialViewModel} directly with Hilt injection.
 * This factory is kept for backward compatibility but is no longer needed.
 */
@Deprecated
public class LearningMaterialViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    private final LearningMaterialRepository repository;

    public LearningMaterialViewModelFactory(@NonNull Application application, @NonNull LearningMaterialRepository repository) {
        this.application = application;
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LearningMaterialViewModel.class)) {
            return (T) new LearningMaterialViewModel(application, repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
