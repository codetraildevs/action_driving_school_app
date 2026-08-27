export class ApiClient {
  private baseUrl: string;
  private refreshPromise: Promise<string> | null = null;

  constructor(baseUrl: string = "") {
    this.baseUrl = baseUrl;
  }

  private getAuthHeaders(): Record<string, string> {
    const token = localStorage.getItem("admin_token");
    const headers: Record<string, string> = {};
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    
    return headers;
  }

  private async refreshTokens(): Promise<{ accessToken: string; refreshToken: string }> {
    const refreshToken = localStorage.getItem("admin_refresh_token");
    
    if (!refreshToken) {
      throw new Error("No refresh token available");
    }

    const response = await fetch(`${this.baseUrl}/api/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      // Clear tokens if refresh fails
      localStorage.removeItem("admin_token");
      localStorage.removeItem("admin_refresh_token");
      const error = await response.json();
      throw new Error(error.message || "Token refresh failed");
    }

    const data = await response.json();
    return data;
  }

  private async handleTokenRefresh(): Promise<string> {
    // If a refresh is already in progress, wait for it
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = new Promise(async (resolve, reject) => {
      try {
        const tokens = await this.refreshTokens();
        
        // Store new tokens
        localStorage.setItem("admin_token", tokens.accessToken);
        localStorage.setItem("admin_refresh_token", tokens.refreshToken);
        
        resolve(tokens.accessToken);
      } catch (error) {
        reject(error);
      } finally {
        this.refreshPromise = null;
      }
    });

    return this.refreshPromise;
  }

  private async fetchWithAuthRetry(
    endpoint: string,
    options: RequestInit & { retryCount?: number } = {}
  ): Promise<Response> {
    const { retryCount = 0, ...fetchOptions } = options;
    
    // Get base auth headers
    const authHeaders = this.getAuthHeaders();
    
    // Prepare headers for the request
    const isFormData = fetchOptions.body instanceof FormData;
    let requestHeaders: Record<string, string> = { ...authHeaders };
    
    // Only add Content-Type if not FormData and not already provided
    if (!isFormData && !fetchOptions.headers?.['Content-Type']) {
      requestHeaders['Content-Type'] = 'application/json';
    }
    
    // Merge with any existing headers
    if (fetchOptions.headers) {
      requestHeaders = {
        ...requestHeaders,
        ...(fetchOptions.headers as Record<string, string>),
      };
    }
    
    let response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...fetchOptions,
      headers: requestHeaders,
    });

    // If token is expired, try to refresh and retry
    if (response.status === 401 && retryCount === 0) {
      try {
        const newAccessToken = await this.handleTokenRefresh();
        
        // Update authorization header with new token
        requestHeaders['Authorization'] = `Bearer ${newAccessToken}`;
        
        // Retry the request with new token
        response = await fetch(`${this.baseUrl}${endpoint}`, {
          ...fetchOptions,
          headers: requestHeaders,
        });
      } catch (refreshError) {
        // Redirect to login or handle refresh failure
        window.location.href = "/admin/login";
        throw refreshError;
      }
    }

    return response;
  }

  async get<T>(endpoint: string): Promise<T> {
    const response = await this.fetchWithAuthRetry(endpoint, {
      method: "GET",
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || "Request failed");
    }

    return response.json();
  }

  async post<T>(endpoint: string, data: any, customHeaders?: Record<string, string>): Promise<T> {
    const response = await this.fetchWithAuthRetry(endpoint, {
      method: "POST",
      body: data instanceof FormData ? data : JSON.stringify(data),
      headers: customHeaders,
    });

    

    return response.json();
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    const response = await this.fetchWithAuthRetry(endpoint, {
      method: "PUT",
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || "Request failed");
    }

    return response.json();
  }

  async patch<T>(endpoint: string, data?: any): Promise<T> {
    const response = await this.fetchWithAuthRetry(endpoint, {
      method: "PATCH",
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || "Request failed");
    }

    return response.json();
  }

  async delete<T>(endpoint: string, data?: any): Promise<T> {
    const response = await this.fetchWithAuthRetry(endpoint, {
      method: "DELETE",
      body: data ? JSON.stringify(data) : undefined,
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || "Request failed");
    }

    return response.json();
  }

  // Special method for file uploads
  async uploadFile<T>(endpoint: string, formData: FormData): Promise<T> {
    return this.post<T>(endpoint, formData);
  }

  // Method to manually refresh tokens if needed
  async refreshTokensManually(): Promise<{ accessToken: string; refreshToken: string }> {
    return this.refreshTokens();
  }
}

export const apiClient = new ApiClient();