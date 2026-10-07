// Simple in-memory failure counter for the password-reset code flow.
//
// The reset token is a 6-digit numeric code, so without limiting attempts an
// attacker could brute-force it (10^6 combinations, no schema change needed).
// This module counts failed verify/reset attempts per code-holder for a
// bounded window; once a holder exceeds MAX_FAILURES the caller deletes the
// pending request, forcing a brand-new code to be requested.
//
// Note: this is intentionally process-local (no DB column). It resets on
// server restart, which is acceptable as a first line of defense for a small
// admin console; a persistent limiter would be the next step.

const MAX_FAILURES = 5;
const WINDOW_MS = 15 * 60 * 1000;

interface FailureRecord {
  count: number;
  firstFailureAt: number;
}

const failures = new Map<string, FailureRecord>();

function pruneWindow(now: number) {
  for (const [key, rec] of failures) {
    if (now - rec.firstFailureAt > WINDOW_MS) failures.delete(key);
  }
}

export function recordResetFailure(key: string): number {
  const now = Date.now();
  pruneWindow(now);
  const rec = failures.get(key);
  if (!rec) {
    failures.set(key, { count: 1, firstFailureAt: now });
    return 1;
  }
  rec.count += 1;
  return rec.count;
}

export function resetFailuresExceeded(key: string): boolean {
  const now = Date.now();
  pruneWindow(now);
  const rec = failures.get(key);
  return !!rec && rec.count >= MAX_FAILURES;
}

export function clearResetFailures(key: string): void {
  failures.delete(key);
}
