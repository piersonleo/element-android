package com.vcard.vchat.mesh.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class BatchAccountData {

    @SerializedName("n", alternate = ["name"])
    var name: String? = null

    @SerializedName("a", alternate = ["address"])
    var address: String? = null

    @SerializedName("e", alternate = ["encryptedKey"])
    var encryptedKey: String? = null
}
