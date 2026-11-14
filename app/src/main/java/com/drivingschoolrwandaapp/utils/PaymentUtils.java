package com.drivingschoolrwandaapp.utils;

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
import androidx.fragment.app.Fragment;

import com.drivingschoolrwandaapp.R;
import com.google.android.material.card.MaterialCardView;

import android.widget.TextView;

public class PaymentUtils {

    /** Caps the dialog window height so long payment instructions stay reachable. */
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

    // Kept for backward compatibility with callbacks; no longer requests CALL_PHONE.
    @SuppressWarnings("UnusedAssignment")
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 456;

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
        updateIremboPaymentText(rootView);
    }

    public static void setupPaymentMethods(View rootView, Activity activity, String amount) {
        setupPaymentMethods(rootView, null, activity, amount, DEFAULT_MOMO_PAY_CODE, DEFAULT_MTN_NUMBER, DEFAULT_TIGO_NUMBER);
        updateIremboPaymentText(rootView);
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

    /**
     * Updates the payment instruction text to show Irembo-specific numbers.
     * Called only for Irembo services (not exam payments).
     */
    private static void updateIremboPaymentText(View rootView) {
        TextView tvInstruction1 = rootView.findViewById(R.id.tv_payment_instruction_1);
        TextView tvInstruction2 = rootView.findViewById(R.id.tv_payment_instruction_2);
        TextView tvInstruction3 = rootView.findViewById(R.id.tv_payment_instruction_3);

        if (tvInstruction1 != null) {
            tvInstruction1.setText(DEFAULT_MOMO_PAY_CODE + " (registered on Louise)");
        }
        if (tvInstruction2 != null) {
            tvInstruction2.setText(DEFAULT_MTN_NUMBER);
        }
        if (tvInstruction3 != null) {
            tvInstruction3.setText(DEFAULT_TIGO_NUMBER);
        }
    }

    private static void dialUssd(Fragment fragment, String ussd) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(ussd)));
        fragment.startActivity(intent);
    }

    private static void dialUssd(Activity activity, String ussd) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(ussd)));
        activity.startActivity(intent);
    }

    public static void onRequestPermissionsResult(Fragment fragment, int requestCode, @NonNull int[] grantResults) {
        onRequestPermissionsResultForUssd(fragment, requestCode, grantResults);
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        onRequestPermissionsResultForUssd(activity, requestCode, grantResults);
    }

    // CALL_PHONE permission is no longer needed by this app.
    // If a caller still delivers legacy request results here, ignore them gracefully.

    private static String ussdToCall;


    private static void onRequestPermissionsResultForUssd(Activity activity, int requestCode, @NonNull int[] grantResults) {
    }

    private static void onRequestPermissionsResultForUssd(Fragment fragment, int requestCode, @NonNull int[] grantResults) {
    }
}
