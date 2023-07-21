//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.database

import com.vcard.mesh.sdk.database.entity.AccountEntity
import com.vcard.mesh.sdk.database.entity.AccountMutxoEntity
import com.vcard.mesh.sdk.database.entity.NodeEntity
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
