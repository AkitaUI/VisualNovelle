package com.example.visualstudio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Пример конкретной сцены, наследуемой от BaseSceneFragment.
 * В методе createSceneData() описываем фон, портреты и реплики.
 */
public class Scene1Fragment extends BaseSceneFragment {

    @Override
    protected SceneData createSceneData() {
        // Замените на свои ресурсы из drawable
        int bg = R.drawable.bedroom1;
        int alice = R.drawable.gg_happy;
        int bob = R.drawable.maiden_calm;

        SceneData data = new SceneData(bg, alice, bob);
        data.addDialogue(new Dialogue("Алиса", "Привет, Боб! Как ты?", Side.LEFT));
        data.addDialogue(new Dialogue("Боб", "Привет, Алиса! Я отлично.", Side.RIGHT));
        data.addDialogue(new Dialogue("Алиса", "Давай отправимся в путешествие.", Side.LEFT));
        return data;
    }
}
