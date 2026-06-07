package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.models.IremboService;
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest;
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest;
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.ui.adapters.IremboServiceAdapter;
import com.drivingschoolrwandaapp.ui.adapters.RecentActivityAdapter;
import com.drivingschoolrwandaapp.utils.PaymentUtils;
import com.drivingschoolrwandaapp.viewmodel.IremboViewModel;
import com.drivingschoolrwandaapp.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IremboActivity extends AppCompatActivity implements IremboServiceAdapter.OnItemClickListener, RecentActivityAdapter.OnItemClickListener {

    private IremboViewModel iremboViewModel;
    private UserViewModel userViewModel;
    private User currentUser;
    private AlertDialog loadingDialog;
    private RecentActivityAdapter recentActivityAdapter;
    private JSONObject locationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_irembo);

        iremboViewModel = new ViewModelProvider(this).get(IremboViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        setupObservers();

        setupToolbar();
        setupRecentActivity();
        setupBrowseServices();
        setupTrackApplication();
        loadLocationData();

        iremboViewModel.fetchRecentApplications();
        userViewModel.loadProfile();
    }

    private void loadLocationData() {
        try {
            InputStream is = getAssets().open("location.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            locationData = new JSONObject(json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading location data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupTrackApplication() {
        TextInputEditText etApplicationNumber = findViewById(R.id.et_application_number);
        Button btnTrack = findViewById(R.id.btn_track);

        btnTrack.setOnClickListener(v -> {
            String appNumber = etApplicationNumber.getText().toString().trim();
            if (TextUtils.isEmpty(appNumber)) {
                etApplicationNumber.setError(getString(R.string.error_required_field));
                return;
            }
            iremboViewModel.fetchApplicationDetails(appNumber);
        });
    }

    private void setupRecentActivity() {
        RecyclerView recyclerView = findViewById(R.id.rv_recent_activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(recentActivityAdapter);

        findViewById(R.id.tv_view_all).setOnClickListener(v -> {
            startActivity(new Intent(this, MyApplicationsActivity.class));
        });
    }

    private void setupBrowseServices() {
        RecyclerView recyclerView = findViewById(R.id.rv_browse_services);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<IremboService> services = new ArrayList<>();
        services.add(new IremboService(getString(R.string.provisional_license), R.drawable.ic_car_side));
        services.add(new IremboService(getString(R.string.special_irembo_service), R.drawable.ic_edit_document));

        IremboServiceAdapter adapter = new IremboServiceAdapter(services, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(IremboService service) {
        if (service.getName().equals(getString(R.string.provisional_license
        ))) {
            showIremboLicenseDialog();
        } else if (service.getName().equals(getString(R.string.special_irembo_service))) {
            showIremboSpecialDialog();
        }
    }

    @Override
    public void onItemClick(IremboApplication application) {
        Intent intent = new Intent(this, ApplicationDetailsActivity.class);
        intent.putExtra("application_details", application);
        startActivity(intent);
    }

    private void setupObservers() {
        iremboViewModel.getRecentApplications().observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    recentActivityAdapter.setApplications(resource.data);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
            }
        });

        iremboViewModel.getLicenseRequestStatus().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else {
                    Toast.makeText(this, "License request submitted successfully", Toast.LENGTH_SHORT).show();
                    iremboViewModel.fetchRecentApplications();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });

        iremboViewModel.getSpecialRequestStatus().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else {
                    Toast.makeText(this, "Special request submitted successfully", Toast.LENGTH_SHORT).show();
                    iremboViewModel.fetchRecentApplications();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_LONG).show();
            }
        });

        iremboViewModel.getApplicationDetails().observe(this, resource -> {
             if (resource.status == Resource.Status.LOADING) {
                 showLoadingDialog();
             } else if (resource.status == Resource.Status.SUCCESS) {
                 hideLoadingDialog();
                 if (resource.data != null) {
                     Intent intent = new Intent(this, ApplicationDetailsActivity.class);
                     intent.putExtra("application_details", resource.data);
                     startActivity(intent);
                 }
             } else if (resource.status == Resource.Status.ERROR) {
                 hideLoadingDialog();
                 Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_LONG).show();
             }
         });

        userViewModel.getUserLiveData().observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    currentUser = resource.data;
                }
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(new android.widget.ProgressBar(this));
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showIremboLicenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_irembo_license, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etName = dialogView.findViewById(R.id.et_applicant_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_applicant_phone);
        TextInputEditText etNationalId = dialogView.findViewById(R.id.et_applicant_national_id);

        if (currentUser != null) {
            etName.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            etPhone.setText(currentUser.getPhoneNumber());
        }

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

            ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, provinces);
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
                        e.printStackTrace();
                    }

                    ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(IremboActivity.this, android.R.layout.simple_spinner_item, districts);
                    districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDistrict.setAdapter(districtAdapter);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Setup Category Spinner
        String[] categories = {"A", "B", "C", "D"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String nationalId = etNationalId.getText().toString().trim();

            String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
            String district = spinnerDistrict.getSelectedItem() != null ? spinnerDistrict.getSelectedItem().toString() : "";
            String address = province + ", " + district;

            Object selectedCategory = spinnerCategory.getSelectedItem();
            String category = selectedCategory != null ? selectedCategory.toString() : "";

            int selectedLicenseTypeId = rgLicenseType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
            if (TextUtils.isEmpty(phone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
            if (TextUtils.isEmpty(nationalId)) { etNationalId.setError(getString(R.string.error_required_field)); return; }
            if (nationalId.length() != 16) { etNationalId.setError("Must be 16 digits"); return; }
            if (TextUtils.isEmpty(province) || TextUtils.isEmpty(district)) { Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show(); return; }
            if (TextUtils.isEmpty(category)) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
            if (selectedLicenseTypeId == -1) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }

            RadioButton rbLicenseType = dialogView.findViewById(selectedLicenseTypeId);

            String licenseType = rbLicenseType.getText().toString().toUpperCase();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
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

        if (currentUser != null) {
            etName.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            etPhone.setText(currentUser.getPhoneNumber());
        }

        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Setup Spinner
        String[] categories = {"A"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String serviceName = "";
            Object selectedCategory = spinnerCategory.getSelectedItem();
            String category = selectedCategory != null ? selectedCategory.toString() : "";

            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String nationalId = etNationalId.getText().toString().trim();
            String description =  "";

            if (TextUtils.isEmpty(category)) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
            if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
            if (TextUtils.isEmpty(phone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_payment_confirmation, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        int amount = (int) paymentDetails.getAmount();

        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        String currency = paymentDetails.getCurrency() != null ? paymentDetails.getCurrency() : "RWF";

        tvAmount.setText(format.format(amount) + " " + currency);

        PaymentUtils.setupPaymentMethods(dialogView, this, String.valueOf(amount));

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            iremboViewModel.fetchRecentApplications();
        });

        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PaymentUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
