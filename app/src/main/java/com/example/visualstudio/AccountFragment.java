package com.example.visualstudio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountFragment extends Fragment {

    private TextView tvName, tvEmail;
    private LinearLayout savesContainer;
    private Button btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        savesContainer = view.findViewById(R.id.saves_container);
        btnBack = view.findViewById(R.id.btn_back);

        // Получаем текущего пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Отображаем email
            tvEmail.setText(currentUser.getEmail());

            // Если вы в будущем добавите displayName, можно сделать:
            String displayName = currentUser.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "Без имени";
            }
            tvName.setText(displayName);
        } else {
            tvName.setText("Гость");
            tvEmail.setText("-");
        }

        // Генерируем заглушки «Сохранение 1…5»
        for (int i = 1; i <= 5; i++) {
            TextView saveItem = new TextView(requireContext());
            saveItem.setText("Сохранение " + i);
            saveItem.setTextSize(16f);
            saveItem.setPadding(0, 8, 0, 8);
            savesContainer.addView(saveItem);
        }

        btnBack.setOnClickListener(v -> {
            // Возвращаемся на StartFragment (без бэктрека AuthFragment)
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StartFragment())
                    .commit();
        });

        return view;
    }
}