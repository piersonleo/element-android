package com.vcard.vchat.mesh.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.math.BigInteger

// Transaction data sent by sender.
data class MeshTransactionData (
        val s: String, //sender
        val c: String,
        val n: Long,
        val t: Int, //type
        val r: String,
        val amt: Long,
        val fee: BigInteger,
        val ref: String
)


class MeshTransactionDataSerializer : JsonSerializer<MeshTransactionData> {

    override fun serialize(meshTransactionData: MeshTransactionData, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("s", jsonSerializationContext.serialize(meshTransactionData.s))
        jsonObject.add("c", jsonSerializationContext.serialize(meshTransactionData.c))
        jsonObject.add("n", jsonSerializationContext.serialize(meshTransactionData.n))
        jsonObject.add("t", jsonSerializationContext.serialize(meshTransactionData.t))
        jsonObject.add("r", jsonSerializationContext.serialize(meshTransactionData.r))
        jsonObject.add("amt", jsonSerializationContext.serialize(meshTransactionData.amt))
        jsonObject.add("fee", jsonSerializationContext.serialize(meshTransactionData.fee))
        jsonObject.add("ref", jsonSerializationContext.serialize(meshTransactionData.ref))

        return jsonObject
    }
}
