package com.vcard.vchat.mesh.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

data class MeshTransactionReceipt(
        val v: Int = 1, //version
        val d: ByteArray, //txnData
        val dsig: IntArray, //txnData signature
        val f: ByteArray, //fee
        val fsig: ByteArray, //fee signature
        val e: IntArray, //election
        val esig: ByteArray //election signaturre
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshTransactionReceipt

        if (v != other.v) return false
        if (!d.contentEquals(other.d)) return false
        if (!dsig.contentEquals(other.dsig)) return false
        if (!f.contentEquals(other.f)) return false
        if (!fsig.contentEquals(other.fsig)) return false
        if (!e.contentEquals(other.e)) return false
        if (!esig.contentEquals(other.esig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = v
        result = 31 * result + d.contentHashCode()
        result = 31 * result + dsig.contentHashCode()
        result = 31 * result + f.contentHashCode()
        result = 31 * result + fsig.contentHashCode()
        result = 31 * result + e.contentHashCode()
        result = 31 * result + esig.contentHashCode()
        return result
    }
}

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
