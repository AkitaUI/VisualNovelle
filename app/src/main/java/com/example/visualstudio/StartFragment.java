package com.example.visualstudio;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            ImageView backgroundImage = view.findViewById(R.id.blur_background);
            float radius = 10f; // 10% размытие — небольшое
            RenderEffect blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP);
            backgroundImage.setRenderEffect(blurEffect);
        }

        // Кнопка "Играть" (осталась без изменений)
        Button btnPlay = view.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new GameFragment())
                    .addToBackStack(null)
                    .commit();
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