// File: BaseSceneFragment.java
package com.example.visualstudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSceneFragment extends Fragment {

    // UI-элементы базовые
    private ImageView ivBackground;
    private ImageView ivLeftChar;
    private ImageView ivRightChar;
    private TextView tvSpeakerName;
    private TextView tvDialogueText;
    private View clickInterceptor;

    // Новые поля для меню
    private ImageButton btnMenu;         // основная иконка
    private LinearLayout menuPopup;      // всплывающее окно
    private ImageButton btnMenuClose;    // иконка внутри popup для закрытия
    private Button btnSaveGame;
    private Button btnLoadGame;
    private Button btnReturnMenu;

    private com.google.firebase.auth.FirebaseAuth mAuth;
    private com.google.firebase.database.DatabaseReference savesRef;

    private static final String PREFS_NAME = "guest_save_prefs";
    private static final String KEY_SCENE = "local_scene_index";
    private static final String KEY_DIALOGUE = "local_dialogue_index";

    // Данные сцены
    private SceneData sceneData;
    protected int currentDialogueIndex = 0;

    // Для эффекта «печати»
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;
    private boolean isTyping = false;
    private CharSequence fullTextToType;
    private int typingIndex = 0;
    private static final long TYPING_DELAY_MS = 50; // 50 ms на символ → 20 символов/с

    // Для «тряски» портрета
    private final Handler shakeHandler = new Handler(Looper.getMainLooper());

    public interface SceneCompleteListener {
        void onSceneComplete(BaseSceneFragment finishedFragment);
    }

    private SceneCompleteListener sceneCompleteListener;

    protected abstract SceneData createSceneData();

    public void setSceneCompleteListener(SceneCompleteListener listener) {
        this.sceneCompleteListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base_scene, container, false);

        // ======== Инициализируем mAuth и savesRef (но пока не используем в btnLoadGame) ========
        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.database.FirebaseDatabase database =
                com.google.firebase.database.FirebaseDatabase.getInstance();
        savesRef = database.getReference("saves");

        // ======== Находим все View-элементы ========
        ivBackground     = view.findViewById(R.id.iv_background);
        ivLeftChar       = view.findViewById(R.id.iv_left_char);
        ivRightChar      = view.findViewById(R.id.iv_right_char);
        tvSpeakerName    = view.findViewById(R.id.tv_speaker_name);
        tvDialogueText   = view.findViewById(R.id.tv_dialogue_text);
        clickInterceptor = view.findViewById(R.id.click_interceptor);

        // ======== Меню ========
        btnMenu       = view.findViewById(R.id.btn_menu);
        menuPopup     = view.findViewById(R.id.menu_popup);
        btnMenuClose  = view.findViewById(R.id.btn_menu_close);
        btnSaveGame   = view.findViewById(R.id.btn_save_game);
        btnLoadGame   = view.findViewById(R.id.btn_load_game);
        btnReturnMenu = view.findViewById(R.id.btn_return_menu);

        btnMenu.bringToFront();
        menuPopup.bringToFront();
        btnMenuClose.bringToFront();
        btnSaveGame.bringToFront();
        btnLoadGame.bringToFront();
        btnReturnMenu.bringToFront();

        // ======== Настройки поведения меню ========
        btnMenu.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.GONE) {
                menuPopup.setVisibility(View.VISIBLE);
            } else {
                menuPopup.setVisibility(View.GONE);
            }
        });

        btnMenuClose.setOnClickListener(v -> menuPopup.setVisibility(View.GONE));

        btnSaveGame.setOnClickListener(v -> {
            // 1) Определяем текущую позицию
            int sceneIndex = 0;
            if (getParentFragment() instanceof GameFragment) {
                sceneIndex = ((GameFragment) getParentFragment()).getCurrentSceneIndex();
            }
            int dialogueIndex = currentDialogueIndex;

            // 2) Сохраняем в SharedPreferences (гость)
            SharedPreferences prefs =
                    requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt(KEY_SCENE, sceneIndex)
                    .putInt(KEY_DIALOGUE, dialogueIndex)
                    .apply();

            // Сообщаем, что локальное сохранение записано
            Toast.makeText(getContext(),
                    "Сохранено локально: сцена=" + sceneIndex + " диалог=" + dialogueIndex,
                    Toast.LENGTH_SHORT).show();

            // Загружаем это же сохранение (для проверки)
            showGuestSave();
        });

        // Убираем любую логику Firebase, оставляем только локальную загрузку
        btnLoadGame.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Нажата кнопка LoadGame (локальный режим)", Toast.LENGTH_SHORT).show();
            showGuestSave();
        });

        btnReturnMenu.setOnClickListener(v -> {
            menuPopup.setVisibility(View.GONE);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StartFragment())
                    .commit();
        });

        // ======== Чтение аргумента "load_dialogue_index" ========
        Bundle bb = getArguments();
        if (bb != null && bb.containsKey("load_dialogue_index")) {
            currentDialogueIndex = bb.getInt("load_dialogue_index", 0);
        } else {
            currentDialogueIndex = 0;
        }

        // ======== Подготовка данных сцены ========
        sceneData = createSceneData();

        // ======== Установка фона ========
        ivBackground.setImageResource(sceneData.backgroundResId);

        // ======== Вычисляем ширину портрета: 35% ширины экрана ========
        int screenWidth   = getResources().getDisplayMetrics().widthPixels;
        int portraitWidth = (int) (screenWidth * 0.35f);

        // Левый портрет
        if (sceneData.leftCharResId != null) {
            ivLeftChar.setImageResource(sceneData.leftCharResId);
            ViewGroup.LayoutParams lpLeft = ivLeftChar.getLayoutParams();
            lpLeft.width = portraitWidth;
            ivLeftChar.setLayoutParams(lpLeft);
            ivLeftChar.setVisibility(View.VISIBLE);
        } else {
            ivLeftChar.setVisibility(View.GONE);
        }

        // Правый портрет
        if (sceneData.rightCharResId != null) {
            ivRightChar.setImageResource(sceneData.rightCharResId);
            ViewGroup.LayoutParams lpRight = ivRightChar.getLayoutParams();
            lpRight.width = portraitWidth;
            ivRightChar.setLayoutParams(lpRight);
            ivRightChar.setVisibility(View.VISIBLE);
        } else {
            ivRightChar.setVisibility(View.GONE);
        }

        // ======== Показ текущей реплики ========
        showDialogue();

        // ======== Перехват клика по экрану ========
        clickInterceptor.setOnClickListener(v -> {
            if (isTyping) {
                finishTypingImmediately();
            } else {
                advanceDialogue();
            }
        });

        return view;
    }

    private void showDialogue() {
        List<Dialogue> list = sceneData.dialogues;
        if (currentDialogueIndex < 0 || currentDialogueIndex >= list.size()) {
            // Все реплики закончились → уведомляем слушателя
            if (sceneCompleteListener != null) {
                sceneCompleteListener.onSceneComplete(this);
            }
            return;
        }

        Dialogue dlg = list.get(currentDialogueIndex);
        // Имя говорящего (показываем сразу)
        tvSpeakerName.setText(dlg.speakerName);

        // Запускаем «типографический» эффект для текста
        startTyping(dlg.text);

        // Анимация «тряски» портрета говорящего
        if (dlg.side == Side.LEFT && ivLeftChar.getVisibility() == View.VISIBLE) {
            shakeImage(ivLeftChar);
        } else if (dlg.side == Side.RIGHT && ivRightChar.getVisibility() == View.VISIBLE) {
            shakeImage(ivRightChar);
        }
    }

    private void advanceDialogue() {
        currentDialogueIndex++;
        showDialogue();
    }

    private void startTyping(CharSequence fullText) {
        // Остановим предыдущую печать, если она ещё шла
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        isTyping = true;
        typingIndex = 0;
        fullTextToType = fullText;
        tvDialogueText.setText(""); // очищаем поле

        // Запускаем Runnable, который добавляет по 1 символу каждые TYPING_DELAY_MS
        typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (typingIndex <= fullTextToType.length()) {
                    // Берём подстроку [0..typingIndex)
                    CharSequence sub = fullTextToType.subSequence(0, typingIndex);
                    tvDialogueText.setText(sub);
                    typingIndex++;
                    // Если не дошли до конца, запланировать следующий «тираж»
                    if (typingIndex <= fullTextToType.length()) {
                        typingHandler.postDelayed(this, TYPING_DELAY_MS);
                    } else {
                        // Закончили печать
                        isTyping = false;
                    }
                }
            }
        };
        // Первый запуск сразу (покажет пустую либо первую букву, в зависимости от index)
        typingHandler.post(typingRunnable);
    }

    private void finishTypingImmediately() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        isTyping = false;
        // Показать весь текст
        Dialogue dlg = sceneData.dialogues.get(currentDialogueIndex);
        tvDialogueText.setText(dlg.text);
    }

    private void shakeImage(ImageView imageView) {
        int delta = 30;           // смещение в пикселях
        int cycles = 4;           // 4 полных «туда-обратно» в секунду
        int totalDuration = 1000; // миллисекунд
        int singleDuration = totalDuration / (cycles * 2);

        TranslateAnimation anim = new TranslateAnimation(0, 0, -delta, delta);
        anim.setDuration(singleDuration);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(cycles * 2 - 1);
        imageView.startAnimation(anim);

        // Очистим анимацию по окончании, чтобы вернуть портрет на исходную позицию
        shakeHandler.postDelayed(imageView::clearAnimation, totalDuration);
    }

    /**
     * Получает список сохранений для данного uid и показывает AlertDialog
     * с именами (ключами) сохранений. При выборе — грузим сохранение.
     */
    private void showSaveListFromFirebase(String uid) {
        com.google.firebase.database.DatabaseReference userSavesRef =
                savesRef.child(uid);
        userSavesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    android.widget.Toast.makeText(getContext(),
                            "Нет сохранений", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                // Собираем список ключей (timestamp) и отображаем их
                List<String> saveKeys = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    saveKeys.add(child.getKey());
                }
                CharSequence[] items = saveKeys.toArray(new CharSequence[0]);
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Выберите сохранение")
                        .setItems(items, (dialog, which) -> {
                            String chosenKey = saveKeys.get(which);
                            // После выбора считываем SavePoint и грузим
                            com.google.firebase.database.DataSnapshot chosenSnapshot =
                                    snapshot.child(chosenKey);
                            SavePoint sp = chosenSnapshot.getValue(SavePoint.class);
                            if (sp != null) {
                                loadFromSave(sp.sceneIndex, sp.dialogueIndex);
                            }
                        })
                        .show();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                android.widget.Toast.makeText(getContext(),
                        "Ошибка доступа к базе: " + error.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Загружает единственное локальное сохранение из SharedPreferences (для гостя).
     */
    /**
     * Загружает единственное локальное сохранение из SharedPreferences (для гостя).
     */
    private void showGuestSave() {
        Toast.makeText(getContext(), "showGuestSave() вызван", Toast.LENGTH_SHORT).show();

        SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_SCENE)) {
            Toast.makeText(getContext(), "Нет локального сохранения", Toast.LENGTH_SHORT).show();
            return;
        }
        int sceneIndex    = prefs.getInt(KEY_SCENE, 0);
        int dialogueIndex = prefs.getInt(KEY_DIALOGUE, 0);

        Toast.makeText(getContext(),
                "Загружаем локально: сцена=" + sceneIndex + " диалог=" + dialogueIndex,
                Toast.LENGTH_SHORT).show();

        loadFromSave(sceneIndex, dialogueIndex);
    }

    private void loadFromSave(int sceneIndex, int dialogueIndex) {
        menuPopup.setVisibility(View.GONE);

        if (getParentFragment() instanceof GameFragment) {
            GameFragment newGame = GameFragment.newInstance(sceneIndex, dialogueIndex);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, newGame)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /** Сторона говорящего (лево или право) */
    protected enum Side { LEFT, RIGHT }

    /** Одна реплика: имя, текст, сторона. */
    protected static class Dialogue {
        public final String speakerName;
        public final String text;
        public final Side side;

        public Dialogue(String speakerName, String text, Side side) {
            this.speakerName = speakerName;
            this.text = text;
            this.side = side;
        }
    }

    protected static class SceneData {
        public final int backgroundResId;
        public final Integer leftCharResId;
        public final Integer rightCharResId;
        public final List<Dialogue> dialogues = new ArrayList<>();

        public SceneData(int backgroundResId, Integer leftCharResId, Integer rightCharResId) {
            this.backgroundResId = backgroundResId;
            this.leftCharResId = leftCharResId;
            this.rightCharResId = rightCharResId;
        }

        public void addDialogue(Dialogue d) {
            dialogues.add(d);
        }
    }

    public static class SavePoint {
        public int sceneIndex;
        public int dialogueIndex;

        public SavePoint() { } // конструктор без параметров нужен для Firebase

        public SavePoint(int sceneIndex, int dialogueIndex) {
            this.sceneIndex = sceneIndex;
            this.dialogueIndex = dialogueIndex;
        }
    }
}
