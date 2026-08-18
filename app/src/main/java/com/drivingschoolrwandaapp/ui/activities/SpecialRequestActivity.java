package com.drivingschoolrwandaapp.ui.activities;

import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.utils.AnalyticsUtils;
import com.drivingschoolrwandaapp.utils.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SpecialRequestActivity extends BaseIremboFormActivity {

    private AutoCompleteTextView actvCategory;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etNationalId;

    @Override
    protected int getFormLayoutId() {
        return R.layout.activity_irembo_special_request;
    }

    @Override
    protected void onFormViewsReady() {
        actvCategory = findViewById(R.id.actv_category);
        etName = findViewById(R.id.et_applicant_name);
        etPhone = findViewById(R.id.et_applicant_phone);
        etNationalId = findViewById(R.id.et_national_id);

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

    private void setupCategorySpinner() {
        String[] categories = {"A"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_dropdown_menu, categories);
        actvCategory.setAdapter(adapter);
    }

    private void setupSubmit() {
        findViewById(R.id.btn_submit).setOnClickListener(v -> submit());
    }

    private void submit() {
        String serviceName = getString(R.string.special_irembo_service);
        CharSequence categoryText = actvCategory.getText();
        String category = categoryText != null ? categoryText.toString().trim() : "";

        CharSequence nameText = etName.getText();
        String name = nameText != null ? nameText.toString().trim() : "";
        CharSequence phoneText = etPhone.getText();
        String rawPhone = phoneText != null ? phoneText.toString().trim() : "";
        CharSequence nationalIdText = etNationalId.getText();
        String nationalId = nationalIdText != null ? nationalIdText.toString().trim() : "";
        String description = "";

        if (TextUtils.isEmpty(category)) { Toast.makeText(this, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(name)) { etName.setError(getString(R.string.error_required_field)); return; }
        if (TextUtils.isEmpty(rawPhone)) { etPhone.setError(getString(R.string.error_required_field)); return; }
        String phoneError = PhoneUtils.getValidationError(rawPhone);
        if (phoneError != null) { etPhone.setError(getString(R.string.invalid_phone)); return; }
        String phone = PhoneUtils.normalize(rawPhone);
        if (TextUtils.isEmpty(nationalId)) { etNationalId.setError(getString(R.string.error_required_field)); return; }
        if (nationalId.length() != 16) { etNationalId.setError(getString(R.string.national_id_16_digits)); return; }

        // Block duplicates: a user may only have one active special request.
        if (iremboViewModel.hasActiveIremboRequest("SPECIAL")) {
            showAlreadyRequestedDialog();
            return;
        }

        IremboSpecialRequest request = new IremboSpecialRequest(
                serviceName, category, name, phone, nationalId, description
        );
        AnalyticsUtils.logIremboRequestSubmitted(this, "special_service");
        iremboViewModel.submitSpecialRequest(request);
    }

    private void setupSubmitObserver() {
        iremboViewModel.getSpecialRequestStatus().observe(this, resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.LOADING) {
                showLoadingDialog();
            } else if (resource.status == Resource.Status.SUCCESS) {
                hideLoadingDialog();
                if (resource.data != null) {
                    showPaymentConfirmationDialog(resource.data);
                } else if (!isFinishing()) {
                    Toast.makeText(this, getString(R.string.special_submitted), Toast.LENGTH_SHORT).show();
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
