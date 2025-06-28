package com.dungeon.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, MediaPlayer> soundEffects;
    private MediaPlayer backgroundMusic;
    private boolean isGlobalMuted;

    private double masterVolume;
    private double musicVolume;
    private double effectsVolume;

    private MediaPlayer runningSound;
    private final ExecutorService soundExecutor;

    private static final String PREF_MASTER_VOL = "masterVolume";
    private static final String PREF_MUSIC_VOL = "musicVolume";
    private static final String PREF_EFFECTS_VOL = "effectsVolume";
    private static final String PREF_MUTED = "isGlobalMuted";

    private SoundManager() {
        soundEffects = new HashMap<>();
        soundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SoundThread");
            t.setDaemon(true); // Allows JVM to exit without waiting for this thread
            return t;
        });
        loadSoundSettings();
        loadSounds();
        // Apply initial mute state after sounds are loaded
        applyMuteState();
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void loadSounds() {
        try {
            String musicPath = getClass().getResource("/com/dungeon/assets/sounds/game-music-loop-3-144252.mp3").toExternalForm();
            Media musicMedia = new Media(musicPath);
            backgroundMusic = new MediaPlayer(musicMedia);
            backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
            updateBackgroundMusicVolume();
        } catch (Exception e) {
            System.err.println("Error loading background music: " + e.getMessage());
            backgroundMusic = null;
        }

        loadSoundEffect("teleport", "game-teleport-90735.mp3");
        loadSoundEffect("running", "running-video-game-330951.mp3");
        loadSoundEffect("fail", "game-fail-90322.mp3");
        loadSoundEffect("character", "game-character-140506.mp3");
        loadSoundEffect("start", "game-start-317318.mp3");
        loadSoundEffect("gameOver", "game-over-160612.mp3");
        loadSoundEffect("victory", "victory.mp3");
        loadSoundEffect("damage", "damage.mp3");
        // Add new sounds here if needed, for example:
        // loadSoundEffect("puzzle_solved", "level-passed-86288.mp3");


        runningSound = soundEffects.get("running");
        if (runningSound != null) {
            runningSound.setCycleCount(MediaPlayer.INDEFINITE);
        }
    }

    private void loadSoundEffect(String name, String filename) {
        try {
            String path = getClass().getResource("/com/dungeon/assets/sounds/" + filename).toExternalForm();
            if (path == null) {
                 System.err.println("Sound file not found: " + filename);
                 return;
            }
            Media media = new Media(path);
            MediaPlayer player = new MediaPlayer(media);
            soundEffects.put(name, player);
            updateSoundEffectVolume(player); // Apply initial volume
        } catch (Exception e) {
            System.err.println("Error loading sound effect '" + name + "' ("+filename+"): " + e.getMessage());
        }
    }
    
    private void applyMuteState() {
        if (isGlobalMuted) {
            muteAllInternal(true, false); // Mute without pausing music initially
        } else {
            updateAllVolumes(); // Ensure volumes are set correctly if not muted
        }
    }

    private void updateBackgroundMusicVolume() {
        if (backgroundMusic != null) {
            if (isGlobalMuted) {
                backgroundMusic.setVolume(0);
            } else {
                backgroundMusic.setVolume(clampVolume(masterVolume * musicVolume));
            }
        }
    }

    private void updateEffectsVolume() {
        for (MediaPlayer player : soundEffects.values()) {
            if (player != null) {
                updateSoundEffectVolume(player);
            }
        }
    }

    private void updateSoundEffectVolume(MediaPlayer player) {
        if (player != null) {
            if (isGlobalMuted) {
                player.setVolume(0);
            } else {
                player.setVolume(clampVolume(masterVolume * effectsVolume));
            }
        }
    }

    public void playBackgroundMusic() {
        if (isGlobalMuted || backgroundMusic == null) return;

        soundExecutor.submit(() -> {
            System.out.println("playBackgroundMusic running on thread: " + Thread.currentThread().getName());
            updateBackgroundMusicVolume();
            if (backgroundMusic.getStatus() != MediaPlayer.Status.PLAYING) {
                if (backgroundMusic.getStatus() == MediaPlayer.Status.STOPPED ||
                    backgroundMusic.getStatus() == MediaPlayer.Status.READY ||
                    // If it was paused by mute, then stopped by game, it might be at end
                    backgroundMusic.getCurrentTime().equals(backgroundMusic.getStopTime())) {
                    backgroundMusic.seek(javafx.util.Duration.ZERO);
                }
                backgroundMusic.play();
                System.out.println("Background music playing. Actual volume: " + backgroundMusic.getVolume());
            }
        });
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            soundExecutor.submit(backgroundMusic::stop);
            System.out.println("Background music stopped.");
        }
    }

    public void playSound(String soundName) {
        if (isGlobalMuted) return;
        soundExecutor.submit(() -> {
            MediaPlayer player = soundEffects.get(soundName);
            if (player != null) {
                updateSoundEffectVolume(player);
                if (soundName.equals("running")) {
                    if (runningSound != null && runningSound.getStatus() != MediaPlayer.Status.PLAYING) {
                        runningSound.play();
                    }
                    return;
                }
                // --- Fix for rapid-fire sounds like 'damage' ---
                if (soundName.equals("damage")) {
                    // Create a new MediaPlayer for each play
                    String path = getClass().getResource("/com/dungeon/assets/sounds/damage.mp3").toExternalForm();
                    Media media = new Media(path);
                    MediaPlayer tempPlayer = new MediaPlayer(media);
                    updateSoundEffectVolume(tempPlayer);
                    tempPlayer.setOnEndOfMedia(tempPlayer::dispose);
                    tempPlayer.play();
                    return;
                }
                // --- End fix ---
                player.stop();
                player.seek(javafx.util.Duration.ZERO);
                player.play();
            } else {
                System.err.println("Attempted to play unknown sound: " + soundName);
            }
        });
    }

    public void playVictoryMusic() {
        if (isGlobalMuted) return;
        soundExecutor.submit(() -> {
            System.out.println("Playing victory music on thread: " + Thread.currentThread().getName());
            MediaPlayer victoryPlayer = soundEffects.get("victory");
            if (victoryPlayer != null) {
                // Stop background music first
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                }
                
                // Set victory music volume using music volume setting
                if (isGlobalMuted) {
                    victoryPlayer.setVolume(0);
                } else {
                    victoryPlayer.setVolume(clampVolume(masterVolume * musicVolume));
                }
                
                victoryPlayer.stop();
                victoryPlayer.seek(javafx.util.Duration.ZERO);
                victoryPlayer.play();
                System.out.println("Victory music playing. Volume: " + victoryPlayer.getVolume());
            } else {
                System.err.println("Victory music not found!");
            }
        });
    }

    public void stopSound(String soundName) {
        soundExecutor.submit(() -> {
            MediaPlayer player = soundEffects.get(soundName);
            if (player != null) {
                player.stop();
            }
        });
    }

    public void setMasterVolume(double volume) {
        this.masterVolume = clampVolume(volume);
        updateAllVolumes();
        saveSoundSettings();
    }

    public double getMasterVolume() {
        return masterVolume;
    }

    public void setMusicVolume(double volume) {
        this.musicVolume = clampVolume(volume);
        updateBackgroundMusicVolume();
        saveSoundSettings();
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setEffectsVolume(double volume) {
        this.effectsVolume = clampVolume(volume);
        updateEffectsVolume();
        saveSoundSettings();
    }

    public double getEffectsVolume() {
        return effectsVolume;
    }

    private double clampVolume(double volume) {
        return Math.max(0.0, Math.min(1.0, volume));
    }

    private void updateAllVolumes() {
        updateBackgroundMusicVolume();
        updateEffectsVolume();
    }
    
    private void muteAllInternal(boolean mute, boolean affectMusicPlayback) {
        if (mute) {
            if (backgroundMusic != null) {
                backgroundMusic.setVolume(0);
                if (affectMusicPlayback && backgroundMusic.getStatus() == MediaPlayer.Status.PLAYING) {
                    backgroundMusic.pause();
                }
            }
            for (MediaPlayer player : soundEffects.values()) {
                if (player != null) player.setVolume(0);
            }
        } else {
            updateAllVolumes(); // This restores volumes based on settings
            if (affectMusicPlayback && backgroundMusic != null && backgroundMusic.getStatus() == MediaPlayer.Status.PAUSED) {
                 // Only play if it was paused by the mute function,
                 // and master/music volume is > 0
                if (masterVolume * musicVolume > 0) {
                    backgroundMusic.play();
                }
            }
        }
    }

    public void toggleGlobalMute() {
        isGlobalMuted = !isGlobalMuted;
        muteAllInternal(isGlobalMuted, true); // true to affect music playback (pause/play)
        saveSoundSettings();
    }

    public boolean isGlobalMuted() {
        return isGlobalMuted;
    }

    public void stopAllSounds() {
        System.out.println("Stopping all sounds via stopAllSounds()");
        soundExecutor.submit(() -> {
            if (backgroundMusic != null) {
                backgroundMusic.stop();
            }
            for (MediaPlayer player : soundEffects.values()) {
                if (player != null) {
                    player.stop();
                }
            }
        });
    }

    private void saveSoundSettings() {
        Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);
        prefs.putDouble(PREF_MASTER_VOL, masterVolume);
        prefs.putDouble(PREF_MUSIC_VOL, musicVolume);
        prefs.putDouble(PREF_EFFECTS_VOL, effectsVolume);
        prefs.putBoolean(PREF_MUTED, isGlobalMuted);
        try {
            prefs.flush();
        } catch (java.util.prefs.BackingStoreException e) {
            System.err.println("Error saving sound preferences: " + e.getMessage());
        }
    }

    private void loadSoundSettings() {
        Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);
        masterVolume = clampVolume(prefs.getDouble(PREF_MASTER_VOL, 0.75));
        musicVolume = clampVolume(prefs.getDouble(PREF_MUSIC_VOL, 0.75));
        effectsVolume = clampVolume(prefs.getDouble(PREF_EFFECTS_VOL, 0.75));
        isGlobalMuted = prefs.getBoolean(PREF_MUTED, false);
        System.out.println("Loaded sound settings: Master=" + masterVolume + ", Music=" + musicVolume + ", Effects=" + effectsVolume + ", Muted=" + isGlobalMuted);
    }

    public void shutdown() {
        System.out.println("Shutting down SoundManager.");
        stopAllSounds();
        soundExecutor.shutdownNow();
    }
}
