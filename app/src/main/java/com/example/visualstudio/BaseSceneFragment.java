// File: BaseSceneFragment.java
package com.example.visualstudio;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSceneFragment extends Fragment {

    private ImageView ivBackground;
    private ImageView ivLeftChar;
    private ImageView ivRightChar;
    private TextView tvSpeakerName;
    private TextView tvDialogueText;
    private View clickInterceptor;

    private SceneData sceneData;
    private int currentDialogueIndex = 0;
    private final Handler handler = new Handler();

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

        sceneData = createSceneData();

        ivBackground.setImageResource(sceneData.backgroundResId);

        if (sceneData.leftCharResId != null) {
            ivLeftChar.setImageResource(sceneData.leftCharResId);
            ivLeftChar.setVisibility(View.VISIBLE);
        } else {
            ivLeftChar.setVisibility(View.GONE);
        }

        if (sceneData.rightCharResId != null) {
            ivRightChar.setImageResource(sceneData.rightCharResId);
            ivRightChar.setVisibility(View.VISIBLE);
        } else {
            ivRightChar.setVisibility(View.GONE);
        }

        currentDialogueIndex = 0;
        showDialogue();

        clickInterceptor.setOnClickListener(v -> advanceDialogue());

        return view;
    }

    private void showDialogue() {
        List<Dialogue> list = sceneData.dialogues;
        if (currentDialogueIndex < 0 || currentDialogueIndex >= list.size()) {
            if (sceneCompleteListener != null) {
                sceneCompleteListener.onSceneComplete(this);
            }
            return;
        }

        Dialogue dlg = list.get(currentDialogueIndex);
        tvSpeakerName.setText(dlg.speakerName);
        tvDialogueText.setText(dlg.text);

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

    private void shakeImage(ImageView imageView) {
        int delta = 20;
        int cycles = 4;
        int totalDuration = 1000;
        int singleDuration = totalDuration / (cycles * 2);

        TranslateAnimation anim = new TranslateAnimation(-delta, delta, 0, 0);
        anim.setDuration(singleDuration);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(cycles * 2 - 1);
        imageView.startAnimation(anim);

        handler.postDelayed(imageView::clearAnimation, totalDuration);
    }

    protected enum Side { LEFT, RIGHT }

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
