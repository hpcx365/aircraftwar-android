package pers.hpcx.aircraftwar.client

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeaderboardDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        
        private const val DATABASE_NAME = "leaderboard.db"
        private const val DATABASE_VERSION = 1
        
        const val TABLE_NAME = "scores"
        const val COLUMN_ID = "id"
        const val COLUMN_SCORE = "score"
        const val COLUMN_DIFFICULTY = "difficulty"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_DATE = "date"
        
        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SCORE INTEGER NOT NULL,
                $COLUMN_DIFFICULTY TEXT NOT NULL,
                $COLUMN_DURATION REAL NOT NULL,
                $COLUMN_DATE TEXT NOT NULL
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
    
    /**
     * 插入新的分数记录
     */
    fun insertScore(score: Int, difficulty: GameDifficulty, duration: Float): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SCORE, score)
            put(COLUMN_DIFFICULTY, difficulty.name)
            put(COLUMN_DURATION, duration)
            put(COLUMN_DATE, System.currentTimeMillis().toString())
        }
        return db.insert(TABLE_NAME, null, values)
    }
    
    /**
     * 获取所有分数记录，按分数降序排列
     */
    fun getAllScores(): List<LeaderboardEntry> {
        val scores = mutableListOf<LeaderboardEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_SCORE DESC, $COLUMN_DATE DESC"
        )
        
        with(cursor) {
            while (moveToNext()) {
                scores.add(getLeaderboardEntry())
            }
            close()
        }
        
        return scores
    }
    
    /**
     * 获取指定难度的分数记录
     */
    fun getScoresByDifficulty(difficulty: String): List<LeaderboardEntry> {
        val scores = mutableListOf<LeaderboardEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_DIFFICULTY = ?",
            arrayOf(difficulty),
            null,
            null,
            "$COLUMN_SCORE DESC, $COLUMN_DATE DESC"
        )
        
        with(cursor) {
            while (moveToNext()) {
                scores.add(getLeaderboardEntry())
            }
            close()
        }
        
        return scores
    }
    
    /**
     * 获取前N名分数记录
     */
    fun getTopScores(limit: Int = 10): List<LeaderboardEntry> {
        val scores = mutableListOf<LeaderboardEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_SCORE DESC, $COLUMN_DATE DESC",
            limit.toString()
        )
        
        with(cursor) {
            while (moveToNext()) {
                scores.add(getLeaderboardEntry())
            }
            close()
        }
        
        return scores
    }
    
    private fun Cursor.getLeaderboardEntry(): LeaderboardEntry {
        val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
        val score = getInt(getColumnIndexOrThrow(COLUMN_SCORE))
        val difficulty = GameDifficulty.valueOf(getString(getColumnIndexOrThrow(COLUMN_DIFFICULTY)))
        val duration = getFloat(getColumnIndexOrThrow(COLUMN_DURATION))
        val date = getString(getColumnIndexOrThrow(COLUMN_DATE))
        return LeaderboardEntry(id, score, difficulty, duration, date)
    }
    
    /**
     * 删除所有记录
     */
    fun clearAllScores() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
    
    /**
     * 删除指定ID的记录
     */
    fun deleteScoreById(id: Int): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rowsDeleted > 0
    }
}

/**
 * 排行榜数据实体
 */
data class LeaderboardEntry(
    val id: Int,
    val score: Int,
    val difficulty: GameDifficulty,
    val duration: Float,
    val date: String
) {
    
    /**
     * 格式化时间显示
     */
    fun getFormattedDuration(): String {
        val totalSeconds = duration.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * 格式化日期显示
     */
    fun getFormattedDate(): String {
        val timestamp = date.toLongOrNull() ?: 0L
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
