package pers.hpcx.aircraftwar.client

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import pers.hpcx.aircraftwar.R

class SpriteRepository(context: Context) {
    
    private val resources = context.resources
    
    val bg1: Bitmap = load(R.drawable.bg1)
    val bg2: Bitmap = load(R.drawable.bg2)
    val bg3: Bitmap = load(R.drawable.bg3)
    val bg4: Bitmap = load(R.drawable.bg4)
    val bg5: Bitmap = load(R.drawable.bg5)
    
    val heroRed: Bitmap = load(R.drawable.hero_red)
    val heroBlue: Bitmap = load(R.drawable.hero_blue)
    
    val enemyMob: Bitmap = load(R.drawable.enemy_mob)
    val enemyElite: Bitmap = load(R.drawable.enemy_elite)
    val enemySuper: Bitmap = load(R.drawable.enemy_super)
    val enemyBoss: Bitmap = load(R.drawable.enemy_boss)
    
    val bulletHeroRed: Bitmap = load(R.drawable.bullet_hero_red)
    val bulletHeroBlue: Bitmap = load(R.drawable.bullet_hero_blue)
    val bulletEnemy: Bitmap = load(R.drawable.bullet_enemy)
    
    val propHealth: Bitmap = load(R.drawable.prop_health)
    val propEnhance: Bitmap = load(R.drawable.prop_enhance)
    val propRampage: Bitmap = load(R.drawable.prop_rampage)
    val propBomb: Bitmap = load(R.drawable.prop_bomb)
    
    private fun load(@DrawableRes resId: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        return BitmapFactory.decodeResource(resources, resId, options)
    }
}
