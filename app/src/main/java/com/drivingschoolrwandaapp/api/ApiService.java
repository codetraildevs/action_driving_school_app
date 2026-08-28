package com.drivingschoolrwandaapp.api;

import com.drivingschoolrwandaapp.data.models.LearningMaterialResponse;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.models.entities.Bookmark;
import com.drivingschoolrwandaapp.models.entities.Notification;
import com.drivingschoolrwandaapp.models.entities.PdfFile;
import com.drivingschoolrwandaapp.models.entities.LeaderboardEntry;
import com.drivingschoolrwandaapp.models.entities.User;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.drivingschoolrwandaapp.models.request.BookmarkRequest;
import com.drivingschoolrwandaapp.models.request.FirebaseTokenUpdateRequest;
import com.drivingschoolrwandaapp.models.request.ForgotPasswordRequest;
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest;
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest;
import com.drivingschoolrwandaapp.models.request.LoginRequest;
import com.drivingschoolrwandaapp.models.request.PasswordChangeRequest;
import com.drivingschoolrwandaapp.models.request.RefreshTokenRequest;
import com.drivingschoolrwandaapp.models.request.ResetPasswordRequest;
import com.drivingschoolrwandaapp.models.request.VerifyOtpRequest;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse;
import com.drivingschoolrwandaapp.models.response.LoginResponse;
import com.drivingschoolrwandaapp.models.response.PaginatedResponse;
import com.drivingschoolrwandaapp.models.response.PdfFilesResponse;
import com.drivingschoolrwandaapp.models.response.RegisterResponse;
import com.drivingschoolrwandaapp.models.response.SubscriptionPlansResponse;
import com.drivingschoolrwandaapp.models.response.UserSubscriptionResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {

    // Authentication endpoints
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<RegisterResponse> register(@Body User user);

    @POST("auth/refresh")
    Call<LoginResponse> refreshToken(@Body RefreshTokenRequest request);

    @POST("auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/verify-otp")
    Call<ApiResponse<String>> verifyOtp(@Body VerifyOtpRequest request);

    @POST("auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();
    
    @DELETE("users/delete/")
    Call<ApiResponse<Void>> deleteAccount();

    // User management
    @GET("users/profile")
    Call<ApiResponse<User>> getProfile();

    @PUT("users/profile")
    Call<ApiResponse<User>> updateProfile(@Body User user);

    @POST("auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body PasswordChangeRequest request);

    @POST("firebase/")
    Call<ApiResponse<Void>> updateFirebaseToken(@Body FirebaseTokenUpdateRequest request);

    // PDF Files (backed by /api/files; pass type=pdf to get only PDFs)
    @GET("files")
    Call<PdfFilesResponse> getPdfFiles(
            @Query("page") int page,
            @Query("pageSize") int pageSize,
            @Query("type") String type,
            @Query("search") String search
    );

    @GET("files/{id}")
    Call<ApiResponse<PdfFile>> getPdfFile(@Path("id") int id);

    @POST("files/{id}/bookmark")
    Call<ApiResponse<Bookmark>> addBookmark(@Path("id") int pdfId, @Body BookmarkRequest request);

    // Notifications
    @GET("notifications")
    Call<ApiResponse<PaginatedResponse<Notification>>> getNotifications(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @PUT("notifications/{id}/read")
    Call<ApiResponse<Void>> markNotificationAsRead(@Path("id") int notificationId);

    // Learning Materials
    @GET("learning-materials")
    Call<LearningMaterialResponse> getLearningMaterials(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @Streaming
    @GET("learning-materials/{id}/download")
    Call<ResponseBody> downloadLearningMaterial(@Path("id") int materialId);

    // Subscriptions
    @GET("subscriptions")
    Call<SubscriptionPlansResponse> getSubscriptionPlans();

    @GET("subscriptions/user")
    Call<UserSubscriptionResponse> getUserSubscription();

    @POST("subscriptions/user")
    Call<UserSubscriptionResponse> subscribeToPlan(@Query("planId") int planId);

    @POST("subscriptions/user")
    Call<ApiResponse<Void>> requestTestAccess(
            @Query("testNumber") int testNumber,
            @Query("days") int days,
            @Query("currentLanguageId") int currentLanguageId
    );

    @POST("subscriptions/user/cancel")
    Call<ApiResponse<Void>> cancelSubscription();
    
    @PUT("subscriptions/userRequests/sleep")
    Call<ApiResponse<Void>> sleepSubscription(@Query("languageId") int languageId);

    // Irembo Services
    @POST("irembo/driving")
    Call<ApiResponse<IremboPaymentResponse>> requestIremboLicense(@Body IremboLicenseRequest request);

    @POST("irembo/special")
    Call<ApiResponse<IremboPaymentResponse>> requestSpecialIremboService(@Body IremboSpecialRequest request);

    @GET("irembo/applications")
    Call<ApiResponse<List<IremboApplication>>> getRecentIremboApplications();

    @GET("irembo/applications/{applicationNumber}")
    Call<ApiResponse<IremboApplication>> getIremboApplicationByNumber(@Path("applicationNumber") String applicationNumber);

    // WhatsApp Groups
    @GET("whatsapp-groups")
    Call<List<WhatsAppGroup>> getWhatsAppGroups();

    // Leaderboard
    @GET("leaderboard")
    Call<ApiResponse<List<LeaderboardEntry>>> getLeaderboard(
            @Query("limit") int limit
    );
}
