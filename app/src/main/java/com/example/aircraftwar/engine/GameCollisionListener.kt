package com.example.aircraftwar.engine

import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.dynamics.contacts.Contact

class GameCollisionListener(
    private val onCollision: (GameEntity, GameEntity) -> Unit
) : ContactListener {
    override fun beginContact(contact: Contact) {
        val a = contact.fixtureA.body.userData as? GameEntity ?: return
        val b = contact.fixtureB.body.userData as? GameEntity ?: return
        onCollision(a, b)
    }

    override fun endContact(contact: Contact) = Unit

    override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit

    override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit
}
