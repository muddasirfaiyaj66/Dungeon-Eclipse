package com.dungeon.controllers;

import com.dungeon.audio.SoundManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;

public class OptionsController {

    @FXML
    private Slider masterVolumeSlider;
    @FXML
    private Label masterVolumeLabel;

    @FXML
    private Slider musicVolumeSlider;
    @FXML
    private Label musicVolumeLabel;

    @FXML
    private Slider effectsVolumeSlider;
    @FXML
    private Label effectsVolumeLabel;

    @FXML
    private CheckBox muteCheckBox;

    @FXML
    private Button closeButton;

    private SoundManager soundManager;

    private static final DecimalFormat percentFormat = new DecimalFormat("#0%");

    @FXML
    public void initialize() {
        soundManager = SoundManager.getInstance();
        setupVolumeControls();
        updateUIFromSoundManager();
    }

    private void setupVolumeControls() {
        // Master Volume
        masterVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            soundManager.setMasterVolume(newVal.doubleValue());
            masterVolumeLabel.setText(percentFormat.format(newVal.doubleValue()));
        });

        // Music Volume
        musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            soundManager.setMusicVolume(newVal.doubleValue());
            musicVolumeLabel.setText(percentFormat.format(newVal.doubleValue()));
        });

        // Effects Volume
        effectsVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            soundManager.setEffectsVolume(newVal.doubleValue());
            effectsVolumeLabel.setText(percentFormat.format(newVal.doubleValue()));
        });

        // Mute CheckBox
        muteCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != soundManager.isGlobalMuted()) { // Avoid redundant calls if already in desired state
                soundManager.toggleGlobalMute();
            }
            // Disabling sliders when muted can be a good UX, re-enable when unmuted
            masterVolumeSlider.setDisable(newVal);
            musicVolumeSlider.setDisable(newVal);
            effectsVolumeSlider.setDisable(newVal);
        });
    }

    private void updateUIFromSoundManager() {
        masterVolumeSlider.setValue(soundManager.getMasterVolume());
        masterVolumeLabel.setText(percentFormat.format(soundManager.getMasterVolume()));

        musicVolumeSlider.setValue(soundManager.getMusicVolume());
        musicVolumeLabel.setText(percentFormat.format(soundManager.getMusicVolume()));

        effectsVolumeSlider.setValue(soundManager.getEffectsVolume());
        effectsVolumeLabel.setText(percentFormat.format(soundManager.getEffectsVolume()));

        muteCheckBox.setSelected(soundManager.isGlobalMuted());
        masterVolumeSlider.setDisable(soundManager.isGlobalMuted());
        musicVolumeSlider.setDisable(soundManager.isGlobalMuted());
        effectsVolumeSlider.setDisable(soundManager.isGlobalMuted());
    }

    @FXML
    private void handleCloseButtonAction() {
        // Get the current stage (the Options window)
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (stage != null) {
            stage.close(); // Close the modal window
        }
    }
}