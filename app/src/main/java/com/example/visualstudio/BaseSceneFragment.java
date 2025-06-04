// File: BaseSceneFragment.java
package com.example.visualstudio;

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

    // Данные сцены
    private SceneData sceneData;
    private int currentDialogueIndex = 0;

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

        ivBackground = view.findViewById(R.id.iv_background);
        ivLeftChar = view.findViewById(R.id.iv_left_char);
        ivRightChar = view.findViewById(R.id.iv_right_char);
        tvSpeakerName = view.findViewById(R.id.tv_speaker_name);
        tvDialogueText = view.findViewById(R.id.tv_dialogue_text);
        clickInterceptor = view.findViewById(R.id.click_interceptor);

        // ======== Новые для меню ========
        btnMenu = view.findViewById(R.id.btn_menu);
        menuPopup = view.findViewById(R.id.menu_popup);
        btnMenuClose = view.findViewById(R.id.btn_menu_close);
        btnSaveGame = view.findViewById(R.id.btn_save_game);
        btnLoadGame = view.findViewById(R.id.btn_load_game);
        btnReturnMenu = view.findViewById(R.id.btn_return_menu);

        btnMenu.bringToFront();
        menuPopup.bringToFront();
        btnMenuClose.bringToFront();
        btnSaveGame.bringToFront();
        btnLoadGame.bringToFront();
        btnReturnMenu.bringToFront();

        // ======== Настройки поведения меню ========
        // При клике на иконку меню — показываем или скрываем окно
        btnMenu.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.GONE) {
                menuPopup.setVisibility(View.VISIBLE);
            } else {
                menuPopup.setVisibility(View.GONE);
            }
        });
        // При клике по иконке внутри popup — просто скрыть окно
        btnMenuClose.setOnClickListener(v -> menuPopup.setVisibility(View.GONE));
        // Заглушки для кнопок (пока без логики)
        btnSaveGame.setOnClickListener(v -> {
            // TODO: позже реализуем
        });
        btnLoadGame.setOnClickListener(v -> {
            // TODO: позже реализуем
        });
        btnReturnMenu.setOnClickListener(v -> {
            // TODO: позже реализуем
        });

        // 1. Подготовка данных сцены
        sceneData = createSceneData();

        // 2. Устанавливаем фон
        ivBackground.setImageResource(sceneData.backgroundResId);

        // 3. Вычисляем ширину портрета: 35% ширины экрана
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
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

        // 4. Стартуем с первой реплики
        currentDialogueIndex = 0;
        showDialogue();

        // 5. Нажатие на экран:
        //    - если идёт печать текста, раскрываем сразу весь;
        //    - иначе переходим к следующей реплике.
        clickInterceptor.setOnClickListener(v -> {
            if (isTyping) {
                // Прервать печать и показать весь текст
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
}
