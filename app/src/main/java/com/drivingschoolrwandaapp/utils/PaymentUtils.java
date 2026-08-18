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

    public static void setupPaymentMethods(View rootView, Fragment fragment, String amount) {
        MaterialCardView paymentMethod1 = rootView.findViewById(R.id.payment_method_1);
        MaterialCardView paymentMethod2 = rootView.findViewById(R.id.payment_method_2);
        MaterialCardView paymentMethod3 = rootView.findViewById(R.id.payment_method_3);

        String momoPayUssd = "*182*8*1*644209*" + amount + "#";
        String numberUssd = "*182*1*1*0785460748*" + amount + "#";
        String airtelUssd = "*182*1*1*0726656615*" + amount + "#";

        if(paymentMethod1 != null) paymentMethod1.setOnClickListener(v -> dialUssd(fragment, momoPayUssd));
        if(paymentMethod2 != null) paymentMethod2.setOnClickListener(v -> dialUssd(fragment, numberUssd));
        if (paymentMethod3 != null) paymentMethod3.setOnClickListener(v -> dialUssd(fragment, airtelUssd));
    }

    public static void setupPaymentMethods(View rootView, Activity activity, String amount) {
        MaterialCardView paymentMethod1 = rootView.findViewById(R.id.payment_method_1);
        MaterialCardView paymentMethod2 = rootView.findViewById(R.id.payment_method_2);
        MaterialCardView paymentMethod3 = rootView.findViewById(R.id.payment_method_3);

        String momoPayUssd = "*182*8*1*644209*" + amount + "#";
        String numberUssd = "*182*1*1*0785460748*" + amount + "#";
        String airtelUssd = "*182*1*1*0726656615*" + amount + "#";

        if(paymentMethod1 != null) paymentMethod1.setOnClickListener(v -> dialUssd(activity, momoPayUssd));
        if(paymentMethod2 != null) paymentMethod2.setOnClickListener(v -> dialUssd(activity, numberUssd));
        if (paymentMethod3 != null) paymentMethod3.setOnClickListener(v -> dialUssd(activity, airtelUssd));
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
