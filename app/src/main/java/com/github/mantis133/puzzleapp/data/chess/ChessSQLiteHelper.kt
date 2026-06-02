package com.github.mantis133.puzzleapp.data.chess

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File

/**
 * Thin read-only wrapper around [SQLiteDatabase] for the pre-built Lichess puzzle file.
 *
 * Using plain SQLiteDatabase (not Room) so there is no schema validation — Room would
 * wipe the pre-built database the first time it opened it because the file lacks
 * Room's internal `room_master_table`.
 */
class ChessSQLiteHelper(context: Context) {

    private val dbFile: File = context.getDatabasePath(DB_NAME)
    private var db: SQLiteDatabase? = null

    fun isInstalled(): Boolean = dbFile.exists() && dbFile.length() > 0

    /** Opens the database. Returns false if the file doesn't exist or is corrupt. */
    fun open(): Boolean {
        if (db?.isOpen == true) return true
        if (!isInstalled()) return false
        return try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            true
        } catch (e: SQLiteException) {
            false
        }
    }

    fun close() {
        db?.close()
        db = null
    }

    /** Returns a random puzzle in the given Elo band, or null if none exist. */
    fun randomPuzzle(minRating: Int = 600, maxRating: Int = 2500): ChessPuzzleData? {
        ensureOpen() ?: return null
        val cursor = db!!.rawQuery(
            """SELECT id, fen, moves, rating, ratingDeviation, popularity, themes, openingTags
               FROM chess_puzzles
               WHERE rating BETWEEN ? AND ?
               ORDER BY RANDOM()
               LIMIT 1""",
            arrayOf(minRating.toString(), maxRating.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) c.toPuzzleData() else null
        }
    }

    /** Returns a random puzzle with the given theme tag in the given Elo band. */
    fun randomPuzzleWithTheme(minRating: Int, maxRating: Int, theme: String): ChessPuzzleData? {
        ensureOpen() ?: return null
        val cursor = db!!.rawQuery(
            """SELECT id, fen, moves, rating, ratingDeviation, popularity, themes, openingTags
               FROM chess_puzzles
               WHERE rating BETWEEN ? AND ?
                 AND themes LIKE ?
               ORDER BY RANDOM()
               LIMIT 1""",
            arrayOf(minRating.toString(), maxRating.toString(), "%$theme%")
        )
        return cursor.use { c ->
            if (c.moveToFirst()) c.toPuzzleData() else null
        }
    }

    /** Approximate count (fast, uses sqlite_stat1 if available). */
    fun count(): Long {
        ensureOpen() ?: return 0
        val cursor = db!!.rawQuery("SELECT COUNT(*) FROM chess_puzzles", null)
        return cursor.use { c -> if (c.moveToFirst()) c.getLong(0) else 0 }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun ensureOpen(): SQLiteDatabase? {
        if (db?.isOpen == true) return db
        open()
        return db
    }

    private fun android.database.Cursor.toPuzzleData() = ChessPuzzleData(
        id             = getString(0),
        fen            = getString(1),
        moves          = getString(2),
        rating         = getInt(3),
        ratingDeviation = getInt(4),
        popularity     = getInt(5),
        themes         = getString(6),
        openingTags    = getString(7)
    )

    companion object {
        const val DB_NAME = "chess_puzzles.db"
    }
}

/** Plain data class — no Room annotations. */
data class ChessPuzzleData(
    val id: String,
    val fen: String,
    val moves: String,
    val rating: Int,
    val ratingDeviation: Int,
    val popularity: Int,
    val themes: String,
    val openingTags: String
)

