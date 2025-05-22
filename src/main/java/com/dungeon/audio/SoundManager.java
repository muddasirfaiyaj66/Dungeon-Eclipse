package com.dungeon.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, MediaPlayer> soundEffects;
    private MediaPlayer backgroundMusic;
    private boolean isMuted;
    private double volume;
    private MediaPlayer runningSound; // Special handling for running sound

    private SoundManager() {
        soundEffects = new HashMap<>();
        isMuted = false;
        volume = 0.5; // Default volume at 50%
        loadSounds();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void loadSounds() {
        // Load background music
        String musicPath = getClass().getResource("/com/dungeon/assets/sounds/game-music-loop-3-144252.mp3").toExternalForm();
        Media musicMedia = new Media(musicPath);
        backgroundMusic = new MediaPlayer(musicMedia);
        backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundMusic.setVolume(volume);

        // Load sound effects
        loadSoundEffect("teleport", "game-teleport-90735.mp3");
        loadSoundEffect("running", "running-video-game-330951.mp3");
        loadSoundEffect("fail", "game-fail-90322.mp3");
        loadSoundEffect("character", "game-character-140506.mp3");
        loadSoundEffect("start", "game-start-317318.mp3");
        loadSoundEffect("gameOver", "game-over-160612.mp3");

        // Special setup for running sound
        runningSound = soundEffects.get("running");
        if (runningSound != null) {
            runningSound.setCycleCount(MediaPlayer.INDEFINITE);
        }
    }

    private void loadSoundEffect(String name, String filename) {
        String path = getClass().getResource("/com/dungeon/assets/sounds/" + filename).toExternalForm();
        Media media = new Media(path);
        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(volume);
        soundEffects.put(name, player);
    }

    public void playBackgroundMusic() {
        if (!isMuted && backgroundMusic != null) {
            // First check and reset if needed
            if (backgroundMusic.getStatus() == MediaPlayer.Status.STOPPED) {
                backgroundMusic.stop(); // Ensure fully stopped
                backgroundMusic.seek(javafx.util.Duration.ZERO); // Reset to beginning
            }
            
            // Then play
            backgroundMusic.play();
            
            // Log the action for debugging
            System.out.println("Background music started: " + 
                (backgroundMusic.getStatus() == MediaPlayer.Status.PLAYING ? "PLAYING" : "NOT PLAYING"));
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    public void playSound(String soundName) {
        if (!isMuted && soundEffects.containsKey(soundName)) {
            MediaPlayer player = soundEffects.get(soundName);
            
            // Special handling for running sound
            if (soundName.equals("running")) {
                if (runningSound != null && runningSound.getStatus() != MediaPlayer.Status.PLAYING) {
                    runningSound.play();
                }
                return;
            }
            
            // For other sounds, stop and play
            player.stop();
            player.play();
        }
    }

    public void stopSound(String soundName) {
        if (soundEffects.containsKey(soundName)) {
            MediaPlayer player = soundEffects.get(soundName);
            player.stop();
        }
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(this.volume);
        }
        for (MediaPlayer player : soundEffects.values()) {
            player.setVolume(this.volume);
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            stopBackgroundMusic();
            // Stop all sound effects
            for (MediaPlayer player : soundEffects.values()) {
                player.stop();
            }
        } else {
            playBackgroundMusic();
        }
    }

    public void stopAllSounds() {
        System.out.println("Stopping all sounds");
        // Stop all sound effects
        for (MediaPlayer player : soundEffects.values()) {
            if (player != null) {
                player.stop();
            }
        }
        
        // Also stop background music if it's playing
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public double getVolume() {
        return volume;
    }
} 