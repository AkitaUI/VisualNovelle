// File: AuthFragment.java
package com.example.visualstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnLogin, btnCancel;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        // Инициализируем Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Находим все View-элементы
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogin = view.findViewById(R.id.btn_login);
        btnCancel = view.findViewById(R.id.btn_cancel_auth);  // новая кнопка
        progressBar = view.findViewById(R.id.progress_bar);

        progressBar.setVisibility(View.GONE);

        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());

        // Обработчик для кнопки «Отмена» (переход на StartFragment)
        btnCancel.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StartFragment())
                    .commit();
        });

        return view;
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Введите email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Введите пароль");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Пароль должен быть ≥ 6 символов");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            UserProfile profile = new UserProfile(user.getEmail());
                            usersRef.child(uid).setValue(profile)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                        requireActivity()
                                                .getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragment_container, new AccountFragment())
                                                .commit();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(),
                                                    "Ошибка при сохранении профиля: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show()
                                    );
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Не удалось зарегистрироваться: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Введите email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Введите пароль");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(getContext(), "Вход выполнен!", Toast.LENGTH_SHORT).show();
                            requireActivity()
                                    .getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new AccountFragment())
                                    .commit();
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка входа: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
