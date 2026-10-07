"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { useRouter } from "next/navigation";

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName?: string;
  role: {
    id: number;
    roleName: string;
  };
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const storedToken = localStorage.getItem("admin_token");
    const storedUser = localStorage.getItem("admin_user");

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    let response: Response;
    try {
      response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({identifier:  email, password }),
      });
    } catch {
      // fetch itself failed (offline, DNS, server down) — show that instead
      // of a confusing generic error.
      throw new Error("Cannot reach the server. Check your internet connection and try again.");
    }

    if (!response.ok) {
      // The backend reports the specific reason (unknown number, wrong
      // password, inactive account, device mismatch, …) in `message`. Older
      // builds used `error`; read both and never parse-throw on non-JSON.
      let payload: { message?: string; error?: string } | null = null;
      try {
        payload = await response.json();
      } catch {
        payload = null;
      }
      throw new Error(
        payload?.message || payload?.error || `Login failed (server error ${response.status})`
      );
    }

    const data = await response.json();

    localStorage.setItem("admin_token", data.accessToken);
    localStorage.setItem("admin_user", JSON.stringify(data.user));
    localStorage.setItem("admin_refresh_token", data.refreshToken);

    setToken(data.accessToken);
    setUser(data.user);

    router.push("/admin/dashboard");
  };

  const logout = () => {
    localStorage.removeItem("admin_token");
    localStorage.removeItem("admin_user");
    // Also drop the refresh token: leaving it behind lets a stray 401 after
    // logout mint a fresh access token for the PREVIOUS user (apiClient
    // refreshes from localStorage), so the next account could see the old
    // account's data. This is a cross-account data-leak bug, not a nicety.
    localStorage.removeItem("admin_refresh_token");
    setToken(null);
    setUser(null);
    router.push("/admin/login");
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
