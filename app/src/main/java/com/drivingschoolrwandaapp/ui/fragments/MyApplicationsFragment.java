package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.drivingschoolrwandaapp.R;
import com.google.android.material.appbar.MaterialToolbar;

public class MyApplicationsFragment extends Fragment {

    private RecyclerView myApplicationsRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                try {
                    NavHostFragment.findNavController(this).navigateUp();
                } catch (Exception e) {
                    android.util.Log.e("MyApplicationsFragment", "Navigation failed", e);
                }
            }
        });

        myApplicationsRecyclerView = view.findViewById(R.id.rv_my_applications);

        // TODO: Setup Adapter and ViewModel
    }
}
