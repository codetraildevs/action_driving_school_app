package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.drivingschoolrwandaapp.models.entities.AdminDashboardStats;
import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.drivingschoolrwandaapp.models.entities.AdminUser;
import com.drivingschoolrwandaapp.models.entities.AdminUserDetail;
import com.drivingschoolrwandaapp.repository.AdminRepository;
import com.drivingschoolrwandaapp.repository.Resource;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminViewModel extends AndroidViewModel {

    private final AdminRepository adminRepository;

    private final MutableLiveData<Resource<AdminDashboardStats>> dashboardStats = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<AdminUser>>> users = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<AdminRequest>>> requests = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminUserDetail>> userDetail = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<AdminRequest>>> userRequests = new MutableLiveData<>();

    // Track current observers so we can clean up before re-creating them
    // (mirrors the UserViewModel pattern to avoid observer leaks / stale results).
    private LiveData<Resource<AdminDashboardStats>> currentDashboardLiveData = null;
    private Observer<Resource<AdminDashboardStats>> currentDashboardObserver = null;
    private LiveData<Resource<List<AdminUser>>> currentUsersLiveData = null;
    private Observer<Resource<List<AdminUser>>> currentUsersObserver = null;
    private LiveData<Resource<List<AdminRequest>>> currentRequestsLiveData = null;
    private Observer<Resource<List<AdminRequest>>> currentRequestsObserver = null;
    private LiveData<Resource<AdminUserDetail>> currentUserDetailLiveData = null;
    private Observer<Resource<AdminUserDetail>> currentUserDetailObserver = null;
    private LiveData<Resource<List<AdminRequest>>> currentUserRequestsLiveData = null;
    private Observer<Resource<List<AdminRequest>>> currentUserRequestsObserver = null;

    @Inject
    public AdminViewModel(@NonNull Application application, AdminRepository adminRepository) {
        super(application);
        this.adminRepository = adminRepository;
    }

    public LiveData<Resource<AdminDashboardStats>> getDashboardStats() {
        return dashboardStats;
    }

    public LiveData<Resource<List<AdminUser>>> getUsers() {
        return users;
    }

    public LiveData<Resource<List<AdminRequest>>> getRequests() {
        return requests;
    }

    public LiveData<Resource<AdminUserDetail>> getUserDetail() {
        return userDetail;
    }

    public LiveData<Resource<List<AdminRequest>>> getUserRequests() {
        return userRequests;
    }

    /**
     * Refreshes dashboard stats. Safe to call repeatedly: the previous
     * observer is detached first, and the current one removes itself once a
     * terminal (non-loading) state is reached, so no observers leak and stale
     * results from an earlier refresh never surface.
     */
    public void refreshDashboard() {
        if (currentDashboardLiveData != null && currentDashboardObserver != null) {
            currentDashboardLiveData.removeObserver(currentDashboardObserver);
        }
        currentDashboardLiveData = adminRepository.fetchDashboardStats();
        currentDashboardObserver = resource -> {
            dashboardStats.setValue(resource);
            if (resource.getStatus() != Resource.Status.LOADING) {
                currentDashboardLiveData.removeObserver(currentDashboardObserver);
                currentDashboardLiveData = null;
                currentDashboardObserver = null;
            }
        };
        currentDashboardLiveData.observeForever(currentDashboardObserver);
    }

    public void refreshUsers() {
        if (currentUsersLiveData != null && currentUsersObserver != null) {
            currentUsersLiveData.removeObserver(currentUsersObserver);
        }
        currentUsersLiveData = adminRepository.fetchUsers();
        currentUsersObserver = resource -> {
            users.setValue(resource);
            if (resource.getStatus() != Resource.Status.LOADING) {
                currentUsersLiveData.removeObserver(currentUsersObserver);
                currentUsersLiveData = null;
                currentUsersObserver = null;
                // The dashboard aggregates user data — when fresh users load
                // (first tab visit, pull-to-refresh, retry), keep its stats in
                // sync so returning to the tab shows current totals.
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    refreshDashboard();
                }
            }
        };
        currentUsersLiveData.observeForever(currentUsersObserver);
    }

    public void refreshRequests() {
        if (currentRequestsLiveData != null && currentRequestsObserver != null) {
            currentRequestsLiveData.removeObserver(currentRequestsObserver);
        }
        currentRequestsLiveData = adminRepository.fetchRequests();
        currentRequestsObserver = resource -> {
            requests.setValue(resource);
            if (resource.getStatus() != Resource.Status.LOADING) {
                currentRequestsLiveData.removeObserver(currentRequestsObserver);
                currentRequestsLiveData = null;
                currentRequestsObserver = null;
                // The dashboard shows request counts/activity — when fresh
                // requests load, refresh its stats as well.
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    refreshDashboard();
                }
            }
        };
        currentRequestsLiveData.observeForever(currentRequestsObserver);
    }

    /**
     * Loads one user's detail + their Irembo requests for the detail dialog.
     * Safe to call repeatedly for different users: the previous observers are
     * detached first, and each removes itself once a terminal state arrives.
     */
    public void refreshUserDetail(int userId) {
        if (currentUserDetailLiveData != null && currentUserDetailObserver != null) {
            currentUserDetailLiveData.removeObserver(currentUserDetailObserver);
        }
        if (currentUserRequestsLiveData != null && currentUserRequestsObserver != null) {
            currentUserRequestsLiveData.removeObserver(currentUserRequestsObserver);
        }

        currentUserDetailLiveData = adminRepository.fetchUserDetail(userId);
        currentUserDetailObserver = resource -> {
            userDetail.setValue(resource);
            if (resource.getStatus() != Resource.Status.LOADING) {
                currentUserDetailLiveData.removeObserver(currentUserDetailObserver);
                currentUserDetailLiveData = null;
                currentUserDetailObserver = null;
            }
        };
        currentUserDetailLiveData.observeForever(currentUserDetailObserver);

        currentUserRequestsLiveData = adminRepository.fetchUserRequests(userId);
        currentUserRequestsObserver = resource -> {
            userRequests.setValue(resource);
            if (resource.getStatus() != Resource.Status.LOADING) {
                currentUserRequestsLiveData.removeObserver(currentUserRequestsObserver);
                currentUserRequestsLiveData = null;
                currentUserRequestsObserver = null;
            }
        };
        currentUserRequestsLiveData.observeForever(currentUserRequestsObserver);
    }
}
