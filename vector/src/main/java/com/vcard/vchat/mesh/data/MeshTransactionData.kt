package com.vcard.vchat.mesh.data

import com.google.gson.JsonArray
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
        val c: String, //currency
       // val n: Long,
        val t: Int, //type
        val r: String, //recipientAddress
        val amt: BigInteger,
        val fee: BigInteger,
        val ref: String,
        val utc: String, //utcTimestamp
        val mxi: List<MUnspentTransactionObjectDataTxd> //mutxoInput
)

class MeshTransactionDataSerializer : JsonSerializer<MeshTransactionData> {

    override fun serialize(meshTransactionData: MeshTransactionData, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {

        val mutxoList: ArrayList<JsonObject> = ArrayList()
        for (mutxo in meshTransactionData.mxi) {
            val addressObject = JsonObject()
            addressObject.add("p", jsonSerializationContext.serialize(mutxo.ownerAddress!!.p))
            addressObject.add("a", jsonSerializationContext.serialize(mutxo.ownerAddress!!.a))
            addressObject.add("c", jsonSerializationContext.serialize(mutxo.ownerAddress!!.c ))

            val mutxoObject = JsonObject()
            mutxoObject.add("k", jsonSerializationContext.serialize(mutxo.mutxoKeyBytes))
            mutxoObject.add("o", jsonSerializationContext.serialize(addressObject))
            mutxoObject.add("c", jsonSerializationContext.serialize(mutxo.currency))
            mutxoObject.add("a", jsonSerializationContext.serialize(mutxo.amount))
            mutxoObject.add("s", jsonSerializationContext.serialize(mutxo.sourceBytes))
            mutxoObject.add("st", jsonSerializationContext.serialize(mutxo.sourceType))
            mutxoObject.add("r", jsonSerializationContext.serialize(mutxo.reference))

            mutxoList.add(mutxoObject)
        }

        val jsonObject = JsonObject()
        jsonObject.add("s", jsonSerializationContext.serialize(meshTransactionData.s))
        jsonObject.add("c", jsonSerializationContext.serialize(meshTransactionData.c))
        //jsonObject.add("n", jsonSerializationContext.serialize(meshTransactionData.n))
        jsonObject.add("t", jsonSerializationContext.serialize(meshTransactionData.t))
        jsonObject.add("r", jsonSerializationContext.serialize(meshTransactionData.r))
        jsonObject.add("amt", jsonSerializationContext.serialize(meshTransactionData.amt))
        jsonObject.add("fee", jsonSerializationContext.serialize(meshTransactionData.fee))
        jsonObject.add("ref", jsonSerializationContext.serialize(meshTransactionData.ref))
        jsonObject.add("utc", jsonSerializationContext.serialize(meshTransactionData.utc))
        jsonObject.add("mxi", jsonSerializationContext.serialize(mutxoList))

        return jsonObject
    }
}
