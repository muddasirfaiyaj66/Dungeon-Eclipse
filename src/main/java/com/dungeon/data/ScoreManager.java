package com.dungeon.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ScoreManager {

    private static final String SCORES_FILE = "highscores.dat";
    private static final int MAX_SCORES = 10; // Maximum number of scores to keep

    // Inner class to represent a score entry
    private static class ScoreEntry implements Serializable, Comparable<ScoreEntry> {
        private static final long serialVersionUID = 1L;
        String playerName;
        int score;
        String date;

        public ScoreEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
            this.date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }

        @Override
        public int compareTo(ScoreEntry other) {
            return Integer.compare(other.score, this.score); // Sort descending by score
        }

        @Override
        public String toString() {
            return String.format("%s - %d (%s)", playerName, score, date);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<String> loadScores() {
        List<String> scoreStrings = new ArrayList<>();
        if (!Files.exists(Paths.get(SCORES_FILE))) {
            return scoreStrings; // Return empty list if file doesn't exist
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SCORES_FILE))) {
            List<ScoreEntry> entries = (List<ScoreEntry>) ois.readObject();
            for (ScoreEntry entry : entries) {
                scoreStrings.add(entry.toString());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading scores: " + e.getMessage());
            // e.printStackTrace(); // Optionally print stack trace for debugging
            // If file is corrupted or old format, can decide to delete or backup
            // For now, we just return an empty list or whatever was loaded before error
        }
        return scoreStrings;
    }

    @SuppressWarnings("unchecked")
    public static synchronized void saveScore(String playerName, int score) {
        List<ScoreEntry> entries = new ArrayList<>();
        if (Files.exists(Paths.get(SCORES_FILE))) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SCORES_FILE))) {
                entries = (List<ScoreEntry>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error reading existing scores, starting fresh: " + e.getMessage());
                // If there's an error reading, we might be starting with a new list
            }
        }

        entries.add(new ScoreEntry(playerName, score));
        Collections.sort(entries); // Sorts descending by score

        // Keep only the top MAX_SCORES
        if (entries.size() > MAX_SCORES) {
            entries = new ArrayList<>(entries.subList(0, MAX_SCORES));
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORES_FILE))) {
            oos.writeObject(entries);
        } catch (IOException e) {
            System.err.println("Error saving scores: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 