# Maze Master - Definitive Edition

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/UI-Swing-blue.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> A sophisticated Java application for generating and solving mazes using multiple algorithms with real-time visualization.

![Maze Master Demo](demo.gif) <!-- Add a demo gif when ready -->

## ✨ Features

- 🏗️ **Multiple Generation Algorithms**
  - Depth-First Search (Recursive Backtracking)
  - Kruskal's Algorithm (Coming Soon)
  - Prim's Algorithm (Coming Soon)

- 🧭 **Advanced Solving Algorithms**
  - Depth-First Search
  - Breadth-First Search (Shortest Path)
  - A* Search (Heuristic-based)
  - Dijkstra's Algorithm (Coming Soon)

- 🎨 **Rich Visualization**
  - Real-time generation and solving animation
  - Customizable animation speed
  - Mouse wheel zooming
  - Color-coded cell states

- 💾 **File Operations**
  - Save/Load maze states
  - Export mazes as PNG images
  - Persistent settings

- 🎛️ **User Controls**
  - Customizable maze dimensions
  - Algorithm selection
  - Start/Stop operations
  - Progress indicators

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Any Java-compatible operating system

### Building and Running

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/maze-master.git
   cd maze-master
   ```

2. **Build the project**

   ```bash
   # Linux/Mac
   chmod +x build.sh
   ./build.sh
   
   # Windows
   build.bat
   ```

3. **Run the application**

   ```bash
   java -cp build/classes com.mazemaster.MazeMasterApplication
   
   # Or run the JAR (if built)
   java -jar MazeMaster.jar
   ```

### Manual Compilation

```bash
# Create build directory
mkdir -p build/classes

# Compile all sources
find src -name "*.java" | xargs javac -d build/classes

# Run
java -cp build/classes com.mazemaster.MazeMasterApplication
```

## 🎮 How to Use

1. **Set Dimensions**: Enter desired maze size (rows × columns)
2. **Choose Generation Algorithm**: Select from the dropdown menu
3. **Generate**: Click "Generate" to create a new maze
4. **Select Solving Algorithm**: Choose your preferred pathfinding algorithm
5. **Solve**: Click "Solve" to watch the algorithm find the path
6. **Adjust Speed**: Use the slider to control animation speed
7. **Save/Export**: Save your maze or export as an image

### Controls

- **Mouse Wheel**: Zoom in/out on the maze
- **Stop Button**: Interrupt generation or solving
- **Reset**: Clear the current maze
- **New Maze**: Create maze with different dimensions

## 🏗️ Architecture

The project follows clean architecture principles with clear separation of concerns:

```project structure
src/com/mazemaster/
├── model/              # Data structures
├── generation/         # Maze generation algorithms
├── solving/           # Pathfinding algorithms
├── controller/        # Application logic
└── ui/               # User interface
    └── swing/        # Swing implementation
```

### Design Patterns Used

- **MVC (Model-View-Controller)**: Clear separation of concerns
- **Strategy Pattern**: Pluggable algorithms
- **Observer Pattern**: Event-driven updates
- **Interface Segregation**: Clean contracts between components

## 🧮 Algorithms

### Generation Algorithms

#### Depth-First Search (Recursive Backtracking)

- **Time Complexity**: O(n)
- **Space Complexity**: O(n)
- **Characteristics**: Creates mazes with long, winding passages

### Solving Algorithms

#### Depth-First Search

- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V)
- **Guarantees**: Finds *a* solution (not necessarily shortest)

#### Breadth-First Search

- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V)
- **Guarantees**: Finds shortest path

#### A* Search

- **Time Complexity**: O(b^d) where b is branching factor, d is depth
- **Space Complexity**: O(b^d)
- **Guarantees**: Optimal path with admissible heuristic

## 🎨 Customization

### Adding New Algorithms

1. **Generation**: Implement `MazeGenerationStrategy` interface
2. **Solving**: Implement `MazeSolvingStrategy` interface
3. **Register**: Add to the respective algorithm map

```java
// Example: Adding a new generation algorithm
public class MyGenerationAlgorithm implements MazeGenerationStrategy {
    @Override
    public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
        // Your algorithm implementation
    }
}
```

### Color Themes

Modify the color array in `SwingMazeView`:

```java
private final Color[] mazeColors = {
    // Customize these colors
    new Color(240, 240, 240), // Background
    new Color(60, 60, 60),    // Wall
    // ... etc
};
```

## 📁 File Structure

```file structure
maze-master/
├── src/                    # Source code
├── build/                  # Compiled classes
├── build.sh               # Build script (Unix)
├── build.bat              # Build script (Windows)
├── README.md              # This file
├── .gitignore             # Git ignore rules
└── LICENSE                # License file
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by classic maze generation and solving algorithms
- Built with Java Swing for cross-platform compatibility
- Designed with clean architecture principles

## 📊 Performance

- **Small Mazes** (50×50): Near-instant generation and solving
- **Medium Mazes** (100×100): Generation <1s, solving <2s
- **Large Mazes** (200×200): Generation ~3s, solving varies by algorithm

Performance measured on modern hardware (AMD Ryzen 5 5600H, 16GB RAM)
