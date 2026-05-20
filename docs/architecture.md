# Architecture Notes

Maze Master is organized as a small desktop MVC application. The model owns maze state, the controller owns application workflow and background operations, and the Swing view owns rendering and user interaction.

## Package Structure

```text
src/main/java/com/mazemaster/
|-- MazeMasterApplication.java
|-- controller/
|   `-- MazeController.java
|-- export/
|   `-- AnimatedGifExporter.java
|-- generation/
|   |-- DepthFirstSearchGenerator.java
|   |-- KruskalGenerator.java
|   |-- MazeGenerationContext.java
|   |-- MazeGenerationListener.java
|   |-- MazeGenerationStrategy.java
|   |-- MazeGenerator.java
|   |-- PrimGenerator.java
|   `-- Wall.java
|-- model/
|   |-- Maze.java
|   `-- MazeMetrics.java
|-- persistence/
|   `-- MazeFileService.java
|-- solving/
|   |-- AStarSolver.java
|   |-- BreadthFirstSearchSolver.java
|   |-- DepthFirstSearchSolver.java
|   |-- MazeSolver.java
|   |-- MazeSolvingContext.java
|   |-- MazeSolvingListener.java
|   `-- MazeSolvingStrategy.java
`-- ui/
    |-- MazeView.java
    `-- swing/
        |-- MazePanel.java
        `-- SwingMazeView.java
```

## Startup Flow

`MazeMasterApplication` sets Swing rendering options, applies the system look and feel, creates the controller, creates the Swing view, then displays the window. The view registers itself with the controller through `controller.setView(this)`.

The welcome dialog is owned by `SwingMazeView` because it depends on Swing preferences. It is short, startup-only, and can be permanently hidden with the `welcome.show` preference.

## Model

`Maze` stores:

- cell grid
- dimensions
- start and goal coordinates
- generation seed
- generation algorithm name

The endpoint coordinates are metadata on the model. The Swing renderer draws the green start marker and red goal marker from those coordinates rather than requiring special grid values at those cells. Solver visualization still uses cell values such as `PATH`, `VISITED`, and `START` to show exploration and final path state.

`MazeMetrics` is an immutable snapshot of the current generation/solving cycle:

- generation time
- solving time
- walkable cells
- explored cells
- path length
- whether solving was attempted
- whether solving succeeded

The controller replaces the snapshot as work progresses, which keeps view updates simple and avoids exposing mutable metric counters.

## Controller Flow

`MazeController` coordinates all application behavior:

- creates blank mazes
- starts generation and solving
- pauses, resumes, and stops background work
- validates whether solving is allowed
- moves start and goal endpoints
- saves and loads files
- updates metrics
- notifies the view

Generation and solving run on a single `ExecutorService`. Keeping a single worker means the app cannot generate and solve at the same time, and the controller can reason about one active operation at a time.

The controller exposes `canSolveMaze()` for both UI state and defensive controller behavior. A maze can be solved only when no operation is running and the start can reach the goal through walkable cells.

## View Boundary

`MazeView` is the controller-facing interface. It lets the controller update the maze, refresh rendering, report lifecycle events, update controls, update metrics, and display messages without depending on Swing classes.

`SwingMazeView` implements the full desktop UI:

- top action buttons
- algorithm, dimension, seed, metrics, and speed controls
- status bar
- file dialogs
- GIF and image export actions
- endpoint click handling
- persistent window, zoom, speed, and welcome-dialog preferences

`MazePanel` is responsible for drawing the maze grid, selected endpoint rings, zoom behavior, and cell hit detection.

## Generation Strategies

`MazeGenerator` is the orchestrator for registered generation strategies. It keeps the strategy map and delegates by algorithm name:

- `DFS` -> `DepthFirstSearchGenerator`
- `Kruskal` -> `KruskalGenerator`
- `Prim` -> `PrimGenerator`

Each strategy implements `MazeGenerationStrategy` and receives a `MazeGenerationContext`. The context provides:

- seeded `Random`
- pause/resume-aware sleep
- cancellation checks
- animation delay
- cell-change callbacks
- generation-step callbacks

To add a generation algorithm, implement `MazeGenerationStrategy` and register it in `MazeGenerator`.

## Solving Strategies

`MazeSolver` mirrors the generation design. It registers solving strategies by display name:

- `Depth First Search` -> `DepthFirstSearchSolver`
- `Breadth First Search` -> `BreadthFirstSearchSolver`
- `A*` -> `AStarSolver`

Each strategy implements `MazeSolvingStrategy` and receives a `MazeSolvingContext`. The context provides:

- pause/resume-aware sleep
- cancellation checks
- animation delay
- explored-cell callbacks
- backtrack callbacks
- final-path callbacks

To add a solver, implement `MazeSolvingStrategy` and register it in `MazeSolver`.

## Pause And Resume

Pause/resume is an intended feature, not a workaround. The controller owns atomic pause and stop flags for generation and solving. Context objects poll those flags through `pauseAwareSleep`.

When paused:

- the worker thread stays alive
- algorithm-local state stays on the stack or in local collections
- the operation resumes from the same point
- reset is enabled so the user can safely abandon paused work

When stopped:

- the controller clears pause flags first so a paused worker can continue far enough to observe the stop flag
- it waits briefly for the task to finish
- it cancels the task if the worker does not exit in time

## Seed Reproducibility

The controller stores the current generation seed and selected generation algorithm. Before generation starts, it resets the generator's `Random` with that seed and writes the seed/algorithm metadata into the maze.

A maze is reproducible when the same values are used:

- rows
- columns
- generation algorithm
- seed

Loaded maze files restore seed and algorithm metadata so the user can see how the maze was produced.

## Persistence Format

`MazeFileService` saves maze state as UTF-8 text with a magic header and version number.

Current format, version 3:

```text
MAZE_MASTER_SAVE 3
<rows> <columns>
seed <long>
algorithm <base64-encoded-name>
start <row> <column>
goal <row> <column>
<space-separated cell values>
...
```

Version history:

- Version 1 stored dimensions and grid only.
- Version 2 added seed and generation algorithm metadata.
- Version 3 added start and goal endpoint metadata.

The loader accepts older supported versions and supplies defaults where older files lack metadata. Unsupported versions, malformed dimensions, invalid metadata, and unsupported cell values fail with `IOException`.

## Metrics Lifecycle

Metrics reset completely when a blank workspace is created or generation starts. Generation completion records elapsed generation time and walkable cell count.

Solving metrics reset when solving starts, when a solution is cleared, or when endpoints move. Solving completion records elapsed solve time, explored cell count, final path length, and solved state.

Loaded mazes recompute walkable cell count but do not claim generation or solving timings because those timings are runtime observations, not persisted file data.

## Export

PNG export paints the current maze panel to an image. GIF export records frames during generation or solving, scales large mazes down to a bounded GIF size, and writes an animated image through `AnimatedGifExporter`.

The GIF feature intentionally records the most recent visible animation rather than re-running algorithms offscreen. That keeps the feature local to the UI and avoids introducing a second simulation path.

## Test Coverage

The tests focus on behavior most likely to regress:

- maze initialization and endpoint validation
- deterministic seeded generation
- generation and solving completion callbacks
- pause/resume behavior
- reset behavior
- controller solve eligibility
- save/load format validation and compatibility
- metrics lifecycle
- GIF export validation

The controller depends on `MazeView`, not Swing directly, so controller tests use small test views instead of launching a GUI.
