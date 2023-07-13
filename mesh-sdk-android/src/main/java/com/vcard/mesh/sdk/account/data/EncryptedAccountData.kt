//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.account.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class EncryptedAccountData {

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

class EncryptedAccountDataSerializer : JsonSerializer<EncryptedAccountData> {

    override fun serialize(encryptedKeyData: EncryptedAccountData, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("fulladdress", jsonSerializationContext.serialize(encryptedKeyData.fullAddress))
        jsonObject.add("prefix", jsonSerializationContext.serialize(encryptedKeyData.prefix))
        jsonObject.add("address", jsonSerializationContext.serialize(encryptedKeyData.address))
        jsonObject.add("checksum", jsonSerializationContext.serialize(encryptedKeyData.checksum))
        jsonObject.add("encryptedkey", jsonSerializationContext.serialize(encryptedKeyData.encryptedKey))

        return jsonObject
    }
}

