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
  - Kruskal's Algorithm (MST-based generation)
  - Prim's Algorithm (Randomized frontier selection)

- 🧭 **Advanced Solving Algorithms**
  - Depth-First Search (Recursive exploration)
  - Breadth-First Search (Guaranteed shortest path)
  - A* Search (Heuristic-based optimal pathfinding)

- ⏯️ **Pause/Resume Functionality**
  - Pause any running operation (generation or solving) at any time
  - Resume exactly from the paused state
  - Independent pause/resume controls for different operations
  - Real-time status updates during pause/resume cycles

- 🎨 **Rich Visualization & User Experience**
  - Real-time generation and solving animation
  - Customizable animation speed with persistence
  - Mouse wheel zooming with zoom level persistence
  - Color-coded cell states for clear visualization
  - Modern responsive UI with progress feedback
  - Window size and position persistence

- 🎛️ **Smart Reset Controls**
  - Context-aware reset button behavior
  - **Generated maze**: Reset clears solution only (preserves maze structure)
  - **Blank/partial maze**: Reset clears to completely blank state
  - **Generation paused**: Reset stops and clears to blank (fresh start)
  - **Solving paused**: Reset clears solution only (preserves maze)

- 💾 **Advanced File Operations**
  - Save/Load maze states (versioned text format)
  - Export mazes as high-quality PNG images
  - File dialogs with format selection
  - Comprehensive error handling

- 🎛️ **Enhanced User Controls**
  - Customizable maze dimensions (5x5 to 200x200)
  - Algorithm selection with dropdown menus
  - Reproducible generation seed with randomize control
  - Smart operation controls (Generate/Solve/Pause/Reset)
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
4. **Set or Randomize Seed**: Keep the displayed seed to recreate a maze later
5. **Generate Maze**: Click "Generate" and watch real-time creation
6. **Pause/Resume**: Use "Pause" button to pause generation at any time
7. **Select Solving Algorithm**: Choose pathfinding method
8. **Solve Maze**: Click "Solve" to watch the algorithm work
9. **Adjust Settings**: Use speed slider and zoom controls
10. **Save/Export**: Preserve your maze or export as image

### Advanced Controls

- **Mouse Wheel**: Zoom in/out (zoom level persists)
- **Seed Field**: Reuse the same seed, generation algorithm, and dimensions to recreate a maze
- **Create Seed Button**: Creates a fresh maze from the entered seed and current dimensions
- **Pause/Resume Button**: Pause any operation and resume from exact same state
- **Reset Button**: Context-aware reset (clears solution or resets to blank)
- **New Maze Button**: Creates entirely new maze with current dimensions
- **Speed Slider**: Real-time animation speed control (persists)
- **Window Management**: Resize, move, maximize - all settings persist

### Smart Reset Button Behavior

| Maze State | Reset Action |
| ------------ | -------------- |
| **Idle with generated maze** | Clears solution only (preserves maze structure) |
| **Idle with blank maze** | No change (already blank) |
| **Generation paused** | Stops generation → resets to blank (fresh start) |
| **Solving paused** | Clears solution only (preserves generated maze) |
| **Generation running** | Button disabled (must pause first) |
| **Solving running** | Button disabled (must pause first) |

### Pause/Resume Functionality

- **During Generation**: Pause mid-generation, resume exactly where left off
- **During Solving**: Pause mid-solving, resume with same exploration state
- **Real-time Response**: Operations pause immediately when button clicked
- **State Preservation**: All progress maintained during pause
- **Status Updates**: Clear feedback on current operation state

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
- **State Pattern**: Pause/resume state management
- **Interface Segregation**: Clean contracts between components
- **Dependency Injection**: Loose coupling through constructor injection
- **Command Pattern**: Encapsulated user actions and operations

### Key Architectural Benefits

- **Maintainability**: Single responsibility principle throughout
- **Extensibility**: Easy to add new algorithms without changing existing code
- **Testability**: Interface-based design enables comprehensive unit testing
- **Performance**: Optimized rendering with viewport clipping
- **Thread Safety**: Proper concurrent operation handling with pause support
- **User Experience**: Responsive UI with non-blocking operations

## 🧮 Algorithms

### Generation Algorithms

#### Depth-First Search (Recursive Backtracking)

- **Time Complexity**: O(n) where n is number of cells
- **Space Complexity**: O(n) for recursion stack
- **Characteristics**: Creates mazes with long, winding corridors
- **Implementation**: Randomized wall removal with connectivity tracking

#### Kruskal's Algorithm

- **Time Complexity**: O(E log E) where E is number of edges
- **Space Complexity**: O(V) for Union-Find structure
- **Characteristics**: Creates mazes with more branching paths
- **Implementation**: Minimum spanning tree approach with Union-Find

#### Prim's Algorithm

- **Time Complexity**: O(E log V) where V=vertices, E=edges
- **Space Complexity**: O(V) for frontier management
- **Characteristics**: Creates mazes with organic, tree-like structure
- **Implementation**: Randomized frontier selection and wall removal

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
    public void generate(Maze maze, MazeGenerationContext context) {
        // Your algorithm implementation
        // Use context.notifyCellChanged() for real-time updates
        // Use context.isStopped() for cancellation
        // Use context.pauseAwareSleep() for pause/resume-aware animation
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
    public boolean solve(Maze maze, MazeSolvingContext context) {
        // Your pathfinding implementation
        // Return true if solution found
        // Use context.pauseAwareSleep() for pause/resume-aware animation
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
./gradlew test
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
- **Pause-Aware Operations**: Minimal overhead during pause states
- **Memory Management**: Defensive copying and garbage collection friendly
- **JVM Tuning**: Optimized JVM arguments for Swing applications

## 🔧 Advanced Features

### Pause/Resume Implementation

- **Thread-Safe**: Uses AtomicBoolean flags for thread-safe pause control
- **State Preservation**: Maintains complete algorithm state during pause
- **Responsive**: Immediate pause response with 100ms check intervals
- **Memory Efficient**: No additional memory overhead during pause

### Context-Aware Reset

- **Intelligent Behavior**: Different reset actions based on current maze state
- **User-Friendly**: Prevents accidental loss of generated mazes
- **Status Feedback**: Clear messages indicating reset action taken

### Window State Persistence

- **Complete State**: Saves position, size, maximized state, zoom level
- **Cross-Session**: Settings preserved between application restarts
- **User Preferences**: Animation speed and UI preferences maintained

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Algorithms**: Classic computer science maze generation and pathfinding algorithms
- **Architecture**: Clean architecture principles and Gang of Four design patterns
- **UI Framework**: Java Swing for robust cross-platform desktop applications
- **Build System**: Gradle for modern dependency management and build automation
- **Testing**: JUnit 5 ecosystem for comprehensive test coverage
- **Concurrency**: Java threading model for responsive pause/resume functionality

## 🔗 Links

- **Documentation**: [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- **Java**: [OpenJDK 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- **Swing**: [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)
- **Threading**: [Java Concurrency Guide](https://docs.oracle.com/javase/tutorial/essential/concurrency/)

---

## 🆕 Recent Updates

### Version 2.0.0 - Pause/Resume & Smart Controls

- ✅ **Pause/Resume Functionality**: Pause any operation and resume from exact state
- ✅ **Context-Aware Reset**: Smart reset behavior based on maze state
- ✅ **All Generation Algorithms**: DFS, Kruskal's, and Prim's algorithms implemented
- ✅ **Enhanced UI**: Improved button states and user feedback
- ✅ **Code Quality**: Refactored for reduced cognitive complexity
- ✅ **Thread Safety**: Robust concurrent operation handling
