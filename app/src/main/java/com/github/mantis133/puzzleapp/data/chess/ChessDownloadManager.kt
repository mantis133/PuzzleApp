package com.github.mantis133.puzzleapp.data.chess

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Downloads the pre-built, gzip-compressed Lichess puzzle SQLite database from
 * a GitHub Release (or any static HTTP host) and decompresses it into the app's
 * Room database directory.
 *
 * After [downloadAndInstall] completes successfully, call
 * [ChessPuzzleDatabase.invalidate] so the next [getInstance] picks up the new file.
 */
class ChessDownloadManager(private val context: Context) {

    /**
     * URL of the gzip-compressed pre-built SQLite file.
     * Replace with your actual GitHub Release asset URL before publishing.
     */
    val downloadUrl: String = "https://github.com/mantis133/PuzzleApp/releases/download/puzzles/chess_puzzles.db.gz"

    sealed class DownloadState {
        data object Idle        : DownloadState()
        data object Checking    : DownloadState()
        /** [received] and [total] are bytes (-1 if total unknown). */
        data class  Downloading(val received: Long, val total: Long) : DownloadState()
        data object Decompressing : DownloadState()
        data object Done          : DownloadState()
        data class  Error(val message: String) : DownloadState()
    }

    /** True if the database file already exists in the databases directory. */
    fun isDatabaseInstalled(): Boolean =
        context.getDatabasePath(ChessSQLiteHelper.DB_NAME).exists()

    /**
     * Downloads and decompresses the puzzle database.
     * [onProgress] is called on each chunk with the current [DownloadState].
     * Returns true on success.
     */
    suspend fun downloadAndInstall(onProgress: (DownloadState) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val client = HttpClient(Android)
                val gzTemp = File(context.cacheDir, "chess_puzzles_download.db.gz")
                val dbFile  = context.getDatabasePath(ChessSQLiteHelper.DB_NAME)

            try {
                onProgress(DownloadState.Downloading(0, -1))

                // ── Download ────────────────────────────────────────────────
                client.prepareGet(downloadUrl).execute { response ->
                    val total   = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                    val channel = response.bodyAsChannel()
                    val buffer  = ByteArray(DEFAULT_BUFFER_SIZE)
                    var received = 0L

                    FileOutputStream(gzTemp).use { out ->
                        while (!channel.isClosedForRead) {
                            val bytes = channel.readAvailable(buffer)
                            if (bytes > 0) {
                                out.write(buffer, 0, bytes)
                                received += bytes
                                onProgress(DownloadState.Downloading(received, total))
                            }
                        }
                    }
                }

                // ── Decompress ──────────────────────────────────────────────
                onProgress(DownloadState.Decompressing)
                dbFile.parentFile?.mkdirs()
                GZIPInputStream(gzTemp.inputStream().buffered()).use { gz ->
                    FileOutputStream(dbFile).use { out -> gz.copyTo(out) }
                }

                onProgress(DownloadState.Done)
                true

            } catch (e: Exception) {
                onProgress(DownloadState.Error(e.message ?: "Unknown error"))
                false
            } finally {
                gzTemp.delete()
                client.close()
            }
        }
}




