package com.example.visualstudio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Scene2Fragment extends BaseSceneFragment {
    @Override
    protected SceneData createSceneData() {
        int bg = R.drawable.corridor1;
        Integer alice = null;               // У Алисы нет портрета в этой сцене
        Integer bob = R.drawable.friend_unhappy;  // У Боба есть портрет

        SceneData data = new SceneData(bg, alice, bob);
        data.addDialogue(new Dialogue("Боб", "Где же Алиса? Она пропала...", Side.RIGHT));
        data.addDialogue(new Dialogue("Боб", "Нужно её срочно найти.", Side.RIGHT));
        return data;
    }
}

