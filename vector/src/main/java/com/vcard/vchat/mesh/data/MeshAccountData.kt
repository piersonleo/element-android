package com.vcard.vchat.mesh.data

import com.google.gson.annotations.SerializedName
import io.realm.annotations.PrimaryKey

class MeshAccountData {

    @SerializedName("address", alternate = ["AccountAddress"])
    val address: String = ""

    @SerializedName("name")
    val name: String = ""

    @SerializedName("encryptedKey", alternate = ["encryptedkey"])
    var encryptedKey: String = ""

    @SerializedName("privateKey", alternate = ["privatekey"])
    var privateKey: String = ""

    @SerializedName("Currency")
    var currency: String = ""

    @SerializedName("Nonce")
    var nonce: Int = 0

    @SerializedName("Balance")
    var balance: Long = 0L

    @SerializedName("RootHash")
    var rootHash: String = ""

    @SerializedName("ModuleHash")
    var moduleHash: String? = ""

    @SerializedName("type", alternate = ["Type"])
    var type: String = ""

}
