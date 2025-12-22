package com.example.doan_zaloclone.ui.personal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;

/**
 * PersonalFragment - Main fragment for Personal tab
 * Displays user profile card and menu items
 */
public class PersonalFragment extends Fragment {
    
    private PersonalViewModel viewModel;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // TODO: Will be implemented in Phase 3
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: Initialize views and observe ViewModel in Phase 3
    }
}
