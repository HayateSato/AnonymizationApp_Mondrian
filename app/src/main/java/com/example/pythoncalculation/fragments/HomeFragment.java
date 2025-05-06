package com.example.pythoncalculation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.pythoncalculation.R;
import com.example.pythoncalculation.databinding.FragmentHomeBinding;

/**
 * Home page fragment that serves as the main landing page for the application.
 * Provides buttons to navigate to other sections of the app.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the NavController for navigation
        NavController navController = Navigation.findNavController(view);

        // Set up navigation to Data Fragment
        binding.checkDataButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_home_to_data));

        // Set up navigation to Anonymization Fragment
        binding.anonymizeButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_home_to_anonymization));

        // Set up navigation to Settings Fragment
        binding.settingsButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_home_to_settings));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
