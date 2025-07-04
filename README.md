# Maze Master - Definitive Edition

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-blue.svg)](https://gradle.org/)
[![Swing](https://img.shields.io/badge/UI-Swing-green.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

> A sophisticated Java application for generating and solving mazes using multiple algorithms with real-time visualization and advanced features.

![Maze Master Demo](demo.gif) <!-- Add a demo gif when ready -->

## ✨ Features

- 🏗️ **Multiple Generation Algorithms**
  - Depth-First Search (Recursive Backtracking)
  - Kruskal's Algorithm (Coming Soon)
  - Prim's Algorithm (Coming Soon)

- 🧭 **Advanced Solving Algorithms**
  - Depth-First Search (Recursive exploration)
  - Breadth-First Search (Guaranteed shortest path)
  - A* Search (Heuristic-based optimal pathfinding)

- 🎨 **Rich Visualization & User Experience**
  - Real-time generation and solving animation
  - Customizable animation speed with persistence
  - Mouse wheel zooming with zoom level persistence
  - Color-coded cell states for clear visualization
  - Modern responsive UI with progress feedback
  - Window size and position persistence

- 💾 **Advanced File Operations**
  - Save/Load maze states (serialized format)
  - Export mazes as high-quality PNG images
  - File dialogs with format selection
  - Comprehensive error handling

- 🎛️ **Enhanced User Controls**
  - Customizable maze dimensions (5x5 to 200x200)
  - Algorithm selection with dropdown menus
  - Smart Start/Stop/Reset operations
  - Real-time status updates and feedback
  - Keyboard shortcuts and tooltips

- ⚙️ **Persistent Settings**
  - Window size, position, and maximized state
  - Maze zoom level
  - Animation speed preferences
  - All settings automatically restored on startup

## 🚀 Quick Start

### Prerequisites

- **Java 17 or higher** (tested with OpenJDK 17+)
- **Gradle** (included via wrapper - no installation required)
- Any Java-compatible operating system

### Building and Running with Gradle

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/maze-master.git
   cd maze-master
   ```

2. **Build the project**

   ```bash
   # Build everything (compile, test, create JAR)
   ./gradlew build
   
   # On Windows
   gradlew.bat build
   ```

3. **Run the application**

   ```bash
   # Run with optimized JVM settings (recommended)
   ./gradlew runApp
   
   # Or run with standard settings
   ./gradlew run
   
   # Or run the generated JAR directly
   java -jar build/libs/maze-master-2.0.0.jar
   ```

### Gradle Tasks

```bash
# Development
./gradlew clean build          # Clean and build everything
./gradlew runApp              # Run with JVM optimizations
./gradlew test                # Run unit tests
./gradlew javadoc             # Generate documentation

# Distribution
./gradlew jar                 # Create executable JAR
./gradlew distZip            # Create distribution package
./gradlew installDist        # Install to build/install/

# IDE Integration
./gradlew idea               # Generate IntelliJ IDEA project files
./gradlew eclipse            # Generate Eclipse project files

# Debugging
./gradlew build --info       # Verbose build output
./gradlew dependencies       # Show dependency tree
```

## 🎮 How to Use

### Basic Workflow

1. **Launch Application**: Run via Gradle or JAR file
2. **Set Dimensions**: Enter desired maze size in the side panel
3. **Choose Generation Algorithm**: Select from dropdown (default: DFS)
4. **Generate Maze**: Click "Generate" and watch real-time creation
5. **Select Solving Algorithm**: Choose pathfinding method
6. **Solve Maze**: Click "Solve" to watch the algorithm work
7. **Adjust Settings**: Use speed slider and zoom controls
8. **Save/Export**: Preserve your maze or export as image

### Advanced Controls

- **Mouse Wheel**: Zoom in/out (zoom level persists)
- **Reset Button**: Clears solution while keeping generated maze
- **New Maze Button**: Creates entirely new maze with current dimensions
- **Stop Operation**: Interrupts generation/solving in progress
- **Speed Slider**: Real-time animation speed control (persists)
- **Window Management**: Resize, move, maximize - all settings persist

### Smart Behaviors

- **Persistent Settings**: All preferences automatically saved and restored
- **Intelligent Reset**: Reset clears both exploration path and final solution
- **Responsive UI**: Operations run on background threads
- **Error Handling**: Comprehensive user feedback for all operations

## 🏗️ Architecture

This project demonstrates professional software architecture with clean separation of concerns:

```project structure
src/main/java/com/mazemaster/
├── MazeMasterApplication.java     # Application entry point
├── model/
│   └── Maze.java                  # Core maze data structure
├── generation/
│   ├── MazeGenerator.java         # Generation orchestrator
│   ├── MazeGenerationStrategy.java    # Strategy interface
│   └── MazeGenerationListener.java    # Event listener interface
├── solving/
│   ├── MazeSolver.java            # Solving orchestrator
│   ├── MazeSolvingStrategy.java       # Strategy interface
│   └── MazeSolvingListener.java       # Event listener interface
├── controller/
│   └── MazeController.java        # MVC controller layer
└── ui/
    ├── MazeView.java              # View interface contract
    └── swing/
        ├── SwingMazeView.java     # Main UI implementation
        └── MazePanel.java         # Custom maze rendering component
```

### Design Patterns Implemented

- **MVC (Model-View-Controller)**: Clean separation of data, logic, and presentation
- **Strategy Pattern**: Pluggable algorithms for generation and solving
- **Observer Pattern**: Event-driven UI updates and progress notifications
- **Interface Segregation**: Clean contracts between components
- **Dependency Injection**: Loose coupling through constructor injection
- **Command Pattern**: Encapsulated user actions and operations

### Key Architectural Benefits

- **Maintainability**: Single responsibility principle throughout
- **Extensibility**: Easy to add new algorithms without changing existing code
- **Testability**: Interface-based design enables comprehensive unit testing
- **Performance**: Optimized rendering with viewport clipping
- **Thread Safety**: Proper concurrent operation handling

## 🧮 Algorithms

### Generation Algorithms

#### Depth-First Search (Recursive Backtracking)

- **Time Complexity**: O(n) where n is number of cells
- **Space Complexity**: O(n) for recursion stack
- **Characteristics**: Creates mazes with long, winding corridors
- **Implementation**: Randomized wall removal with connectivity tracking
<!-- markdownlint-disable-next-line MD036 -->
*Future algorithms: Kruskal's MST-based generation, Prim's algorithm*

### Solving Algorithms

#### Depth-First Search

- **Time Complexity**: O(V + E) where V=vertices, E=edges
- **Space Complexity**: O(V) for recursion stack
- **Guarantees**: Finds *a* solution, not necessarily optimal
- **Behavior**: Explores deeply before backtracking

#### Breadth-First Search

- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V) for queue storage
- **Guarantees**: Finds shortest path in unweighted graphs
- **Behavior**: Explores level-by-level, highlights optimal path in green

#### A* Search

- **Time Complexity**: O(b^d) where b=branching factor, d=solution depth
- **Space Complexity**: O(b^d) for open set storage
- **Guarantees**: Optimal path with admissible heuristic (Manhattan distance)
- **Behavior**: Guided search using distance heuristic

## 🎨 Customization

### Adding New Generation Algorithms

1. **Implement Strategy Interface**:

```java
public class MyGenerationAlgorithm implements MazeGenerationStrategy {
    @Override
    public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
        // Your algorithm implementation
        // Use listener.onCellChanged() for real-time updates
        // Check stopFlag.get() for cancellation
    }
}
```
<!-- markdownlint-disable-next-line MD029 -->
2. **Register Algorithm**:

```java
// In MazeGenerator constructor
strategies.put("My Algorithm", new MyGenerationAlgorithm());
```

### Adding New Solving Algorithms

1. **Implement Strategy Interface**:

```java
public class MySolvingAlgorithm implements MazeSolvingStrategy {
    @Override
    public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
        // Your pathfinding implementation
        // Return true if solution found
    }
}
```
<!-- markdownlint-disable-next-line MD029 -->
2. **Register Algorithm**:

```java
// In MazeSolver constructor
strategies.put("My Solver", new MySolvingAlgorithm());
```

### Customizing Visual Themes

Modify colors in `SwingMazeView`:

```java
private final Color[] mazeColors = {
    new Color(240, 240, 240), // Background
    new Color(60, 60, 60),    // Wall - dark gray
    new Color(100, 150, 255), // Path - blue
    Color.WHITE,              // Empty - white
    new Color(200, 200, 200), // Visited - light gray
    new Color(50, 200, 50),   // Solution - green
    new Color(255, 100, 100)  // Goal marker - red
};
```

## 🧪 Testing

The project includes comprehensive unit tests using modern testing frameworks:

```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew test --info

# Generate test reports
./gradlew test jacocoTestReport
```

**Testing Stack**:

- **JUnit 5**: Modern testing framework with advanced features
- **Mockito**: Mocking framework for isolated unit tests
- **AssertJ**: Fluent assertion library for readable tests
- **AssertJ Swing**: Specialized testing for Swing components

## 📁 Project Structure

```file structure
maze-master/
├── src/
│   ├── main/
│   │   ├── java/com/mazemaster/     # Source code
│   │   └── resources/               # Configuration files
│   └── test/
│       └── java/com/mazemaster/     # Unit tests
├── build/
│   ├── classes/                     # Compiled bytecode
│   ├── libs/                        # Generated JARs
│   └── reports/                     # Test and documentation reports
├── gradle/
│   └── wrapper/                     # Gradle wrapper files
├── build.gradle                     # Build configuration
├── settings.gradle                  # Project settings
├── gradlew                          # Unix wrapper script
├── gradlew.bat                      # Windows wrapper script
├── README.md                        # This documentation
├── LICENSE                          # MIT license
└── .gitignore                       # Git ignore rules
```

## 🚀 Performance & Scalability

### Performance Metrics

- **Small Mazes** (50×50): Generation <100ms, solving <200ms
- **Medium Mazes** (100×100): Generation <500ms, solving <1s
- **Large Mazes** (200×200): Generation ~2s, solving 1-5s (algorithm dependent)
<!-- markdownlint-disable-next-line MD036 -->
*Benchmarked on: AMD Ryzen 5 5600H, 16GB RAM, Java 17*

### Optimization Features

- **Viewport Clipping**: Only renders visible maze cells
- **Efficient Rendering**: Optimized Graphics2D usage with anti-aliasing
- **Background Threading**: Non-blocking UI during long operations
- **Memory Management**: Defensive copying and garbage collection friendly
- **JVM Tuning**: Optimized JVM arguments for Swing applications

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Algorithms**: Classic computer science maze generation and pathfinding algorithms
- **Architecture**: Clean architecture principles and Gang of Four design patterns
- **UI Framework**: Java Swing for robust cross-platform desktop applications
- **Build System**: Gradle for modern dependency management and build automation
- **Testing**: JUnit 5 ecosystem for comprehensive test coverage

## 🔗 Links

- **Documentation**: [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- **Java**: [OpenJDK 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- **Swing**: [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)

---
