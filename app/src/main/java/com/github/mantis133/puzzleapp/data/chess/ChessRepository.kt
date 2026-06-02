package com.github.mantis133.puzzleapp.data.chess

import android.content.Context

class ChessRepository(context: Context) {

    private val downloadManager = ChessDownloadManager(context)
    private val sqliteHelper    = ChessSQLiteHelper(context)

    fun isDatabaseInstalled() = downloadManager.isDatabaseInstalled()

    val downloadUrl get() = downloadManager.downloadUrl

    suspend fun downloadDatabase(onProgress: (ChessDownloadManager.DownloadState) -> Unit): Boolean {
        sqliteHelper.close()  // close any open handle before overwriting the file
        return downloadManager.downloadAndInstall(onProgress)
    }

    suspend fun randomPuzzle(minRating: Int = 600, maxRating: Int = 2500): ChessPuzzleData? {
        if (!sqliteHelper.open()) return null
        return sqliteHelper.randomPuzzle(minRating, maxRating)
    }

    suspend fun randomPuzzleWithTheme(minRating: Int, maxRating: Int, theme: String): ChessPuzzleData? {
        if (!sqliteHelper.open()) return null
        return sqliteHelper.randomPuzzleWithTheme(minRating, maxRating, theme)
    }

    suspend fun puzzleCount(): Long {
        if (!sqliteHelper.open()) return 0
        return sqliteHelper.count()
    }
}
