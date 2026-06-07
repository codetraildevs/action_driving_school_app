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

    @Inject
    public UserViewModel(@NonNull Application application, UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
    }

    public void login(String email, String password, String deviceId) {
        userRepository.login(email, password, deviceId).observeForever(loginResult::setValue);
    }

    public LiveData<Resource<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public void forgotPassword(String email) {
        userRepository.forgotPassword(email).observeForever(forgotPasswordResult::setValue);
    }

    public LiveData<Resource<ApiResponse<Void>>> getForgotPasswordResult() {
        return forgotPasswordResult;
    }

    public void verifyOtp(String email, String otp) {
        userRepository.verifyOtp(email, otp).observeForever(verifyOtpResult::setValue);
    }

    public LiveData<Resource<ApiResponse<String>>> getVerifyOtpResult() {
        return verifyOtpResult;
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        userRepository.resetPassword(token, newPassword, confirmPassword)
                .observeForever(resetPasswordResult::setValue);
    }

    public LiveData<Resource<ApiResponse<Void>>> getResetPasswordResult() {
        return resetPasswordResult;
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        userRepository.changePassword(currentPassword, newPassword, confirmPassword).observeForever(changePasswordResult::setValue);
    }

    public LiveData<Resource<ApiResponse<Void>>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public void loadProfile() {
        userRepository.getProfile().observeForever(userLiveData::setValue);
    }
    
    public void updateUser(User user) {
        userRepository.updateUser(user);
    }
    
    public void sleepSubscription(int languageId) {
        userRepository.sleepSubscription(languageId).observeForever(sleepSubscriptionResult::setValue);
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
