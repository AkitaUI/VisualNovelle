package com.example.visualstudio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StartFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        // Кнопка "Играть" (осталась без изменений)
        Button btnPlay = view.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v -> {
            // ... ваша логика "Играть" ...
        });

        // Кнопка "Настройки"
        Button btnSettings = view.findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Кнопка "Аутентификация"
        Button btnAuth = view.findViewById(R.id.btn_auth);
        btnAuth.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AuthFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}