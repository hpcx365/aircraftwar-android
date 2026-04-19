package pers.hpcx.aircraftwar.kernal

import org.json.JSONArray
import org.json.JSONObject

data class FrameSnapshot(
    val worldWidth: Float,
    val worldHeight: Float,
    val difficulty: GameDifficulty,
    val redPlayerId: String? = null,
    val bluePlayerId: String? = null,
    val scoreRed: Int,
    val scoreBlue: Int,
    val elapsedTimeSec: Float,
    val hasBoss: Boolean,
    val started: Boolean,
    val gameOver: Boolean,
    val events: List<AudioEvent>,
    val entities: List<EntitySnapshot>,
) {
    
    fun toJson(): String = JSONObject().apply {
        put("worldWidth", worldWidth)
        put("worldHeight", worldHeight)
        put("difficulty", difficulty.name)
        if (redPlayerId != null) put("redPlayerId", redPlayerId)
        if (bluePlayerId != null) put("bluePlayerId", bluePlayerId)
        put("scoreRed", scoreRed)
        put("scoreBlue", scoreBlue)
        put("elapsedTimeSec", elapsedTimeSec)
        put("hasBoss", hasBoss)
        put("started", started)
        put("gameOver", gameOver)
        put("events", JSONArray().apply { for (event in events) put(event.name) })
        put("entities", JSONArray().apply { for (entity in entities) put(entity.toJson()) })
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): FrameSnapshot = JSONObject(json).let {
            FrameSnapshot(
                worldWidth = it.getDouble("worldWidth").toFloat(),
                worldHeight = it.getDouble("worldHeight").toFloat(),
                difficulty = GameDifficulty.valueOf(it.getString("difficulty")),
                redPlayerId = it.optString("redPlayerId").ifBlank { null },
                bluePlayerId = it.optString("bluePlayerId").ifBlank { null },
                scoreRed = it.getInt("scoreRed"),
                scoreBlue = it.getInt("scoreBlue"),
                elapsedTimeSec = it.getDouble("elapsedTimeSec").toFloat(),
                hasBoss = it.getBoolean("hasBoss"),
                started = it.optBoolean("started", false),
                gameOver = it.getBoolean("gameOver"),
                events = it.getJSONArray("events").let { array ->
                    val events = mutableListOf<AudioEvent>()
                    for (i in 0 until array.length()) {
                        events += AudioEvent.valueOf(array.getString(i))
                    }
                    events
                },
                entities = it.getJSONArray("entities").let { array ->
                    val entities = mutableListOf<EntitySnapshot>()
                    for (i in 0 until array.length()) {
                        entities += EntitySnapshot.fromJson(array.getString(i))
                    }
                    entities
                },
            )
        }
    }
}

data class EntitySnapshot(
    val id: Int,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val hp: Int? = null,
    val maxHp: Int? = null,
) {
    
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("x", x)
        put("y", y)
        put("width", width)
        put("height", height)
        if (hp != null) put("hp", hp)
        if (maxHp != null) put("maxHp", maxHp)
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): EntitySnapshot = JSONObject(json).let {
            EntitySnapshot(
                id = it.getInt("id"),
                type = EntityType.valueOf(it.getString("type")),
                x = it.getDouble("x").toFloat(),
                y = it.getDouble("y").toFloat(),
                width = it.getDouble("width").toFloat(),
                height = it.getDouble("height").toFloat(),
                hp = if (it.has("hp")) it.getInt("hp") else null,
                maxHp = if (it.has("maxHp")) it.getInt("maxHp") else null,
            )
        }
    }
}
