package com.github.mantis133.puzzleapp.puzzle.minesweeper

/**
 * Pure game logic for Minesweeper.
 *
 * All functions are side-effect-free and return new cell lists.
 */
object MinesweeperEngine {

    // ── Board generation ──────────────────────────────────────────────────────

    /**
     * Generate a fresh board for [difficulty].
     *
     * [safePos] and its 8 neighbours are guaranteed to be mine-free so that the
     * first click always starts a flood-fill of revealed cells.
     */
    fun generateBoard(
        difficulty: MinesweeperDifficulty,
        safePos: Int
    ): List<MinesweeperCell> {
        val rows  = difficulty.rows
        val cols  = difficulty.cols
        val total = rows * cols

        val safeRow = safePos / cols
        val safeCol = safePos % cols

        val safeSet = buildSet {
            for (dr in -1..1) for (dc in -1..1) {
                val r = safeRow + dr
                val c = safeCol + dc
                if (r in 0 until rows && c in 0 until cols) add(r * cols + c)
            }
        }

        val minePositions = (0 until total)
            .filter { it !in safeSet }
            .shuffled()
            .take(difficulty.mineCount)
            .toSet()

        return List(total) { idx ->
            val isMine = idx in minePositions
            val r = idx / cols
            val c = idx % cols
            val adj = if (isMine) 0 else {
                var count = 0
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until rows && nc in 0 until cols &&
                        (nr * cols + nc) in minePositions
                    ) count++
                }
                count
            }
            MinesweeperCell(isMine = isMine, adjacentMines = adj)
        }
    }

    // ── Reveal ────────────────────────────────────────────────────────────────

    /**
     * Reveal the cell at [pos].
     *
     * - If it is a mine the function reveals ALL mines and returns `hitMine = true`.
     * - If it has 0 adjacent mines it BFS-floods to reveal all connected empty cells.
     * - Flagged cells are never revealed.
     *
     * @return Updated cell list + whether a mine was struck.
     */
    fun reveal(
        cells: List<MinesweeperCell>,
        pos: Int,
        rows: Int,
        cols: Int
    ): Pair<List<MinesweeperCell>, Boolean> {
        val cell = cells[pos]
        if (cell.state != CellState.HIDDEN) return cells to false

        if (cell.isMine) {
            // Show all mines; mark the struck mine differently via state
            val updated = cells.mapIndexed { idx, c ->
                if (c.isMine) c.copy(state = CellState.REVEALED) else c
            }
            return updated to true
        }

        // BFS flood-fill
        val mutable = cells.toMutableList()
        val queue   = ArrayDeque<Int>()
        val visited = mutableSetOf<Int>()
        queue.add(pos)

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (!visited.add(cur)) continue

            val c = mutable[cur]
            if (c.state == CellState.FLAGGED || c.state == CellState.REVEALED) continue

            mutable[cur] = c.copy(state = CellState.REVEALED)

            if (c.adjacentMines == 0) {
                val r = cur / cols; val col = cur % cols
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr; val nc = col + dc
                    if (nr in 0 until rows && nc in 0 until cols) {
                        val nIdx = nr * cols + nc
                        if (nIdx !in visited) queue.add(nIdx)
                    }
                }
            }
        }

        return mutable to false
    }

    // ── Chord ─────────────────────────────────────────────────────────────────

    /**
     * "Chord" a revealed numbered cell: if the number of adjacent flagged cells
     * equals the cell's [MinesweeperCell.adjacentMines], reveal all remaining
     * hidden neighbours.
     *
     * This is the tap-on-number auto-reveal behaviour that Google Minesweeper
     * exposes via the action popup.
     *
     * @return Updated cells + whether a mine was struck.
     */
    fun chord(
        cells: List<MinesweeperCell>,
        pos: Int,
        rows: Int,
        cols: Int
    ): Pair<List<MinesweeperCell>, Boolean> {
        val cell = cells[pos]
        if (cell.state != CellState.REVEALED || cell.adjacentMines == 0) return cells to false

        val neighbours = neighbours(pos, rows, cols)
        val flagCount  = neighbours.count { cells[it].state == CellState.FLAGGED }
        if (flagCount != cell.adjacentMines) return cells to false

        var current = cells
        var hitMine = false
        for (n in neighbours) {
            if (current[n].state == CellState.HIDDEN) {
                val (updated, mine) = reveal(current, n, rows, cols)
                current = updated
                if (mine) hitMine = true
            }
        }
        return current to hitMine
    }

    // ── Flag ──────────────────────────────────────────────────────────────────

    /** Toggle flag on a HIDDEN or FLAGGED cell; revealed cells are untouched. */
    fun toggleFlag(cells: List<MinesweeperCell>, pos: Int): List<MinesweeperCell> {
        val cell = cells[pos]
        if (cell.state == CellState.REVEALED) return cells
        val newState = if (cell.state == CellState.FLAGGED) CellState.HIDDEN else CellState.FLAGGED
        return cells.toMutableList().also { it[pos] = cell.copy(state = newState) }
    }

    // ── Win detection ─────────────────────────────────────────────────────────

    /** True when every non-mine cell is revealed. */
    fun isWon(cells: List<MinesweeperCell>): Boolean =
        cells.all { it.isMine || it.state == CellState.REVEALED }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun neighbours(pos: Int, rows: Int, cols: Int): List<Int> {
        val r = pos / cols; val c = pos % cols
        return buildList {
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) add(nr * cols + nc)
            }
        }
    }
}

