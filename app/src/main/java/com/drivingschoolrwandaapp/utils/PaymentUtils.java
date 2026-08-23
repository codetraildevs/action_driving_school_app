package com.drivingschoolrwandaapp.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.drivingschoolrwandaapp.R;
import com.google.android.material.card.MaterialCardView;

public class PaymentUtils {

    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 456;
    private static String ussdToCall;

    /**
     * Caps the dialog window height to a fraction of the screen so long payment
     * content (instructions + methods) stays reachable on small devices.
     * The dialog layout uses a weighted ScrollView, so when the window is
     * clamped the body scrolls instead of being cut off.
     *
     * @param dialog           the dialog whose window should be capped (call after {@code show()})
     * @param maxScreenFraction maximum window height as a fraction of the screen height (e.g. 0.8)
     */
    public static void capDialogHeight(Dialog dialog, float maxScreenFraction) {
        if (dialog == null || dialog.getWindow() == null) return;
        Window window = dialog.getWindow();
        DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
        int maxHeight = (int) (dm.heightPixels * maxScreenFraction);
        View decor = window.getDecorView();
        decor.post(() -> {
            if (window.isActive() && decor.getHeight() > maxHeight) {
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.height = maxHeight;
                window.setAttributes(lp);
            }
        });
    }

    /** Default payment numbers (used by Irembo services). */
    private static final String DEFAULT_MOMO_PAY_CODE = "644209";
    private static final String DEFAULT_MTN_NUMBER = "0785460748";
    private static final String DEFAULT_TIGO_NUMBER = "0726656615";

    /** Exam list payment numbers. */
    private static final String EXAM_MOMO_PAY_CODE = "847318";
    private static final String EXAM_MTN_NUMBER = "0782877442";
    private static final String EXAM_TIGO_NUMBER = "0722877442";

    public static void setupPaymentMethods(View rootView, Fragment fragment, String amount) {
        setupPaymentMethods(rootView, fragment, null, amount, DEFAULT_MOMO_PAY_CODE, DEFAULT_MTN_NUMBER, DEFAULT_TIGO_NUMBER);
    }

    public static void setupPaymentMethods(View rootView, Activity activity, String amount) {
        setupPaymentMethods(rootView, null, activity, amount, DEFAULT_MOMO_PAY_CODE, DEFAULT_MTN_NUMBER, DEFAULT_TIGO_NUMBER);
    }

    /** Exam-list payment with custom numbers. */
    public static void setupExamPaymentMethods(View rootView, Fragment fragment, String amount) {
        setupPaymentMethods(rootView, fragment, null, amount, EXAM_MOMO_PAY_CODE, EXAM_MTN_NUMBER, EXAM_TIGO_NUMBER);
    }

    private static void setupPaymentMethods(View rootView, Fragment fragment, Activity activity, String amount,
            String momoPayCode, String mtnNumber, String tigoNumber) {
        MaterialCardView paymentMethod1 = rootView.findViewById(R.id.payment_method_1);
        MaterialCardView paymentMethod2 = rootView.findViewById(R.id.payment_method_2);
        MaterialCardView paymentMethod3 = rootView.findViewById(R.id.payment_method_3);

        String momoPayUssd = "*182*8*1*" + momoPayCode + "*" + amount + "#";
        String mtnUssd = "*182*1*1*" + mtnNumber + "*" + amount + "#";
        String tigoUssd = "*182*1*1*" + tigoNumber + "*" + amount + "#";

        if (paymentMethod1 != null) paymentMethod1.setOnClickListener(v -> {
            if (fragment != null) dialUssd(fragment, momoPayUssd);
            else if (activity != null) dialUssd(activity, momoPayUssd);
        });
        if (paymentMethod2 != null) paymentMethod2.setOnClickListener(v -> {
            if (fragment != null) dialUssd(fragment, mtnUssd);
            else if (activity != null) dialUssd(activity, mtnUssd);
        });
        if (paymentMethod3 != null) paymentMethod3.setOnClickListener(v -> {
            if (fragment != null) dialUssd(fragment, tigoUssd);
            else if (activity != null) dialUssd(activity, tigoUssd);
        });
    }

    private static void dialUssd(Fragment fragment, String ussd) {
        ussdToCall = ussd;
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_REQUEST_CODE);
        } else {
            fragment.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(ussd))));
        }
    }

    private static void dialUssd(Activity activity, String ussd) {
        ussdToCall = ussd;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_REQUEST_CODE);
        } else {
            activity.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(ussd))));
        }
    }

    public static void onRequestPermissionsResult(Fragment fragment, int requestCode, @NonNull int[] grantResults) {
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ussdToCall != null && !ussdToCall.isEmpty()) {
                    dialUssd(fragment, ussdToCall);
                }
            } else {
                Toast.makeText(fragment.getContext(), fragment.getString(R.string.permission_denied_call), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ussdToCall != null && !ussdToCall.isEmpty()) {
                    dialUssd(activity, ussdToCall);
                }
            } else {
                Toast.makeText(activity, activity.getString(R.string.permission_denied_call), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
