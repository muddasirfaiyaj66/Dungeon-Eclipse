package com.dungeon.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import com.dungeon.model.Puzzle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PuzzleServer {

    public static void start() throws IOException {
        List<Puzzle> puzzles = loadPuzzlesFromJSON();
        ServerSocket serverSocket = new ServerSocket(9999);
        System.out.println("✅ Puzzle server running on port 9999...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket, puzzles)).start();
        }
    }

    private static void handleClient(Socket socket, List<Puzzle> puzzles) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String input = in.readLine();
            for (Puzzle puzzle : puzzles) {
                if (input.equalsIgnoreCase(puzzle.getQuestion())) {
                    out.write("Hint: " + puzzle.getHint() + "\n");
                    out.flush();
                    return;
                } else if (input.equalsIgnoreCase(puzzle.getAnswer())) {
                    out.write("🎉 Congratulations! You solved the puzzle!\n");
                    out.flush();
                    return;
                }
            }
            out.write("❌ Not recognized. Please ask a valid puzzle or submit an answer.\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Puzzle> loadPuzzlesFromJSON() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = PuzzleServer.class.getResourceAsStream("/puzzles.json");
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }
}
