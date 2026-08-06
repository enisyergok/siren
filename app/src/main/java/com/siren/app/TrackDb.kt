package com.siren.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val isRecording: Boolean
)

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val lat: Double,
    val lon: Double,
    val time: Long,
    val speedKnots: Double,
    val heading: Double
)

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val createdAt: Long
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val colorHex: String
)

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId"), Index("routeId", "sortOrder")]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val lat: Double,
    val lon: Double,
    val sortOrder: Int,
    val name: String?
)

@Dao
interface TrackDao {
    @Insert suspend fun insertTrack(track: TrackEntity)
    @Insert suspend fun insertPoint(point: TrackPointEntity)
    @Query("UPDATE tracks SET endTime = :end, isRecording = 0 WHERE id = :id")
    suspend fun finishTrack(id: String, end: Long)
    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun observeTracks(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY time ASC")
    suspend fun getPointsForTrack(trackId: String): List<TrackPointEntity>
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?
}

@Dao
interface WaypointDao {
    @Insert suspend fun insert(wp: WaypointEntity)
    @Query("SELECT * FROM waypoints ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WaypointEntity>>
    @Query("SELECT * FROM waypoints ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<WaypointEntity>
    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RouteDao {
    @Insert suspend fun insertRoute(route: RouteEntity)
    @Insert suspend fun insertPoint(point: RoutePointEntity)
    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RouteEntity>>
    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY sortOrder ASC")
    suspend fun getPointsForRoute(routeId: String): List<RoutePointEntity>
    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: String): RouteEntity?
    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun delete(id: String)
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM route_points WHERE routeId = :routeId")
    suspend fun getMaxSortOrder(routeId: String): Int
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS waypoints (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "lat REAL NOT NULL, " +
                "lon REAL NOT NULL, " +
                "createdAt INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS routes (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "colorHex TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS route_points (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "routeId TEXT NOT NULL, " +
                "lat REAL NOT NULL, " +
                "lon REAL NOT NULL, " +
                "sortOrder INTEGER NOT NULL, " +
                "name TEXT, " +
                "FOREIGN KEY(routeId) REFERENCES routes(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_route_points_routeId ON route_points(routeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_route_points_routeId_sortOrder ON route_points(routeId, sortOrder)")
    }
}

@Database(
    entities = [TrackEntity::class, TrackPointEntity::class, WaypointEntity::class, RouteEntity::class, RoutePointEntity::class],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun waypointDao(): WaypointDao
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siren.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
            }
    }
}

fun trackToGpx(track: TrackEntity, points: List<TrackPointEntity>): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val sb = StringBuilder()
    sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    sb.appendLine("<gpx version=\"1.1\" creator=\"SIREN\">")
    sb.appendLine("  <trk>")
    sb.appendLine("    <name>${escapeXml(track.name)}</name>")
    sb.appendLine("    <trkseg>")
    points.forEach { p ->
        sb.appendLine("      <trkpt lat=\"${p.lat}\" lon=\"${p.lon}\">")
        sb.appendLine("        <time>${fmt.format(Date(p.time))}</time>")
        sb.appendLine("      </trkpt>")
    }
    sb.appendLine("    </trkseg>")
    sb.appendLine("  </trk>")
    sb.appendLine("</gpx>")
    return sb.toString()
}

fun waypointsToGpx(waypoints: List<WaypointEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    sb.appendLine("<gpx version=\"1.1\" creator=\"SIREN\">")
    waypoints.forEach { wp ->
        sb.appendLine("  <wpt lat=\"${wp.lat}\" lon=\"${wp.lon}\">")
        sb.appendLine("    <name>${escapeXml(wp.name)}</name>")
        sb.appendLine("  </wpt>")
    }
    sb.appendLine("</gpx>")
    return sb.toString()
}

fun routeToGpx(route: RouteEntity, points: List<RoutePointEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    sb.appendLine("<gpx version=\"1.1\" creator=\"SIREN\">")
    sb.appendLine("  <rte>")
    sb.appendLine("    <name>${escapeXml(route.name)}</name>")
    points.forEach { p ->
        sb.appendLine("    <rtept lat=\"${p.lat}\" lon=\"${p.lon}\">")
        if (!p.name.isNullOrEmpty()) sb.appendLine("      <name>${escapeXml(p.name)}</name>")
        sb.appendLine("    </rtept>")
    }
    sb.appendLine("  </rte>")
    sb.appendLine("</gpx>")
    return sb.toString()
}

private fun escapeXml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
