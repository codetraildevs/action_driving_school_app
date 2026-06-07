package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.WhatsAppRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WhatsAppViewModel extends AndroidViewModel {
    private final WhatsAppRepository repository;

    @Inject
    public WhatsAppViewModel(@NonNull Application application, WhatsAppRepository repository) {
        super(application);
        this.repository = repository;
    }

    public LiveData<Resource<List<WhatsAppGroup>>> getWhatsAppGroups() {
        return repository.getWhatsAppGroups();
    }
}
