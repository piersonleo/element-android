package com.vcard.vchat.mesh.database

import io.realm.annotations.RealmModule

/**
 * Realm module for authentication classes.
 */
@RealmModule(
        classes = [
            AccountEntity::class,
            NodeEntity::class,
            AccountMutxoEntity::class
        ]
)
internal class MeshModule
