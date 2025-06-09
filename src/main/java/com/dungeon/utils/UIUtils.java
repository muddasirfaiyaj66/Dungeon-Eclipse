package com.dungeon.utils;

import javafx.scene.image.Image;
import javafx.stage.Stage;

public class UIUtils {

    /**
     * Sets the application icon for the given stage.
     * @param stage The stage to set the icon on.
     */
    public static void setStageIcon(Stage stage) {
        try {
            Image icon = new Image(UIUtils.class.getResourceAsStream("/com/dungeon/assets/images/dungeon_eclipse.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Error loading application icon: " + e.getMessage());
        }
    }
} 