package com.vcard.vchat.mesh.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class EncryptedKeyData {

    @SerializedName("fulladdress")
    var fullAddress: String = ""

    var prefix: String = ""

    var address: String = ""

    var checksum: String = ""

    @SerializedName("encryptedkey")
    var encryptedKey: EncryptedKey = EncryptedKey()
}

class EncryptedKey{

    var cipher: String = ""

    @SerializedName("encryptedtext")
    var encryptedText: String = ""

    @SerializedName("keychecksum")
    var keyChecksum: String = ""
}

class EncryptedKeyDataSerializer : JsonSerializer<EncryptedKeyData> {

    override fun serialize(encryptedKeyData: EncryptedKeyData, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("fulladdress", jsonSerializationContext.serialize(encryptedKeyData.fullAddress))
        jsonObject.add("prefix", jsonSerializationContext.serialize(encryptedKeyData.prefix))
        jsonObject.add("address", jsonSerializationContext.serialize(encryptedKeyData.address))
        jsonObject.add("checksum", jsonSerializationContext.serialize(encryptedKeyData.checksum))
        jsonObject.add("encryptedkey", jsonSerializationContext.serialize(encryptedKeyData.encryptedKey))

        return jsonObject
    }
}
