package com.example.visualstudio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.List;

public class AccountFragment extends Fragment {

    private TextView tvName, tvEmail;
    private LinearLayout savesContainer;
    private Button btnBack, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CollectionReference savesCollection;

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
        btnLogout = view.findViewById(R.id.btn_logout);

        // Инициализация FirebaseAuth + Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        savesCollection = firestore.collection("saves");

        // 1) Получаем текущего пользователя
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String displayName = currentUser.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "Без имени";
            }
            tvName.setText(displayName);
            tvEmail.setText(email);

            // Загружаем сохранения
            String uid = currentUser.getUid();
            CollectionReference userSavesRef = savesCollection
                    .document(uid)
                    .collection("user_saves");

            // Очищаем контейнер, чтобы не было старых элементов
            savesContainer.removeAllViews();

            userSavesRef
                    .orderBy("timestamp") // сортировка (по возрастанию)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (queryDocumentSnapshots.isEmpty()) {
                                TextView tvEmpty = new TextView(requireContext());
                                tvEmpty.setText("Сохранений нет");
                                tvEmpty.setPadding(0, 8, 0, 8);
                                savesContainer.addView(tvEmpty);
                                return;
                            }

                            List<BaseSceneFragment.SavePoint> savePoints = new ArrayList<>();
                            List<String> labels = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                int sIdx = doc.getLong("sceneIndex").intValue();
                                int dIdx = doc.getLong("dialogueIndex").intValue();
                                long ts = doc.getLong("timestamp");

                                savePoints.add(new BaseSceneFragment.SavePoint(sIdx, dIdx));

                                String label = new java.text.SimpleDateFormat(
                                        "dd.MM.yyyy HH:mm", java.util.Locale.getDefault()
                                ).format(new java.util.Date(ts));
                                labels.add(label);
                            }

                            for (int i = 0; i < labels.size(); i++) {
                                String label = labels.get(i);
                                BaseSceneFragment.SavePoint sp = savePoints.get(i);

                                TextView tvSave = new TextView(requireContext());
                                tvSave.setText(label);
                                tvSave.setTextSize(16f);
                                tvSave.setPadding(0, 8, 0, 8);
                                final int idx = i;
                                tvSave.setOnClickListener(v -> {
                                    // Загружаем сохранение
                                    BaseSceneFragment.SavePoint chosen = savePoints.get(idx);
                                    GameFragment newGame = GameFragment.newInstance(
                                            chosen.sceneIndex,
                                            chosen.dialogueIndex
                                    );
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, newGame)
                                            .commit();
                                });
                                savesContainer.addView(tvSave);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("FirestoreLoad", "Ошибка при чтении: ", e);
                            Toast.makeText(requireContext(),
                                    "Ошибка загрузки: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        } else {
            // Если гость
            tvName.setText("Гость");
            tvEmail.setText("-");
            savesContainer.removeAllViews();
            TextView tvPrompt = new TextView(requireContext());
            tvPrompt.setText("Пожалуйста, войдите в аккаунт, чтобы видеть сохранения");
            tvPrompt.setPadding(0, 8, 0, 8);
            savesContainer.addView(tvPrompt);
        }

        // Кнопка “Назад” возвращает на StartFragment
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StartFragment())
                    .commit();
        });

        // Кнопка “Выйти” (logout)
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StartFragment())
                    .commit();
        });

        return view;
    }

    // Вспомогательный метод для форматирования timestamp → "dd.MM.yyyy HH:mm"
    private String formatTimestamp(long ts) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "dd.MM.yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(ts));
    }
}