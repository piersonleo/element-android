package com.vcard.vchat.mesh.database

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class NodeEntity: RealmObject() {
    @PrimaryKey var id: String = ""
    var ipAddress: String = ""
    var port: String = ""
}
