package com.vcard.vchat.mesh

import org.json.JSONObject

object Payload {

    fun parsePayloadResponse(payload: ByteArray): JSONObject {
        val decodedPayloadResp = payload.decodeToString()

        return JSONObject(decodedPayloadResp)
    }


}
