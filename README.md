# Dungeon Eclipse

A JavaFX-based action-adventure dungeon crawler game featuring procedurally generated dungeons, combat, puzzles, and boss fights.

## Requirements
- Java Development Kit (JDK) 23
- JavaFX SDK 23
- Maven

## Building and Running
1. Clone the repository
2. Navigate to the project directory
3. Build the project:
```bash
mvn clean package
```
4. Run the game:
```bash
mvn javafx:run
```

## Game Features
- Procedurally generated dungeons with level up progression
- Real-time combat system
- Inventory management
- Puzzle-solving mechanics with hinting bot
- Treasure room with items and interesting features
- Weather system that changes automatically with attribute changes of player and enemies depending on weather.
- Boss battles
- Dynamic health and score system
- Extra chat system using gemini API
- Retro-inspired graphics and controllable sound effects

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── dungeon/
│   │           ├── controllers/    # UI controllers
│   │           ├── model/         # Game logic and entities
│   │           ├── utils/         # Utility classes
│   │           └── Main.java      # Application entry point
│   └── resources/
│       └── com/
│           └── dungeon/
│               ├── assets/        # Game assets (sprites, sounds)
│               ├── fxml/         # FXML layout files
│               └── styles/       # CSS style sheets
```

## Controls
- WASD: Move character
- E or Mouse click: Attack
- F: Interact
- I: Open inventory
- V: Toggle torch
- ctrl+W: Manual weather change
- ESC: Pause menu
