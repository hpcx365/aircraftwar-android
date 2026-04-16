package pers.hpcx.aircraftwar.kernal

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

enum class CommandType {
    START_GAME,
    PLAYER_JOIN_RED,
    PLAYER_JOIN_BLUE,
    PLAYER_MOVE,
    PLAYER_STOP,
    PLAYER_LEAVE,
}
