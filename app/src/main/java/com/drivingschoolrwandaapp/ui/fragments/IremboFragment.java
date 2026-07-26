package com.drivingschoolrwandaapp.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.models.IremboService;
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest;
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest;
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.activities.ApplicationDetailsActivity;
import com.drivingschoolrwandaapp.utils.PhoneUtils;
import com.drivingschoolrwandaapp.utils.SafetyUtils;
import com.drivingschoolrwandaapp.ui.adapters.IremboServiceAdapter;
import com.drivingschoolrwandaapp.ui.adapters.RecentActivityAdapter;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class IremboFragment extends Fragment implements IremboServiceAdapter.OnItemClickListener, RecentActivityAdapter.OnItemClickListener {

    private IremboViewModel iremboViewModel;
    private AlertDialog loadingDialog;
    private RecentActivityAdapter recentActivityAdapter;
    private JSONObject locationData;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public IremboFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_irembo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        setupObservers();

        setupToolbar(view);
        setupRecentActivity(view);
        setupBrowseServices(view);
        loadLocationData();

        iremboViewModel.fetchRecentApplications();
    }

    private void executeSafely(Runnable runnable) {
        try {
            if (!executor.isShutdown() && !executor.isTerminated()) {
                executor.execute(runnable);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            Log.w("IremboFragment", "Task rejected, executor is shutting down", e);
        }
    }

    private void loadLocationData() {
        Context context = getContext();
        if (context == null) return;
        executeSafely(() -> {
            try {
                InputStream is = context.getAssets().open("location.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);
                JSONObject result = new JSONObject(json);
                mainHandler.post(() -> locationData = result);
            } catch (IOException | JSONException e) {
                Log.e("IremboFragment", "Error loading location data", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                mainHandler.post(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.error_loading_location), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                SafetyUtils.runIfActivityAttached(this, "setupToolbar",
                        () -> {
                            if (getActivity() != null) {
                                getActivity().getOnBackPressedDispatcher().onBackPressed();
                            }
                        }));
    }

    private void setupRecentActivity(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_recent_activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentActivityAdapter = new RecentActivityAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(recentActivityAdapter);

        view.findViewById(R.id.tv_view_all).setOnClickListener(v -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.all), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBrowseServices(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_browse_services);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<IremboService> services = new ArrayList<>();
        services.add(new IremboService(getString(R.string.provisional_license), R.drawable.ic_car_side));
        services.add(new IremboService(getString((R.string.speacial_request)), R.drawable.ic_edit_document));

        IremboServiceAdapter adapter = new IremboServiceAdapter(services, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(IremboService service) {
        // Compare against the actual strings used when creating the services in setupBrowseServices()
        String name = service.getName();
        if (name.equals(getString(R.string.provisional_license))) {
            showIremboLicenseDialog();
        } else if (name.equals(getString(R.string.speacial_request))) {
            showIremboSpecialDialog();
        }
    }

    @Override
    public void onItemClick(IremboApplication application) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), ApplicationDetailsActivity.class);
            intent.putExtra("application_details", application);
            startActivity(intent);
        }
    }

    private void setupObservers() {
        iremboViewModel.getRecentApplications().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    recentActivityAdapter.setApplications(resource.data);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        iremboViewModel.getLicenseRequestStatus().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else {
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.license_submitted), Toast.LENGTH_SHORT).show();
                    iremboViewModel.fetchRecentApplications();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + resource.message, Toast.LENGTH_LONG).show();
                }
            }
        });

        iremboViewModel.getSpecialRequestStatus().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else {
                    if (getContext() != null) Toast.makeText(getContext(), getString(R.string.special_submitted), Toast.LENGTH_SHORT).show();
                    iremboViewModel.fetchRecentApplications();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + resource.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null && getContext() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(getContext()));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        if (loadingDialog != null) {
            loadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showIremboLicenseDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_irembo_license, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etName = dialogView.findViewById(R.id.et_applicant_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_applicant_phone);
        TextInputEditText etNationalId = dialogView.findViewById(R.id.et_applicant_national_id);
        
        Spinner spinnerProvince = dialogView.findViewById(R.id.spinner_province);
        Spinner spinnerDistrict = dialogView.findViewById(R.id.spinner_district);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        RadioGroup rgLicenseType = dialogView.findViewById(R.id.rg_license_type);
         Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Setup Location Spinners
        if (locationData != null) {
            List<String> provinces = new ArrayList<>();
            Iterator<String> keys = locationData.keys();
            while (keys.hasNext()) {
                provinces.add(keys.next());
            }
            Collections.sort(provinces);
            
            ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, provinces);
            provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProvince.setAdapter(provinceAdapter);

            spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedProvince = provinces.get(position);
                    List<String> districts = new ArrayList<>();
                    try {
                        JSONObject districtsObj = locationData.getJSONObject(selectedProvince);
                        Iterator<String> districtKeys = districtsObj.keys();
                        while (districtKeys.hasNext()) {
                            districts.add(districtKeys.next());
                        }
                        Collections.sort(districts);
                    } catch (JSONException e) {
                        Log.e("IremboFragment", "Error loading districts for province", e);
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    }

                    ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, districts);
                    districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDistrict.setAdapter(districtAdapter);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Setup Spinner
        String[] categories = {"A", "B", "C", "D"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String rawPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String nationalId = etNationalId.getText() != null ? etNationalId.getText().toString().trim() : "";
            
            String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
            String district = spinnerDistrict.getSelectedItem() != null ? spinnerDistrict.getSelectedItem().toString() : "";
            String address = province + ", " + district;
            
            Object selectedCategory = spinnerCategory.getSelectedItem();
            String category = selectedCategory != null ? selectedCategory.toString() : "";

            int selectedLicenseTypeId = rgLicenseType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
            if (TextUtils.isEmpty(rawPhone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
            String phoneError = PhoneUtils.getValidationError(rawPhone);
            if (phoneError != null) { etPhone.setError(getString(R.string.invalid_phone)); return; }
            String phone = PhoneUtils.normalize(rawPhone);
            if (TextUtils.isEmpty(nationalId)) { etNationalId.setError(getString(R.string.error_required_field)); return; }
            if (nationalId.length() != 16) { etNationalId.setError("Must be 16 digits"); return; }
            if (TextUtils.isEmpty(province) || TextUtils.isEmpty(district)) { if (getContext() != null) Toast.makeText(getContext(), getString(R.string.please_select_location), Toast.LENGTH_SHORT).show(); return; }
            if (TextUtils.isEmpty(category)) { if (getContext() != null) Toast.makeText(getContext(), getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
            if (selectedLicenseTypeId == -1) { if (getContext() != null) Toast.makeText(getContext(), getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }

            RadioButton rbLicenseType = dialogView.findViewById(selectedLicenseTypeId);
            if (rbLicenseType == null) {
                if (getContext() != null) Toast.makeText(getContext(), getString(R.string.error_required_field), Toast.LENGTH_SHORT).show();
                return;
            }

            String licenseType = rbLicenseType.getText() != null ? rbLicenseType.getText().toString().toUpperCase(Locale.ROOT) : "NEW";
            // Default to new application type (removed the non-existent RadioGroup for app type)
            String appType = "New";

            IremboLicenseRequest request = new IremboLicenseRequest(
                    category, licenseType, appType, name, phone, nationalId, address
            );

            iremboViewModel.submitLicenseRequest(request);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showIremboSpecialDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_irembo_special, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }


        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        TextInputEditText etName = dialogView.findViewById(R.id.et_applicant_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_applicant_phone);
        TextInputEditText etNationalId = dialogView.findViewById(R.id.et_national_id);

        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Setup Spinner
        String[] categories = {"A", "B", "C", "D"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String serviceName =  "";
            Object selectedCategory = spinnerCategory.getSelectedItem();
            String category = selectedCategory != null ? selectedCategory.toString() : "";
            
            CharSequence nameText = etName.getText();
            String name = nameText != null ? nameText.toString().trim() : "";
            CharSequence phoneText = etPhone.getText();
            String rawPhone = phoneText != null ? phoneText.toString().trim() : "";
            CharSequence nationalIdText = etNationalId.getText();
            String nationalId = nationalIdText != null ? nationalIdText.toString().trim() : "";
            String description = "";

            if (TextUtils.isEmpty(category)) { if (getContext() != null) Toast.makeText(getContext(), getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
            if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
            if (TextUtils.isEmpty(rawPhone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
            String phoneError = PhoneUtils.getValidationError(rawPhone);
            if (phoneError != null) { etPhone.setError(getString(R.string.invalid_phone)); return; }
            String phone = PhoneUtils.normalize(rawPhone);
            if (TextUtils.isEmpty(nationalId)) { etNationalId.setError(getString(R.string.error_required_field)); return; }
            if (nationalId.length() != 16) { etNationalId.setError("Must be 16 digits"); return; }


            IremboSpecialRequest request = new IremboSpecialRequest(
                    serviceName, category, name, phone, nationalId, description
            );
            iremboViewModel.submitSpecialRequest(request);
            dialog.dismiss();
        });

        dialog.show();
    }
    
    private void showPaymentConfirmationDialog(IremboPaymentResponse paymentDetails) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_payment_confirmation, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        
        TextView btnPayMtn = dialogView.findViewById(R.id.payment_method_1);
        TextView btnPayAirtel = dialogView.findViewById(R.id.payment_method_2);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        String currency = paymentDetails.getCurrency() != null ? paymentDetails.getCurrency() : "RWF";
        
        tvAmount.setText(getString(R.string.amount_with_currency_format, format.format(paymentDetails.getAmount()), currency));
        
        String reference = paymentDetails.getReference();

        btnPayMtn.setOnClickListener(v -> {
             String ussdCode = "*182*8*1*847318*" +  paymentDetails.getAmount() + "#";

             Intent intent = new Intent(Intent.ACTION_DIAL);
             intent.setData(Uri.parse("tel:" + Uri.encode(ussdCode)));
             try {
                 startActivity(intent);
             } catch (Exception e) {
                 Log.e("IremboFragment", "Could not open dialer for MTN payment", e);
                 com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                 if (getContext() != null) Toast.makeText(getContext(), getString(R.string.could_not_open_dialer), Toast.LENGTH_SHORT).show();
             }
             dialog.dismiss();
             iremboViewModel.fetchRecentApplications();
        });

        btnPayAirtel.setOnClickListener(v -> {
             String ussdCode = "*182*1*1*0782877442*"+  paymentDetails.getAmount() + "#";
             Intent intent = new Intent(Intent.ACTION_DIAL);
             intent.setData(Uri.parse("tel:" + Uri.encode(ussdCode)));
             try {
                 startActivity(intent);
             } catch (Exception e) {
                 Log.e("IremboFragment", "Could not open dialer for Airtel payment", e);
                 com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                 if (getContext() != null) Toast.makeText(getContext(), getString(R.string.could_not_open_dialer), Toast.LENGTH_SHORT).show();
             }
             dialog.dismiss();
             iremboViewModel.fetchRecentApplications();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
