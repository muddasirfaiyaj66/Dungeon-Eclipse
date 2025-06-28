package com.dungeon.utils;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import com.dungeon.model.Puzzle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PuzzleLoader {
    public static List<Puzzle> loadPuzzlesFromJson(String resourcePath) {
        try (InputStreamReader reader = new InputStreamReader(
                PuzzleLoader.class.getClassLoader().getResourceAsStream(resourcePath))) {
            Gson gson = new Gson();
            Type puzzleListType = new TypeToken<List<Puzzle>>() {}.getType();
            return gson.fromJson(reader, puzzleListType);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
