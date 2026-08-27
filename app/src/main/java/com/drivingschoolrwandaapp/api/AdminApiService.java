package com.drivingschoolrwandaapp.api;

import com.drivingschoolrwandaapp.models.entities.AdminDashboardResponse;
import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.drivingschoolrwandaapp.models.entities.AdminUserDetailResponse;
import com.drivingschoolrwandaapp.models.entities.AdminUsersResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit endpoints for the admin console (backed by the
 * {@code /api/admin/*} routes on the shared backend).
 *
 * <p>Reuses the same OkHttp client as {@link ApiService}, so the
 * {@code Authorization: Bearer <token>} header is attached automatically.
 */
public interface AdminApiService {

    /** Dashboard analytics. Returns {@code { data: AdminDashboardStats }}. */
    @GET("admin/analytics/dashboard")
    Call<AdminDashboardResponse> getDashboardStats();

    /** All users. Returns {@code { success, data: [AdminUser] }}. */
    @GET("admin/users")
    Call<AdminUsersResponse> getUsers();

    /** All Irembo requests (driving license + special). Returns a bare list. */
    @GET("admin/requests")
    Call<List<AdminRequest>> getRequests();

    /**
     * A single user with their current subscription.
     * Returns {@code { success, data: AdminUserDetail }}.
     */
    @GET("admin/users/{id}")
    Call<AdminUserDetailResponse> getUserDetail(@Path("id") int id);

    /** A single user's Irembo requests (driving license + special). Bare list. */
    @GET("admin/users/{id}/requests")
    Call<List<AdminRequest>> getUserRequests(@Path("id") int id);
}
