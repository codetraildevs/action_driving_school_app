package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.LoginResponse;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.UserRepository;

import androidx.lifecycle.Observer;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class UserViewModel extends AndroidViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<Resource<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApiResponse<Void>>> resetPasswordResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApiResponse<Void>>> forgotPasswordResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApiResponse<String>>> verifyOtpResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<LoginResponse>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApiResponse<Void>>> changePasswordResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<ApiResponse<Void>>> sleepSubscriptionResult = new MutableLiveData<>();

    // Track current observers so we can clean up before re-creating them
    private LiveData<Resource<LoginResponse>> currentLoginLiveData = null;
    private Observer<Resource<LoginResponse>> currentLoginObserver = null;
    private LiveData<Resource<ApiResponse<Void>>> currentForgotPasswordLiveData = null;
    private Observer<Resource<ApiResponse<Void>>> currentForgotPasswordObserver = null;
    private LiveData<Resource<ApiResponse<String>>> currentVerifyOtpLiveData = null;
    private Observer<Resource<ApiResponse<String>>> currentVerifyOtpObserver = null;
    private LiveData<Resource<ApiResponse<Void>>> currentResetPasswordLiveData = null;
    private Observer<Resource<ApiResponse<Void>>> currentResetPasswordObserver = null;
    private LiveData<Resource<ApiResponse<Void>>> currentChangePasswordLiveData = null;
    private Observer<Resource<ApiResponse<Void>>> currentChangePasswordObserver = null;
    private LiveData<Resource<User>> currentProfileLiveData = null;
    private Observer<Resource<User>> currentProfileObserver = null;
    private LiveData<Resource<ApiResponse<Void>>> currentSleepSubscriptionLiveData = null;
    private Observer<Resource<ApiResponse<Void>>> currentSleepSubscriptionObserver = null;

    @Inject
    public UserViewModel(@NonNull Application application, UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
    }

    public void login(String email, String password, String deviceId) {
        // Clean up any previous login observer
        if (currentLoginLiveData != null && currentLoginObserver != null) {
            currentLoginLiveData.removeObserver(currentLoginObserver);
        }
        currentLoginLiveData = userRepository.login(email, password, deviceId);
        currentLoginObserver = resource -> {
            loginResult.setValue(resource);
            // When the API completes (SUCCESS or ERROR), clean up
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentLoginLiveData.removeObserver(currentLoginObserver);
                currentLoginLiveData = null;
                currentLoginObserver = null;
            }
        };
        currentLoginLiveData.observeForever(currentLoginObserver);
    }

    public LiveData<Resource<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public void forgotPassword(String email) {
        if (currentForgotPasswordLiveData != null && currentForgotPasswordObserver != null) {
            currentForgotPasswordLiveData.removeObserver(currentForgotPasswordObserver);
        }
        currentForgotPasswordLiveData = userRepository.forgotPassword(email);
        currentForgotPasswordObserver = resource -> {
            forgotPasswordResult.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentForgotPasswordLiveData.removeObserver(currentForgotPasswordObserver);
                currentForgotPasswordLiveData = null;
                currentForgotPasswordObserver = null;
            }
        };
        currentForgotPasswordLiveData.observeForever(currentForgotPasswordObserver);
    }

    public LiveData<Resource<ApiResponse<Void>>> getForgotPasswordResult() {
        return forgotPasswordResult;
    }

    public void verifyOtp(String email, String otp) {
        if (currentVerifyOtpLiveData != null && currentVerifyOtpObserver != null) {
            currentVerifyOtpLiveData.removeObserver(currentVerifyOtpObserver);
        }
        currentVerifyOtpLiveData = userRepository.verifyOtp(email, otp);
        currentVerifyOtpObserver = resource -> {
            verifyOtpResult.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentVerifyOtpLiveData.removeObserver(currentVerifyOtpObserver);
                currentVerifyOtpLiveData = null;
                currentVerifyOtpObserver = null;
            }
        };
        currentVerifyOtpLiveData.observeForever(currentVerifyOtpObserver);
    }

    public LiveData<Resource<ApiResponse<String>>> getVerifyOtpResult() {
        return verifyOtpResult;
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (currentResetPasswordLiveData != null && currentResetPasswordObserver != null) {
            currentResetPasswordLiveData.removeObserver(currentResetPasswordObserver);
        }
        currentResetPasswordLiveData = userRepository.resetPassword(token, newPassword, confirmPassword);
        currentResetPasswordObserver = resource -> {
            resetPasswordResult.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentResetPasswordLiveData.removeObserver(currentResetPasswordObserver);
                currentResetPasswordLiveData = null;
                currentResetPasswordObserver = null;
            }
        };
        currentResetPasswordLiveData.observeForever(currentResetPasswordObserver);
    }

    public LiveData<Resource<ApiResponse<Void>>> getResetPasswordResult() {
        return resetPasswordResult;
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (currentChangePasswordLiveData != null && currentChangePasswordObserver != null) {
            currentChangePasswordLiveData.removeObserver(currentChangePasswordObserver);
        }
        currentChangePasswordLiveData = userRepository.changePassword(currentPassword, newPassword, confirmPassword);
        currentChangePasswordObserver = resource -> {
            changePasswordResult.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentChangePasswordLiveData.removeObserver(currentChangePasswordObserver);
                currentChangePasswordLiveData = null;
                currentChangePasswordObserver = null;
            }
        };
        currentChangePasswordLiveData.observeForever(currentChangePasswordObserver);
    }

    public LiveData<Resource<ApiResponse<Void>>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public void loadProfile() {
        if (currentProfileLiveData != null && currentProfileObserver != null) {
            currentProfileLiveData.removeObserver(currentProfileObserver);
        }
        currentProfileLiveData = userRepository.getProfile();
        currentProfileObserver = resource -> {
            userLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentProfileLiveData.removeObserver(currentProfileObserver);
                currentProfileLiveData = null;
                currentProfileObserver = null;
            }
        };
        currentProfileLiveData.observeForever(currentProfileObserver);
    }
    
    public void updateUser(User user) {
        userRepository.updateUser(user);
    }

    /**
     * Clears cached user data from Room so the next profile load starts fresh.
     * Call this on successful login before navigating to the main app.
     */
    public void clearCachedUser() {
        userRepository.clearCachedUser();
    }

    /**
     * Saves the login response user to Room so the profile screen can display
     * data immediately while the full profile fetch runs in the background.
     */
    public void saveLoginUser(com.drivingschoolrwandaapp.models.entities.User user) {
        userRepository.saveLoginUser(user);
    }
    
    public void sleepSubscription(int languageId) {
        if (currentSleepSubscriptionLiveData != null && currentSleepSubscriptionObserver != null) {
            currentSleepSubscriptionLiveData.removeObserver(currentSleepSubscriptionObserver);
        }
        currentSleepSubscriptionLiveData = userRepository.sleepSubscription(languageId);
        currentSleepSubscriptionObserver = resource -> {
            sleepSubscriptionResult.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS || resource.status == Resource.Status.ERROR) {
                currentSleepSubscriptionLiveData.removeObserver(currentSleepSubscriptionObserver);
                currentSleepSubscriptionLiveData = null;
                currentSleepSubscriptionObserver = null;
            }
        };
        currentSleepSubscriptionLiveData.observeForever(currentSleepSubscriptionObserver);
    }
    
    public LiveData<Resource<ApiResponse<Void>>> getSleepSubscriptionResult() {
        return sleepSubscriptionResult;
    }

    public void logout() {
        userRepository.logout();
    }
    
    public void deleteAccount() {
        userRepository.deleteAccount();
    }

    public LiveData<Resource<User>> getUserLiveData() {
        return userLiveData;
    }
}
