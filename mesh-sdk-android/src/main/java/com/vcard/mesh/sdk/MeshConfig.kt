//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk

import android.content.Context
import com.vcard.mesh.sdk.database.MeshModule
import io.realm.Realm
import io.realm.RealmConfiguration
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object MeshConfig {

    fun initialize(context: Context){
        //insert bouncy castle manually as the prebuilt provider is trimmed down
        Security.removeProvider("BC")
        val bc = BouncyCastleProvider()
        Security.insertProviderAt(bc, 1)

        //initialize database
        Realm.init(context)
        val realmConfig = RealmConfiguration.Builder()
                .name("vcard_mesh.realm")
                .modules(MeshModule())
                .schemaVersion(1)
                .allowWritesOnUiThread(true)
                .build()

        Realm.setDefaultConfiguration(realmConfig)

    }
}
