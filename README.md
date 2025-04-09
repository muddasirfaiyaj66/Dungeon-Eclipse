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
- Procedurally generated dungeons
- Real-time combat system
- Inventory management
- Puzzle-solving mechanics
- Boss battles
- Dynamic health and score system
- Retro-inspired graphics and sound effects

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
- WASD or Arrow Keys: Move character
- Space: Attack
- E: Interact
- I: Open inventory
- ESC: Pause menu
