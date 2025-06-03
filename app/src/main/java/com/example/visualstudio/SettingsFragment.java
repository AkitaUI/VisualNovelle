// File: SettingsFragment.java
package com.example.visualstudio;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends Fragment {

    private Switch switchDarkMode;
    private SeekBar seekbarTextSpeed;
    private TextView tvTextSpeedValue;
    private SeekBar seekbarFontSize;
    private TextView tvFontSizeValue;
    private TextView tvLanguage;
    private Switch switchTutorial;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. Находим элементы
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        seekbarTextSpeed = view.findViewById(R.id.seekbar_text_speed);
        tvTextSpeedValue = view.findViewById(R.id.tv_text_speed_value);

        seekbarFontSize = view.findViewById(R.id.seekbar_font_size);
        tvFontSizeValue = view.findViewById(R.id.tv_font_size_value);

        tvLanguage = view.findViewById(R.id.tv_language);
        switchTutorial = view.findViewById(R.id.switch_tutorial);
        Button btnBack = view.findViewById(R.id.btn_back_settings);

        // 2. Получаем SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // ====== Dark Mode ======
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(darkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            requireActivity().recreate();
        });

        // ====== Text Speed (заглушка) ======
        int textSpeed = prefs.getInt("text_speed", 50); // 0..100
        seekbarTextSpeed.setProgress(textSpeed);
        tvTextSpeedValue.setText(textSpeed + "%");
        seekbarTextSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTextSpeedValue.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("text_speed", seekBar.getProgress()).apply();
            }
        });

        // ====== Font Size (заглушка) ======
        int fontSize = prefs.getInt("font_size", 16); //  десятичное значение sp
        seekbarFontSize.setProgress(fontSize);
        tvFontSizeValue.setText(fontSize + "sp");
        seekbarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvFontSizeValue.setText(progress + "sp");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("font_size", seekBar.getProgress()).apply();
            }
        });

        // ====== Tutorial (заглушка) ======
        boolean tutorialEnabled = prefs.getBoolean("tutorial_enabled", true);
        switchTutorial.setChecked(tutorialEnabled);
        switchTutorial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("tutorial_enabled", isChecked).apply();
        });

        // ====== Language (исправлено, без локальной переменной) ======
        // Устанавливаем текст при старте
        String currentLang = prefs.getString("app_language", "Русский");
        tvLanguage.setText("Язык: " + currentLang);

        tvLanguage.setOnClickListener(v -> {
            // Читаем актуальный язык из SharedPreferences
            String lang = prefs.getString("app_language", "Русский");
            String newLang = lang.equals("Русский") ? "English" : "Русский";
            prefs.edit().putString("app_language", newLang).apply();
            tvLanguage.setText("Язык: " + newLang);
            requireActivity().recreate();
        });

        // ====== Кнопка "Назад" ======
        btnBack.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        return view;
    }
}
