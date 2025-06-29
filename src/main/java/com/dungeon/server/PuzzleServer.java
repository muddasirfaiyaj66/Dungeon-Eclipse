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
import java.util.concurrent.atomic.AtomicBoolean;

import com.dungeon.model.Puzzle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PuzzleServer {
    private static ServerSocket serverSocket;
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void start() throws IOException {
        serverSocket = new ServerSocket(9999);
        System.out.println("✅ Puzzle server running on port 9999...");

        while (running.get()) {
            try {
            Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("❌ Puzzle server error: " + e.getMessage());
                }
                break;
            }
        }
    }

    public static void shutdown() {
        System.out.println("🛑 Shutting down Puzzle server...");
        running.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("✅ Puzzle server shutdown complete");
            } catch (IOException e) {
                System.err.println("❌ Error closing puzzle server: " + e.getMessage());
            }
        }
    }

    // Test method to check if server is accessible
    public static boolean testConnection() {
        try (Socket socket = new Socket("localhost", 9999)) {
            System.out.println("✅ Puzzle server connection test successful!");
            return true;
        } catch (IOException e) {
            System.err.println("❌ Puzzle server connection test failed: " + e.getMessage());
            return false;
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String input = in.readLine();
            System.out.println("🔍 Puzzle server received: '" + input + "'");
            
            // Reload puzzles from JSON on each request to pick up changes
            List<Puzzle> puzzles = loadPuzzlesFromJSON();
            System.out.println("📚 Loaded " + puzzles.size() + " puzzles from JSON");
            
            // Show first few puzzles for debugging
            for (int i = 0; i < Math.min(3, puzzles.size()); i++) {
                Puzzle p = puzzles.get(i);
                System.out.println("  Puzzle " + (i+1) + ": Q='" + p.getQuestion() + "' A='" + p.getAnswer() + "'");
            }
            
            for (Puzzle puzzle : puzzles) {
                String lowerInput = input.toLowerCase();
                // If user asks 'why' or 'how' and mentions the answer, provide explanation
                if ((lowerInput.contains("why") || lowerInput.contains("how")) && lowerInput.contains(puzzle.getAnswer().toLowerCase())) {
                    System.out.println("✅ Explanation requested for answer!");
                    out.write("Explanation: " + puzzle.getExplanation() + "\n");
                    out.flush();
                    return;
                }
            }
            // If user just says 'why' or 'how', prompt for clarification
            String trimmedInput = input.trim().toLowerCase();
            if (trimmedInput.equals("why") || trimmedInput.equals("how")) {
                out.write("Please specify which answer you want explained, e.g., 'why is the answer keyboard?'\n");
                out.flush();
                return;
            }
            for (Puzzle puzzle : puzzles) {
                String lowerInput = input.toLowerCase();
                // Check for exact match with question
                if (input.equalsIgnoreCase(puzzle.getQuestion())) {
                    System.out.println("✅ Exact question match found!");
                    out.write("Hint: " + puzzle.getHint() + "\n");
                    out.flush();
                    return;
                }
                // Check for exact match with answer
                else if (input.equalsIgnoreCase(puzzle.getAnswer())) {
                    System.out.println("✅ Exact answer match found!");
                    out.write("🎉 Congratulations! You solved the puzzle!\n");
                    out.flush();
                    return;
                }
                // Check for partial match with question (contains)
                else if (puzzle.getQuestion().toLowerCase().contains(lowerInput) || 
                         lowerInput.contains(puzzle.getQuestion().toLowerCase())) {
                    System.out.println("✅ Partial question match found!");
                    out.write("Hint: " + puzzle.getHint() + "\n");
                    out.flush();
                    return;
                }
                // Check for partial match with answer (contains)
                else if (puzzle.getAnswer().toLowerCase().contains(lowerInput) || 
                         lowerInput.contains(puzzle.getAnswer().toLowerCase())) {
                    System.out.println("✅ Partial answer match found!");
                    out.write("🎉 Congratulations! You solved the puzzle!\n");
                    out.flush();
                    return;
                }
            }
            
            System.out.println("❌ No match found for: '" + input + "'");
            out.write("❌ Not recognized. Please ask a valid puzzle or submit an answer.\n");
            out.flush();
        } catch (IOException e) {
            if (running.get()) {
            e.printStackTrace();
            }
        }
    }

    private static List<Puzzle> loadPuzzlesFromJSON() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = PuzzleServer.class.getResourceAsStream("/puzzles.json");
        if (inputStream == null) {
            System.err.println("❌ Could not find puzzles.json resource!");
            return List.of();
        }
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }
}
