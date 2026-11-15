package com.drivingschoolrwandaapp.api.interceptors;

final class AuthEndpoints {
    private AuthEndpoints() { }

    static boolean isPublicAuth(String path) {
        return path.endsWith("/auth/login") || path.endsWith("/auth/register")
                || path.endsWith("/auth/refresh");
    }
}
