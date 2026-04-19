package pers.hpcx.aircraftwar.leaderboard

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getStringOrNull
import pers.hpcx.aircraftwar.kernal.GameDifficulty

class LeaderboardDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    
    companion object {
        
        private const val DATABASE_NAME = "leaderboard.db"
        private const val TABLE_NAME = "scores"
        private const val COLUMN_ID = "id"
        private const val COLUMN_RED_PLAYER_ID = "red_player_id"
        private const val COLUMN_BLUE_PLAYER_ID = "blue_player_id"
        private const val COLUMN_DIFFICULTY = "difficulty"
        private const val COLUMN_TOTAL_SCORE = "total_score"
        private const val COLUMN_DURATION = "duration"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_UPLOADED = "uploaded"
        
        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RED_PLAYER_ID TEXT,
                $COLUMN_BLUE_PLAYER_ID TEXT,
                $COLUMN_DIFFICULTY TEXT NOT NULL,
                $COLUMN_TOTAL_SCORE INTEGER NOT NULL,
                $COLUMN_DURATION REAL NOT NULL,
                $COLUMN_DATE INTEGER NOT NULL,
                $COLUMN_UPLOADED INTEGER NOT NULL
            )
        """
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
    
    fun insertScore(record: LeaderboardRecord) {
        try {
            writableDatabase.insert(
                TABLE_NAME,
                null,
                ContentValues().apply {
                    put(COLUMN_RED_PLAYER_ID, record.redPlayerId)
                    put(COLUMN_BLUE_PLAYER_ID, record.bluePlayerId)
                    put(COLUMN_DIFFICULTY, record.difficulty.name)
                    put(COLUMN_TOTAL_SCORE, record.totalScore)
                    put(COLUMN_DURATION, record.duration)
                    put(COLUMN_DATE, record.date)
                    put(COLUMN_UPLOADED, if (record.uploaded) 1 else 0)
                }
            )
        } catch (e: Exception) {
            Log.e(LeaderboardDatabase::class.simpleName, "Error inserting score", e)
        }
    }
    
    fun getScores(ids: Collection<Int>? = null): List<LeaderboardRecord> {
        try {
            val cursor = this.readableDatabase.query(
                TABLE_NAME,
                null,
                if (ids == null) null else "$COLUMN_ID IN (${ids.joinToString(",") { "?" }})",
                ids?.map(Int::toString)?.toTypedArray<String>(),
                null,
                null,
                "$COLUMN_TOTAL_SCORE DESC, $COLUMN_DATE DESC"
            )
            val scores = mutableListOf<LeaderboardRecord>()
            while (cursor.moveToNext()) {
                scores.add(cursor.getLeaderboardEntry())
            }
            cursor.close()
            return scores
        } catch (e: Exception) {
            Log.e(LeaderboardDatabase::class.simpleName, "Error getting scores", e)
            return emptyList()
        }
    }
    
    fun markUploaded(id: Int) {
        try {
            writableDatabase.update(
                TABLE_NAME,
                ContentValues().apply {
                    put(COLUMN_UPLOADED, 1)
                },
                "$COLUMN_ID = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            Log.e(LeaderboardDatabase::class.simpleName, "Error marking score as uploaded", e)
        }
    }
    
    fun markUploadFailed(id: Int) {
        try {
            writableDatabase.update(
                TABLE_NAME,
                ContentValues().apply {
                    put(COLUMN_UPLOADED, 0)
                },
                "$COLUMN_ID = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            Log.e(LeaderboardDatabase::class.simpleName, "Error marking score as failed", e)
        }
    }
    
    fun deleteScore(id: Int) {
        try {
            writableDatabase.delete(
                TABLE_NAME,
                "$COLUMN_ID = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            Log.e(LeaderboardDatabase::class.simpleName, "Error deleting score", e)
        }
    }
    
    private fun Cursor.getLeaderboardEntry(): LeaderboardRecord = LeaderboardRecord(
        id = getInt(getColumnIndexOrThrow(COLUMN_ID)),
        redPlayerId = getStringOrNull(getColumnIndexOrThrow(COLUMN_RED_PLAYER_ID)),
        bluePlayerId = getStringOrNull(getColumnIndexOrThrow(COLUMN_BLUE_PLAYER_ID)),
        difficulty = GameDifficulty.valueOf(getString(getColumnIndexOrThrow(COLUMN_DIFFICULTY))),
        totalScore = getInt(getColumnIndexOrThrow(COLUMN_TOTAL_SCORE)),
        duration = getFloat(getColumnIndexOrThrow(COLUMN_DURATION)),
        date = getLong(getColumnIndexOrThrow(COLUMN_DATE)),
        uploaded = getInt(getColumnIndexOrThrow(COLUMN_UPLOADED)) != 0,
    )
}
