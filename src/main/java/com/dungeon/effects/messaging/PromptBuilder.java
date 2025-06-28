package com.dungeon.effects.messaging;

import com.dungeon.model.Puzzle;

public class PromptBuilder {

    public static String buildHintPrompt(Puzzle puzzle, String userAttempt) {
        String baseHint = (puzzle.getHint() != null && !puzzle.getHint().isBlank())
                ? puzzle.getHint()
                : "Think carefully and logically about the puzzle.";
        String question = puzzle.getQuestion();
        String correctAnswer = puzzle.getAnswer();

        String additionalClue = "";
        if (userAttempt != null && !userAttempt.isBlank()) {
            String cleanedAttempt = userAttempt.trim().toLowerCase();
            String cleanedAnswer = correctAnswer.trim().toLowerCase();

            if (cleanedAttempt.equals(cleanedAnswer)) {
                additionalClue = " (Actually… you got it right! Try submitting your answer.)";
            } else if (cleanedAnswer.contains(cleanedAttempt) || cleanedAttempt.contains(cleanedAnswer)) {
                additionalClue = " You're very close — check spelling or logic.";
            } else if (cleanedAnswer.length() == cleanedAttempt.length()) {
                additionalClue = " Same length — you’re not far off.";
            } else {
                additionalClue = " Think deeper — your guess isn't quite on the right track.";
            }
        }

        return """
               You are a helpful in-game assistant in *Dungeon Eclipse*.

               The player encountered this puzzle:
               \"%s\"

               Your task:
               • Reply with a short, precise, and clear hint.
               • Never give the full answer.
               • If the user gave an attempt, encourage or redirect them without revealing the solution.

               Hint:
               %s%s
               """.formatted(question, baseHint, additionalClue);
    }

    public static String buildConversationResponse(String userMessage) {
        String lowercase = userMessage.toLowerCase();
        if (lowercase.contains("hello") || lowercase.contains("hi")) {
            return "Hey there, adventurer! Need help with a puzzle or just want to chat?";
        } else if (lowercase.contains("help") || lowercase.contains("stuck")) {
            return "I'm here to help. Ask me about any puzzle you're struggling with, and I’ll give you a hint.";
        } else if (lowercase.contains("answer")) {
            return "Nice try! I can't reveal full answers. Just a nudge in the right direction 😉.";
        } else {
            return "Let me know if you’re facing a puzzle. I’ll give you a helpful hint!";
        }
    }
}
