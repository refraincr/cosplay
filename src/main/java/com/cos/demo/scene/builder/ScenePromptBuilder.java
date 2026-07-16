package com.cos.demo.scene.builder;

import com.cos.demo.constant.ScenePromptConstant;
import com.cos.demo.scene.Scene;
//import com.cos.demo.scene.SceneManager;
import org.springframework.stereotype.Component;

@Component
public class ScenePromptBuilder {
//    private final SceneManager sceneManager;
//
//    public ScenePromptBuilder(SceneManager sceneManager) {
//        this.sceneManager = sceneManager;
//    }

    public String build(String sessionId) {
//        Scene scene = sceneManager.get(sessionId).getScene();
//
//        return switch (scene) {
//            case IDIOM -> ScenePromptConstant.IDIOM;
//            default -> "";
//        };
        return "";
    }
}
