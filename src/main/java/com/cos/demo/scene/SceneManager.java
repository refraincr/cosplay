package com.cos.demo.scene;

import org.springframework.stereotype.Component;

@Component
public class SceneManager {
    public void changeScene(String scene) {
        switch (scene) {
            case "IDIOM"->SceneState.scene = Scene.IDIOM;
            case "NORMAL"->SceneState.scene = Scene.NORMAL;
        }
    }
}
