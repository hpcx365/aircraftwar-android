package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.example.aircraftwar.R

class SpriteRepository(context: Context) {
    
    private val resources = context.resources
    
    val background: Bitmap = load(R.drawable.bg1)
    
    val hero: Bitmap = load(R.drawable.hero)
    val mob: Bitmap = load(R.drawable.mob)
    val elite: Bitmap = load(R.drawable.elite)
    val superElite: Bitmap = load(R.drawable.super_elite)
    val boss: Bitmap = load(R.drawable.boss)
    
    val heroBullet: Bitmap = load(R.drawable.bullet_hero)
    val enemyBullet: Bitmap = load(R.drawable.bullet_enemy)
    
    val healthProp: Bitmap = load(R.drawable.prop_blood)
    val bombProp: Bitmap = load(R.drawable.prop_bomb)
    val bulletProp: Bitmap = load(R.drawable.prop_bullet)
    val superBulletProp: Bitmap = load(R.drawable.prop_super_bullet)
    
    private fun load(@DrawableRes resId: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        return BitmapFactory.decodeResource(resources, resId, options)
    }
}
