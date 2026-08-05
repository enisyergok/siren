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
import kotlinx.coroutines.flow.Flow

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

@Dao
interface TrackDao {
    @Insert
    suspend fun insertTrack(track: TrackEntity)

    @Insert
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("UPDATE tracks SET endTime = :end, isRecording = 0 WHERE id = :id")
    suspend fun finishTrack(id: String, end: Long)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun observeTracks(): Flow<List<TrackEntity>>
}

@Database(entities = [TrackEntity::class, TrackPointEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siren.db"
                ).build()
            }
    }
}
