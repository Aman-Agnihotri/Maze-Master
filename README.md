# Maze Master

[![Java](https://img.shields.io/badge/Language-Java-blue.svg)](https://www.java.com)
[![UI](https://img.shields.io/badge/UI-Swing-orange.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)

**A visually engaging Java Swing application for generating and solving complex mazes using various algorithms.**

<!--`[Insert Screenshot/GIF Here]`-->

---

## Overview

Maze Master is an interactive desktop application built with Java Swing that allows users to:

1. **Generate** random mazes of customizable dimensions using a Depth-First Search (DFS) based algorithm.
2. **Visualize** the maze generation process in real-time.
3. **Solve** the generated mazes using a selection of popular pathfinding algorithms:
    * Depth First Search (DFS)
    * Breadth First Search (BFS)
    * A* Search
4. **Visualize** the solving process step-by-step, highlighting the explored path and the final solution.
5. **Control** the speed of generation and solving visualization.
6. **Save** the current maze state to a file.
7. **Load** a previously saved maze state.
8. **Export** the currently displayed maze as a PNG image.

---

## Features

* **Random Maze Generation:** Creates perfect mazes (no loops) using a randomized DFS algorithm that carves passages through a grid.
* **Customizable Dimensions:** Specify the number of rows and columns for the maze before generation (defaults to 41x51, ensures odd dimensions for proper generation).
* **Multiple Solving Algorithms:** Choose between DFS, BFS, and A* to find a path from the top-left corner to the bottom-right corner.
* **Real-time Visualization:** Watch the maze being built and solved step-by-step. Cell colors indicate:
  * `Red`: Walls
  * `White`: Unexplored passages (Empty)
  * `Light Blue`: Current path being explored during solving (Path)
  * `Gray`: Visited/Discarded cells during solving (Visited)
  * `Green`: Final shortest path (for BFS/A*)
* **Speed Control:** Adjust the visualization speed using a slider for slower debugging or faster results.
* **Interactive Controls:** Simple buttons for Generate, Solve, Reset/Stop, Save, Load, and Export.
* **State Persistence:** Save your favorite or challenging mazes and load them back later (`mazeSave.ser`).
* **Image Export:** Save a snapshot of the current maze (generated or solved) as a PNG image (`maze.png`).
* **Responsive UI:** Generation and solving run on separate threads to keep the UI responsive.
* **Stop Functionality:** Interrupt ongoing maze generation or solving using the "Stop" button (which replaces "Reset" during these operations).

---

## How to Run

1. **Prerequisites:** Make sure you have a Java Development Kit (JDK) installed (version 8 or later recommended).
2. **Compile:** Open a terminal or command prompt, navigate to the directory containing the source files, and compile the code:

    ```bash
    javac MazeApp.java MazePanel.java
    ```

3. **Run:** Execute the compiled application:

    ```bash
    java MazeApp
    ```

    This will launch the Maze Master window.

---

## How to Use

1. **Launch:** Run the application using the steps above.
2. **(Optional) Set Size:** Enter the desired number of rows and columns in the text fields *before* clicking "Generate Maze". Note that odd numbers generally work best for the generation algorithm.
3. **Generate:** Click the "Generate Maze" button. Watch as the maze is carved out.
4. **(Optional) Adjust Speed:** Use the vertical slider on the left to control the animation speed (higher is slower).
5. **Select Algorithm:** Choose a solving algorithm (DFS, BFS, A*) from the dropdown menu.
6. **Solve:** Click the "Solve Maze" button. Observe the chosen algorithm exploring the maze. BFS and A* will highlight the shortest path found in green upon completion.
7. **Reset/Stop:**
    * Click "Stop" while generation or solving is in progress to interrupt it.
    * Click "Reset" when idle to clear the current maze.
8. **Save:** Click "Save Maze" to save the current maze structure and state to `mazeSave.ser` in the application's directory.
9. **Load:** Click "Load Maze" to load the maze from `mazeSave.ser`.
10. **Export:** Click "Export Maze" to save the current visual representation of the maze panel as `maze.png`.

---

## Algorithms Implemented

* **Maze Generation (Randomized DFS):**
  * Starts with a grid full of walls.
  * Treats cells as "rooms".
  * Randomly selects walls between rooms.
  * Tears down a wall *only* if the rooms on either side are not already connected (preventing loops).
  * Continues until all reachable cells are part of the same connected area.
* **Maze Solving (DFS - Depth First Search):**
  * Explores as far as possible down one path before backtracking.
  * Uses recursion (implemented iteratively in the code structure via method calls).
  * Guaranteed to find *a* solution if one exists, but not necessarily the shortest one.
* **Maze Solving (BFS - Breadth First Search):**
  * Explores neighbor nodes level by level.
  * Uses a Queue to manage cells to visit.
  * Guaranteed to find the *shortest* path in terms of the number of steps in an unweighted maze.
  * Uses a `parent` array to reconstruct the path after reaching the goal.
* **Maze Solving (A* Search):**
  * An informed search algorithm that aims to find the shortest path more efficiently than BFS.
  * Uses a Priority Queue, prioritizing cells based on a cost function: `f(n) = g(n) + h(n)`
    * `g(n)`: The actual cost (number of steps) from the start node to node `n`.
    * `h(n)`: A heuristic estimate (Manhattan distance) from node `n` to the goal.
  * Also guaranteed to find the shortest path if the heuristic is admissible (which Manhattan distance is).
  * Uses a `parent` array to reconstruct the path.

---

## File Structure

```file structure

.
├── MazeApp.java        # Main application logic, GUI setup, generation/solving algorithms
├── MazePanel.java      # Custom JPanel for drawing the maze
├── README.md           # This file
├── mazeSave.ser        # (Generated) Saved maze state file
└── maze.png            # (Generated) Exported maze image

```

---

Enjoy exploring the world of mazes with Maze Master!
