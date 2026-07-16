package com.cos.demo.scene;

import lombok.Getter;
import lombok.Setter;

public class SceneContext {
    @Getter
    @Setter
    private Scene scene =  Scene.NORMAL;

    @Getter
    private int round;

    public void increaseRound() {
        round++;
    }

    public void reset() {
        round = 0;
    }
}
