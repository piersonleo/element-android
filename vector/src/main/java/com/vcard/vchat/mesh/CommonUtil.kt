package com.vcard.vchat.mesh

import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import org.apache.commons.codec.binary.Hex

class CommonUtil{

    fun binToBytes(data: Any): ByteArray{
        val json = Gson().toJson(data)

        return json.toByteArray()
    }

    fun base64StringToHex(string: String):String{
        val decode = BaseEncoding.base64().decode(string)
        return Hex.encodeHexString(decode, true)
    }
}
