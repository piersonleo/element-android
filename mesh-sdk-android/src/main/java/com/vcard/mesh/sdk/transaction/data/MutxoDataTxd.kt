//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.transaction.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class MutxoDataTxd {
    var mutxoKey :String = ""

    @SerializedName("k", alternate = ["mutxoKeyBytes"])
    var mutxoKeyBytes :String? = ""

    @SerializedName("o", alternate = ["ownerAddress"])
    var ownerAddress: MeshAddressTxd? = null

    @SerializedName("c", alternate = ["currency"])
    var currency: String = ""

    @SerializedName("a", alternate = ["amount"])
    var amount: Long = 0L

    var source: String = ""

    @SerializedName("s", alternate = ["sourceBytes"])
    var sourceBytes :String = ""

    @SerializedName("st", alternate = ["sourceType"])
    var sourceType: Int = 0

    @SerializedName("r", alternate = ["reference"])
    var reference: String = ""
}

class MutxoDataTxdSerializer : JsonSerializer<MutxoDataTxd> {

    override fun serialize(mutxoData: MutxoDataTxd, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {

        val addressObject = JsonObject()
        addressObject.add("p", jsonSerializationContext.serialize(mutxoData.ownerAddress!!.p))
        addressObject.add("a", jsonSerializationContext.serialize(mutxoData.ownerAddress!!.a))
        addressObject.add("c", jsonSerializationContext.serialize(mutxoData.ownerAddress!!.c))

        val jsonObject = JsonObject()
        jsonObject.add("k", jsonSerializationContext.serialize(mutxoData.mutxoKeyBytes))
        jsonObject.add("o", jsonSerializationContext.serialize(addressObject))
        jsonObject.add("c", jsonSerializationContext.serialize(mutxoData.currency))
        jsonObject.add("a", jsonSerializationContext.serialize(mutxoData.amount))
        jsonObject.add("s", jsonSerializationContext.serialize(mutxoData.sourceBytes))
        jsonObject.add("st", jsonSerializationContext.serialize(mutxoData.sourceType))
        jsonObject.add("r", jsonSerializationContext.serialize(mutxoData.reference))

        return jsonObject
    }
}

class MeshAddressTxdSerializer : JsonSerializer<MeshAddressTxd> {

    override fun serialize(meshAddressData: MeshAddressTxd, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("p", jsonSerializationContext.serialize(meshAddressData.p))
        jsonObject.add("a", jsonSerializationContext.serialize(meshAddressData.a))
        jsonObject.add("c", jsonSerializationContext.serialize(meshAddressData.c))

        return jsonObject
    }
}

