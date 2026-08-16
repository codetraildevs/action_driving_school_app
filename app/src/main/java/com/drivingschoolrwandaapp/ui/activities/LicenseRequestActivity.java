package com.drivingschoolrwandaapp.ui.activities;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.utils.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LicenseRequestActivity extends BaseIremboFormActivity {

    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etNationalId;
    private AutoCompleteTextView actvProvince;
    private AutoCompleteTextView actvDistrict;
    private AutoCompleteTextView actvCategory;
    private RadioGroup rgLicenseType;

    @Override
    protected int getFormLayoutId() {
        return R.layout.activity_irembo_license_request;
    }

    @Override
    protected void onFormViewsReady() {
        etName = findViewById(R.id.et_applicant_name);
        etPhone = findViewById(R.id.et_applicant_phone);
        etNationalId = findViewById(R.id.et_applicant_national_id);
        actvProvince = findViewById(R.id.actv_province);
        actvDistrict = findViewById(R.id.actv_district);
        actvCategory = findViewById(R.id.actv_category);
        rgLicenseType = findViewById(R.id.rg_license_type);

        setupLocationSpinners();
        setupCategorySpinner();
        setupSubmit();
        setupSubmitObserver();
    }

    @Override
    protected void onUserLoaded(User user) {
        if (user == null) return;
        if (etName != null) {
            etName.setText(getString(R.string.full_name_format, user.getFirstName(), user.getLastName()));
        }
        if (etPhone != null) {
            etPhone.setText(user.getPhoneNumber());
        }
    }

    private void setupLocationSpinners() {
        if (locationData == null) {
            // location.json may still be loading; try again shortly
            actvProvince.postDelayed(this::setupLocationSpinners, 200);
            return;
        }

        List<String> provinces = new ArrayList<>();
        Iterator<String> keys = locationData.keys();
        while (keys.hasNext()) {
            provinces.add(keys.next());
        }
        Collections.sort(provinces);

        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(this,
                R.layout.item_dropdown_menu, provinces);
        actvProvince.setAdapter(provinceAdapter);

        actvProvince.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
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
                Log.e("LicenseRequest", "Error loading districts for province", e);
            }

            ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(LicenseRequestActivity.this,
                    R.layout.item_dropdown_menu, districts);
            actvDistrict.setAdapter(districtAdapter);
            actvDistrict.setText("");
        });
    }

    private void setupCategorySpinner() {
        String[] categories = {"A", "B", "C", "D"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_dropdown_menu, categories);
        actvCategory.setAdapter(adapter);
    }

    private void setupSubmit() {
        findViewById(R.id.btn_submit).setOnClickListener(v -> submit());
    }

    private void submit() {
        CharSequence nameText = etName.getText();
        String name = nameText != null ? nameText.toString().trim() : "";
        CharSequence phoneText = etPhone.getText();
        String rawPhone = phoneText != null ? phoneText.toString().trim() : "";
        CharSequence nationalIdText = etNationalId.getText();
        String nationalId = nationalIdText != null ? nationalIdText.toString().trim() : "";

        CharSequence provinceText = actvProvince.getText();
        String province = provinceText != null ? provinceText.toString().trim() : "";
        CharSequence districtText = actvDistrict.getText();
        String district = districtText != null ? districtText.toString().trim() : "";
        String address = province + ", " + district;

        CharSequence categoryText = actvCategory.getText();
        String category = categoryText != null ? categoryText.toString().trim() : "";

        int selectedLicenseTypeId = rgLicenseType.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
        if (TextUtils.isEmpty(rawPhone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
        String phoneError = PhoneUtils.getValidationError(rawPhone);
        if (phoneError != null) { etPhone.setError(getString(R.string.invalid_phone)); return; }
        String phone = PhoneUtils.normalize(rawPhone);
        if (TextUtils.isEmpty(nationalId)) { etNationalId.setError(getString(R.string.error_required_field)); return; }
        if (nationalId.length() != 16) { etNationalId.setError(getString(R.string.national_id_16_digits)); return; }
        if (TextUtils.isEmpty(province) || TextUtils.isEmpty(district)) { Toast.makeText(this, getString(R.string.please_select_location), Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(category)) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
        if (selectedLicenseTypeId == -1) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }

        RadioButton rbLicenseType = findViewById(selectedLicenseTypeId);
        if (rbLicenseType == null) {
            Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show();
            return;
        }

        // Map the selected radio to a clean server value instead of its
        // localized label (e.g. the Kinyarwanda radio text would otherwise
        // be stored verbatim in the database).
        String licenseType;
        if (selectedLicenseTypeId == R.id.rb_full) {
            licenseType = "FULL";
        } else {
            licenseType = "NEW";
        }
        String appType = "New";

        IremboLicenseRequest request = new IremboLicenseRequest(
                category, licenseType, appType, name, phone, nationalId, address
        );

        iremboViewModel.submitLicenseRequest(request);
    }

    private void setupSubmitObserver() {
        iremboViewModel.getLicenseRequestStatus().observe(this, resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else if (!isFinishing()) {
                    Toast.makeText(this, getString(R.string.license_submitted), Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoadingDialog();
                if (!isFinishing()) {
                    Toast.makeText(this, getString(R.string.error_format, resource.message != null ? resource.message : getString(R.string.something_went_wrong)), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
