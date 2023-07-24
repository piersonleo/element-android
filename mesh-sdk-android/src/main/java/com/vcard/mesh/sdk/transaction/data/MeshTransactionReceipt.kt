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
import java.lang.reflect.Type

data class MeshTransactionReceipt(
        val v: Int = 1, //version
        val d: MeshTransactionData, //txnData
        val dsig: IntArray, //txnData signature
        val f: MeshTransactionFee, //fee
        val fsig: ByteArray, //fee signature
        val e: MeshTransactionElection, //election
        val esig: ByteArray //election signature
)

class MeshTransactionReceiptSerializer : JsonSerializer<MeshTransactionReceipt> {

    override fun serialize(meshTransactionReceipt: MeshTransactionReceipt, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("v", jsonSerializationContext.serialize(meshTransactionReceipt.v))
        jsonObject.add("d", jsonSerializationContext.serialize(meshTransactionReceipt.d))
        jsonObject.add("dsig", jsonSerializationContext.serialize(meshTransactionReceipt.dsig))
        jsonObject.add("f", jsonSerializationContext.serialize(meshTransactionReceipt.f))
        jsonObject.add("fsig", jsonSerializationContext.serialize(meshTransactionReceipt.fsig))
        jsonObject.add("e", jsonSerializationContext.serialize(meshTransactionReceipt.e))
        jsonObject.add("esig", jsonSerializationContext.serialize(meshTransactionReceipt.esig))

        return jsonObject
    }
}
