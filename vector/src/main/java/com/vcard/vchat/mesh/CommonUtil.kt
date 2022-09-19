package com.vcard.vchat.mesh

import com.google.gson.Gson

class CommonUtil{

    fun binToBytes(data: Any): ByteArray{
        val json = Gson().toJson(data)

        return json.toByteArray()
    }
}
