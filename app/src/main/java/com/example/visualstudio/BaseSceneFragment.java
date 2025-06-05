// File: BaseSceneFragment.java
package com.example.visualstudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CollectionReference savesCollection;

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

        // Инициализация FirebaseAuth и Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        // Ссылка на коллекцию "saves"
        savesCollection = firestore.collection("saves");

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
            FirebaseUser user = mAuth.getCurrentUser();
            Toast.makeText(getContext(),
                    "SaveGame: currentUser = " + (user == null ? "null" : user.getUid()),
                    Toast.LENGTH_LONG).show();

            int sceneIndex = 0;
            if (getParentFragment() instanceof GameFragment) {
                sceneIndex = ((GameFragment) getParentFragment()).getCurrentSceneIndex();
            }
            int dialogueIndex = currentDialogueIndex;

            if (user != null) {
                // Сохранение в Firestore
                String uid = user.getUid();
                // Подколлекция user_saves внутри документа с ID=uid
                CollectionReference userSavesRef = savesCollection
                        .document(uid)
                        .collection("user_saves");

                // Генерируем новый documentId (Firestore сам создаст пустой документ)
                String saveId = userSavesRef.document().getId();
                SavePoint sp = new SavePoint(sceneIndex, dialogueIndex);

                // Добавляем поле timestamp, чтобы проще сортировать
                Map<String, Object> data = new HashMap<>();
                data.put("sceneIndex", sp.sceneIndex);
                data.put("dialogueIndex", sp.dialogueIndex);
                data.put("timestamp", System.currentTimeMillis());

                userSavesRef.document(saveId)
                        .set(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Сохранено в облаке Firestore", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreSave", "Ошибка при set(): ", e);
                            Toast.makeText(getContext(),
                                    "Ошибка при сохранении: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });

            } else {
                // Гость: локальное сохранение (SharedPreferences), как раньше
                SharedPreferences prefs =
                        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putInt(KEY_SCENE, sceneIndex)
                        .putInt(KEY_DIALOGUE, dialogueIndex)
                        .apply();
                Toast.makeText(getContext(),
                        "Сохранено локально: сцена=" + sceneIndex + " диалог=" + dialogueIndex,
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnLoadGame.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Загрузка списка сохранений из Firestore
                String uid = user.getUid();
                CollectionReference userSavesRef = savesCollection
                        .document(uid)
                        .collection("user_saves");

                // Считываем все документы из user_saves
                userSavesRef
                        .orderBy("timestamp")   // сортировка по времени (по возрастанию или убыванию)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    Toast.makeText(getContext(), "Сохранений нет", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                // Собираем все метки (например, форматируем timestamp в строку)
                                List<String> choices = new ArrayList<>();
                                List<SavePoint> savePoints = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    long ts = doc.getLong("timestamp");
                                    int sIdx = doc.getLong("sceneIndex").intValue();
                                    int dIdx = doc.getLong("dialogueIndex").intValue();

                                    savePoints.add(new SavePoint(sIdx, dIdx));

                                    // Пример форматирования: "04.06.2025 12:34"
                                    String label = new java.text.SimpleDateFormat(
                                            "dd.MM.yyyy HH:mm", java.util.Locale.getDefault()
                                    ).format(new java.util.Date(ts));
                                    choices.add(label);
                                }

                                CharSequence[] items = choices.toArray(new CharSequence[0]);
                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Выберите сохранение")
                                        .setItems(items, (dialog, which) -> {
                                            // Когда пользователь выбирает индекс which
                                            SavePoint sp = savePoints.get(which);
                                            loadFromSave(sp.sceneIndex, sp.dialogueIndex);
                                        })
                                        .show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreLoad", "Ошибка при чтении: ", e);
                            Toast.makeText(getContext(),
                                    "Ошибка загрузки: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            } else {
                // Гость: локальная загрузка
                showGuestSave();
            }
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

        public SavePoint() { }                       // для Firebase RTDB
        public SavePoint(int sceneIndex, int dialogueIndex) {
            this.sceneIndex = sceneIndex;
            this.dialogueIndex = dialogueIndex;
        }
    }
}
