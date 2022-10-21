package com.vcard.vchat.mesh.data

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
