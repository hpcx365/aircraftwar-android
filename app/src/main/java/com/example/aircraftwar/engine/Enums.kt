package com.example.aircraftwar.engine

enum class GameDifficulty {
    EASY,
    NORMAL,
    HARD,
}

enum class EntityType {
    RED_HERO,
    BLUE_HERO,
    MOB_ENEMY,
    ELITE_ENEMY,
    SUPER_ENEMY,
    BOSS_ENEMY,
    RED_HERO_BULLET,
    BLUE_HERO_BULLET,
    ENEMY_BULLET,
    HEALTH_PROP,
    ENHANCE_PROP,
    RAMPAGE_PROP,
    BOMB_PROP,
}

enum class PropType {
    HEALTH,
    ENHANCE,
    RAMPAGE,
    BOMB,
}

enum class AudioEvent {
    HERO_SHOOT,
    BULLET_HIT,
    PICKUP_PROP,
    BOMB_TRIGGER,
    GAME_OVER,
}

enum class SerializationType {
    COMMAND_START_GAME,
    COMMAND_PLAYER_JOIN_RED,
    COMMAND_PLAYER_JOIN_BLUE,
    COMMAND_PLAYER_MOVE,
    COMMAND_PLAYER_STOP,
    COMMAND_PLAYER_LEAVE,
    SNAPSHOT_FRAME,
}
