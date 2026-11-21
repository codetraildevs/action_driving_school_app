package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.ResponseInfo;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

/**
 * Release-safe AdMob telemetry (Firebase Analytics / GA4).
 *
 * <p>AdMob reports requests, matches and impressions on its dashboard, but it
 * cannot say how many of those were requested or rendered by this app. These
 * events record the app-side outcome of the same funnel so the two can be
 * reconciled:
 *
 * <pre>
 *   admob_request       an ad load was started
 *   admob_loaded        a fill was returned (AdMob "matched")
 *   admob_load_failed   a load ended without a fill
 *   admob_impression    an ad was shown / displayed
 *   admob_paid          a paid event (revenue estimate)
 *   admob_show_failed   a cached ad could not be shown
 * </pre>
 *
 * <p>Only fixed enums and numbers are sent; never account data, phone numbers
 * or free text from the ad server. Every call is defensive: analytics must
 * never crash or block an ad request.
 *
 * <p>Debug builds use Google test ad units, so their events are still emitted
 * but must not be mixed with production numbers when filtering.
 */
public final class AdAnalytics {

    private AdAnalytics() {
        // utility class
    }

    /** Ad formats as they appear on the AdMob dashboard. */
    public enum Format {
        BANNER("banner"),
        INTERSTITIAL("interstitial"),
        REWARDED("rewarded"),
        REWARDED_INTERSTITIAL("rewarded_interstitial");

        final String value;

        Format(String value) {
            this.value = value;
        }
    }

    private static final String EVENT_REQUEST = "admob_request";
    private static final String EVENT_LOADED = "admob_loaded";
    private static final String EVENT_LOAD_FAILED = "admob_load_failed";
    private static final String EVENT_IMPRESSION = "admob_impression";
    private static final String EVENT_PAID = "admob_paid";
    private static final String EVENT_SHOW_FAILED = "admob_show_failed";

    private static final String PARAM_FORMAT = "ad_format";
    private static final String PARAM_SOURCE = "ad_source";
    private static final String PARAM_ERROR_CODE = "error_code";
    private static final String PARAM_ERROR_DOMAIN = "error_domain";
    private static final String PARAM_ERROR_CAUSE = "error_cause";
    private static final String PARAM_VALUE_MICROS = "value_micros";
    private static final String PARAM_CURRENCY = "currency";
    private static final String PARAM_PRECISION = "precision";

    public static void request(@Nullable Context context, @NonNull Format format) {
        send(context, EVENT_REQUEST, baseParams(format));
    }

    /** Records a fill together with the mediator that served it. */
    public static void loaded(@Nullable Context context, @NonNull Format format,
                              @Nullable ResponseInfo info) {
        Bundle params = baseParams(format);
        params.putString(PARAM_SOURCE, sourceOf(info));
        send(context, EVENT_LOADED, params);
    }

    public static void loadFailed(@Nullable Context context, @NonNull Format format,
                                  @Nullable AdError error) {
        Bundle params = baseParams(format);
        putError(params, error);
        send(context, EVENT_LOAD_FAILED, params);
    }

    public static void impression(@Nullable Context context, @NonNull Format format) {
        send(context, EVENT_IMPRESSION, baseParams(format));
    }

    public static void paid(@Nullable Context context, @NonNull Format format,
                            @Nullable AdValue value) {
        if (value == null) return;
        Bundle params = baseParams(format);
        params.putLong(PARAM_VALUE_MICROS, value.getValueMicros());
        params.putString(PARAM_CURRENCY, value.getCurrencyCode());
        params.putLong(PARAM_PRECISION, value.getPrecisionType());
        send(context, EVENT_PAID, params);
    }

    public static void showFailed(@Nullable Context context, @NonNull Format format,
                                  @Nullable AdError error) {
        Bundle params = baseParams(format);
        putError(params, error);
        send(context, EVENT_SHOW_FAILED, params);
    }

    private static Bundle baseParams(Format format) {
        Bundle params = new Bundle();
        params.putString(PARAM_FORMAT, format.value);
        return params;
    }

    private static void putError(Bundle params, @Nullable AdError error) {
        if (error == null) return;
        params.putLong(PARAM_ERROR_CODE, error.getCode());
        params.putString(PARAM_ERROR_DOMAIN, clip(error.getDomain()));
        params.putString(PARAM_ERROR_CAUSE, causeOf(error));
    }

    /**
     * Coarse bucket for failure reasons. The SDK reports connectivity problems
     * as code 0 (internal error) with a DNS/timeout message, so the message is
     * inspected locally to separate "network" from a genuine internal error.
     * The message itself is never sent.
     */
    @NonNull
    private static String causeOf(@NonNull AdError error) {
        switch (error.getCode()) {
            case AdRequest.ERROR_CODE_NO_FILL:
                return "no_fill";
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                return "network";
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                return "invalid_request";
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
            default:
                return isConnectivityMessage(error.getMessage()) ? "network" : "internal";
        }
    }

    private static boolean isConnectivityMessage(@Nullable String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("unable to resolve host")
                || lower.contains("no address associated")
                || lower.contains("unknownhost")
                || lower.contains("connecting to ad server")
                || lower.contains("timed out")
                || lower.contains("timeout")
                || lower.contains("failed to connect")
                || lower.contains("network");
    }

    /** Never send a server-provided string longer than Firebase accepts. */
    @Nullable
    private static String sourceOf(@Nullable ResponseInfo info) {
        if (info == null) return null;
        String source = null;
        if (info.getLoadedAdapterResponseInfo() != null) {
            source = info.getLoadedAdapterResponseInfo().getAdSourceName();
        }
        if (source == null || source.trim().isEmpty()) {
            source = info.getMediationAdapterClassName();
        }
        return clip(source);
    }

    @Nullable
    private static String clip(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= 100
                ? trimmed.toLowerCase(Locale.ROOT)
                : trimmed.substring(0, 100).toLowerCase(Locale.ROOT);
    }

    private static void send(@Nullable Context context, String event, Bundle params) {
        if (context == null) return;
        try {
            FirebaseAnalytics analytics =
                    FirebaseAnalytics.getInstance(context.getApplicationContext());
            analytics.logEvent(event, params);
        } catch (Throwable ignored) {
            // Analytics must never affect ad delivery.
        }
    }
}
