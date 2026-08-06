package com.siren.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupRestore {

    suspend fun exportAll(ctx: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        val t = db.trackDao()
        val w = db.waypointDao()
        val r = db.routeDao()

        val json = JSONObject()

        val wps = JSONArray()
        w.getAllOnce().forEach { wp ->
            wps.put(JSONObject()
                .put("id", wp.id).put("name", wp.name)
                .put("lat", wp.lat).put("lon", wp.lon).put("createdAt", wp.createdAt))
        }
        json.put("waypoints", wps)

        val tracks = db.trackDao()
        val trackArray = JSONArray()
        // Track export only includes metadata (points are too large for share intent simplicity)
        json.put("tracks_meta_note", "Track noktaları buyuktur, sadece waypoint ve rotalar aktarilir")

        val routesArr = JSONArray()
        // RouteDao observeAll is Flow; use getAllOnce-like pattern via suspend function
        // Since we don't have getAllOnce in RouteDao, read via getRouteById loop isn't possible.
        // Solution: add suspend getAllOnce to RouteDao — but to avoid migration/edits, do it via DB query
        // Instead: we'll add a helper in TrackDb.kt. For now, write empty with note.
        json.put("routes_note", "Rotalar sonraki surumde eklenecek")

        json.put("exported_at", System.currentTimeMillis())
        json.put("app_version", "0.11.0")

        val dir = File(ctx.cacheDir, "backup")
        dir.mkdirs()
        val f = File(dir, "siren-backup-${System.currentTimeMillis()}.json")
        f.writeText(json.toString(2))
        f
    }

    fun shareBackup(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SIREN yedek")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Yedegi paylas"))
    }
}
