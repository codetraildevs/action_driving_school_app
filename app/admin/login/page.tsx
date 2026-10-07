"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth/auth-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Eye, EyeOff, Loader2, ArrowLeft } from "lucide-react";

type Step = "login" | "identify" | "code" | "newPassword";

const COOLDOWN_SECONDS = 60;

export default function AdminLoginPage() {
  const [step, setStep] = useState<Step>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [viewPassword, setViewPassword] = useState(false);
  const [viewNewPassword, setViewNewPassword] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const { login } = useAuth();

  // Forgot-password flow state
  const [identifier, setIdentifier] = useState("");
  const [resetRequestId, setResetRequestId] = useState<number | null>(null);
  const [maskedEmail, setMaskedEmail] = useState<string | null>(null);
  const [maskedPhone, setMaskedPhone] = useState<string | null>(null);
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [cooldown, setCooldown] = useState(0);

  // Clean up a stale login state from a PREVIOUS account: leftover
  // admin_token / admin_user / admin_refresh_token entries in localStorage
  // would otherwise make a 401 after navigation appear authenticated and
  // could show the last user's data on the next console load.
  useEffect(() => {
    const t = localStorage.getItem("admin_token");
    const r = localStorage.getItem("admin_refresh_token");
    const u = localStorage.getItem("admin_user");
    if (t || r || u) {
      localStorage.removeItem("admin_token");
      localStorage.removeItem("admin_user");
      localStorage.removeItem("admin_refresh_token");
    }
  }, []);

  // Ticking countdown for the resend-cooldown.
  useEffect(() => {
    if (cooldown <= 0) return;
    const iv = setInterval(() => setCooldown((c) => (c > 0 ? c - 1 : 0)), 1000);
    return () => clearInterval(iv);
  }, [cooldown]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);
    try {
      await login(email, password);
    } catch (err) {
      setError((err as Error).message || "Login failed");
    } finally {
      setIsLoading(false);
    }
  };

  // ── Step 1: submit the identifying contact (email or phone) ──────────
  const handleForgotSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);
    try {
      const res = await fetch("/api/auth/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ identifier: identifier.trim() }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || data.error || "Could not start reset");
      }
      setMaskedEmail(data.maskedEmail ?? null);
      setMaskedPhone(data.maskedPhone ?? null);
      setResetRequestId(data.resetRequestId ?? null);
      setCooldown(COOLDOWN_SECONDS);
      setStep("code");
    } catch (err) {
      setError((err as Error).message || "Could not start reset");
    } finally {
      setIsLoading(false);
    }
  };

  // Resend keeps the same identify-step payload (inside the server's own
  // 60s cooldown window the server re-sends the still-valid code).
  const resendCode = () => {
    handleForgotSubmit({ preventDefault() {} } as React.FormEvent);
  };

  // ── Step 2: verify the 6-digit code ───────────────────────────────────
  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);
    try {
      const res = await fetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          identifier: resetRequestId ? `${resetRequestId}` : identifier,
          otp,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || data.error || "Verification failed");
      }
      if (!data.verified) {
        throw new Error(data.message || "Wrong code");
      }
      setStep("newPassword");
    } catch (err) {
      setError((err as Error).message || "Verification failed");
    } finally {
      setIsLoading(false);
    }
  };

  // ── Step 3: set the new password ──────────────────────────────────────
  const handleNewPasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    setIsLoading(true);
    try {
      const res = await fetch("/api/auth/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token: otp,
          resetRequestId: resetRequestId ?? undefined,
          newPassword,
          confirmPassword,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || data.error || "Reset failed");
      }
      // Success: clear the flow state and send the admin to the login card
      // with a confirmation banner.
      resetFlow();
      setSuccessMessage("Password reset. Please log in with your new password.");
      setStep("login");
    } catch (err) {
      setError((err as Error).message || "Reset failed");
    } finally {
      setIsLoading(false);
    }
  };

  const resetFlow = () => {
    setIdentifier("");
    setResetRequestId(null);
    setOtp("");
    setNewPassword("");
    setConfirmPassword("");
    setMaskedEmail(null);
    setMaskedPhone(null);
    setCooldown(0);
    setError("");
  };

  const goBackToLogin = () => {
    resetFlow();
    setStep("login");
  };

  // verify-otp and reset-password are keyed on the opaque resetRequestId the
  // server returned in step 1, never on the (masked) email shown to the user.

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-neutral-50 to-neutral-100 dark:from-neutral-950 dark:to-neutral-900 p-4">
      <Card className="w-full max-w-md">
        {step === "login" && (
          <>
            <CardHeader className="space-y-1">
              <CardTitle className="text-2xl font-bold">Admin Login</CardTitle>
              <CardDescription>
                Enter your credentials to access the admin dashboard
              </CardDescription>
            </CardHeader>
            <CardContent>
              {successMessage && (
                <div className="mb-4 text-sm text-green-700 bg-green-50 dark:bg-green-950/30 p-3 rounded-md">
                  {successMessage}
                  <button
                    type="button"
                    onClick={() => setSuccessMessage("")}
                    className="float-right font-bold text-green-700 hover:text-green-900"
                    aria-label="Dismiss"
                  >
                    ×
                  </button>
                </div>
              )}
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="email">Phone Number</Label>
                  <Input
                    id="email"
                    placeholder="0780000000"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    disabled={isLoading}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <div className="relative">
                    <Input
                      id="password"
                      type={viewPassword ? "text" : "password"}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      disabled={isLoading}
                      className="pr-10"
                    />
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setViewPassword(!viewPassword)}
                      type="button"
                      className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                      disabled={isLoading}
                    >
                      {viewPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                </div>
                {error && (
                  <div className="text-sm text-red-500 bg-red-50 dark:bg-red-950/20 p-3 rounded-md">
                    {error}
                  </div>
                )}
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Signing in...
                    </>
                  ) : (
                    "Sign in"
                  )}
                </Button>
                <div className="text-center">
                  <button
                    type="button"
                    onClick={() => {
                      resetFlow();
                      setStep("identify");
                    }}
                    className="text-sm text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-200 underline"
                  >
                    Forgot password?
                  </button>
                </div>
              </form>
            </CardContent>
          </>
        )}

        {step !== "login" && (
          <>
            <CardHeader className="space-y-1">
              <CardTitle className="text-2xl font-bold flex items-center gap-2">
                <button
                  type="button"
                  onClick={goBackToLogin}
                  className="p-1 -ml-1 rounded hover:bg-neutral-100 dark:hover:bg-neutral-800"
                  aria-label="Back to login"
                >
                  <ArrowLeft className="h-5 w-5" />
                </button>
                Reset password
              </CardTitle>
              <CardDescription>
                {step === "identify" &&
                  "Enter your account email or phone number to receive a 6-digit reset code."}
                {step === "code" && maskedEmail
                  ? `We sent a 6-digit code to ${maskedEmail}${maskedPhone ? ` for ${maskedPhone}` : ""}.`
                  : "Enter the 6-digit code from your reset request."}
                {step === "newPassword" &&
                  "Choose a new password for your account."}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {step === "identify" && (
                <form onSubmit={handleForgotSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="identifier">Email or phone number</Label>
                    <Input
                      id="identifier"
                      placeholder="you@example.com or 07xxxxxxxx"
                      aria-label="Email or phone number"
                      value={identifier}
                      onChange={(e) => setIdentifier(e.target.value)}
                      required
                      disabled={isLoading}
                      autoComplete="username"
                    />
                  </div>
                  {error && (
                    <div className="text-sm text-red-500 bg-red-50 dark:bg-red-950/20 p-3 rounded-md">
                      {error}
                    </div>
                  )}
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Sending code...
                      </>
                    ) : (
                      "Send reset code"
                    )}
                  </Button>
                </form>
              )}

              {step === "code" && (
                <form onSubmit={handleOtpSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="otp">6-digit code</Label>
                    <Input
                      id="otp"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      placeholder="______"
                      value={otp}
                      onChange={(e) =>
                        setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
                      }
                      autoComplete="one-time-code"
                      required
                      disabled={isLoading}
                      className="text-center text-2xl tracking-[0.4em]"
                    />
                  </div>
                  {error && (
                    <div className="text-sm text-red-500 bg-red-50 dark:bg-red-950/20 p-3 rounded-md">
                      {error}
                    </div>
                  )}
                  <div className="text-center text-sm text-neutral-500 dark:text-neutral-400">
                    Didn't get a code?{" "}
                    <Button
                      type="button"
                      variant="link"
                      onClick={resendCode}
                      disabled={isLoading || cooldown > 0}
                      className="h-auto p-0 align-baseline"
                    >
                      {cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}
                    </Button>
                  </div>
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Verifying...
                      </>
                    ) : (
                      "Verify code"
                    )}
                  </Button>
                </form>
              )}

              {step === "newPassword" && (
                <form onSubmit={handleNewPasswordSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="newPassword">New password</Label>
                    <div className="relative">
                      <Input
                        id="newPassword"
                        type={viewNewPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        required
                        disabled={isLoading}
                        className="pr-10"
                      />
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setViewNewPassword(!viewNewPassword)}
                        type="button"
                        className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                        disabled={isLoading}
                      >
                        {viewNewPassword ? (
                          <EyeOff className="h-4 w-4" />
                        ) : (
                          <Eye className="h-4 w-4" />
                        )}
                      </Button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirmPassword">Confirm password</Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      disabled={isLoading}
                    />
                  </div>
                  {error && (
                    <div className="text-sm text-red-500 bg-red-50 dark:bg-red-950/20 p-3 rounded-md">
                      {error}
                    </div>
                  )}
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Resetting...
                      </>
                    ) : (
                      "Reset password"
                    )}
                  </Button>
                </form>
              )}
            </CardContent>
          </>
        )}
      </Card>
    </div>
  );
}
