package com.dungeon.effects.messaging;

public class PromptBuilder {
    public static String buildHintPrompt(String userQuestion) {
        return """
               You are a helpful in-game assistant in *Dungeon Eclipse*.

               The player asked:
               \"%s\"

               Your task:
               • Reply with a short, precise, and clear hint (1-2 sentences max).
               • Avoid unnecessary words or long explanations.
               • Give only the most relevant clue for the puzzle or question.
               • Never give away full solutions.
               • Never mention you are an AI or explain technical things.

               Example:
               Instead of "You should try to solve the puzzle by pressing the buttons in this exact order," say:
               "Check the symbols on the wall—they reveal the order."

               Write your concise hint below.
               """.formatted(userQuestion);
    }
}
