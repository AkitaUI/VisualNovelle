package com.example.visualstudio;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Менеджер сцен: запускает Scene1Fragment → ждёт колбэка «сцена завершилась» →
 * делает плавное затемнение → запускает Scene2Fragment → и т.д.
 */
public class GameFragment extends Fragment implements BaseSceneFragment.SceneCompleteListener {

    private Integer startSceneIndex = null;
    private Integer startDialogueIndex = null;

    public int getCurrentSceneIndex() {
        return currentSceneIndex;
    }

    // Для GameFragment: если нужно передать туда сохранённую позицию через аргументы:
    public static GameFragment newInstance(int sceneIndex, int dialogueIndex) {
        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();
        args.putInt("scene_index", sceneIndex);
        args.putInt("dialogue_index", dialogueIndex);
        fragment.setArguments(args);
        return fragment;
    }

    // Список классов фрагментов-сцен. Можно добавить сколько нужно
    private final Class<? extends BaseSceneFragment>[] sceneClasses = new Class[]{
            Scene1Fragment.class,
            Scene2Fragment.class
            // Добавляйте сюда Scene3Fragment.class, Scene4Fragment.class и т.д.
    };
    private int currentSceneIndex = 0;

    // Контейнер, в который мы будем вставлять сами SceneFragment
    private FrameLayout sceneContainer;
    // View поверх, для затемнения
    private View fadeOverlay;

    // Обработчик для задержек
    private final Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Мы используем layout fragment_game.xml
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        sceneContainer = view.findViewById(R.id.scene_container);
        fadeOverlay = view.findViewById(R.id.fade_overlay);
        fadeOverlay.setVisibility(View.GONE);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey("scene_index") && args.containsKey("dialogue_index")) {
                startSceneIndex = args.getInt("scene_index");
                startDialogueIndex = args.getInt("dialogue_index");
            }
        }

        // Если была передана стартовая позиция — запустим с неё, иначе с 0
        if (startSceneIndex != null) {
            currentSceneIndex = startSceneIndex;
            // При загрузке сцены нам нужно передать в неё startDialogueIndex
            loadScene(currentSceneIndex, startDialogueIndex);
        } else {
            loadScene(currentSceneIndex);
        }

        return view;
    }

    private void loadScene(int index) {
        loadScene(index, 0);
    }

    /**
     * Загружает сцену по индексу index и сразу "перепрыгивает" на dialogueIndex.
     */
    private void loadScene(int index, int dialogueStartIndex) {
        if (index < 0 || index >= sceneClasses.length) return;
        try {
            BaseSceneFragment sceneFragment = sceneClasses[index].newInstance();
            sceneFragment.setSceneCompleteListener(this);

            // Передаём в BaseSceneFragment (или в его подкласс) нужный индекс диалога:
            Bundle args = new Bundle();
            args.putInt("load_dialogue_index", dialogueStartIndex);
            sceneFragment.setArguments(args);

            // Вставляем фрагмент
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.scene_container, sceneFragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Колбэк от BaseSceneFragment, когда сцена закончилась.
     * Делает fade-out → fade-in и запускает следующую сцену.
     */
    @Override
    public void onSceneComplete(BaseSceneFragment finishedFragment) {
        // Плавное затемнение (alpha 0 → 1 за 500мс)
        fadeOverlay.setVisibility(View.VISIBLE);
        AlphaAnimation fadeOut = new AlphaAnimation(0f, 1f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                // Ждём полсекунды, затем скрываем overlay, загружаем новую сцену
                handler.postDelayed(() -> {
                    currentSceneIndex++;
                    loadScene(currentSceneIndex);
                    // Теперь плавно убираем overlay (alpha 1 → 0 за 500мс)
                    AlphaAnimation fadeIn = new AlphaAnimation(1f, 0f);
                    fadeIn.setDuration(500);
                    fadeIn.setFillAfter(true);
                    fadeOverlay.startAnimation(fadeIn);
                }, 200); // небольшая задержка, чтобы был пауза между сценами
            }
            @Override public void onAnimationRepeat(Animation animation) { }
        });
        fadeOverlay.startAnimation(fadeOut);
    }
}
