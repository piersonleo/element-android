//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.utils

import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import org.apache.commons.codec.binary.Hex
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object CommonUtil{

    fun binToBytes(data: Any): ByteArray{
        val json = Gson().toJson(data)

        return json.toByteArray()
    }

    fun base64StringToHex(string: String):String{
        val decode = BaseEncoding.base64().decode(string)
        return Hex.encodeHexString(decode, true)
    }

    fun isValidJson(string: String): Boolean {
        try {
            JSONObject(string)
        } catch (ex: JSONException) {
            return false
        }
        return true
    }

    fun isValidJsonArray(string: String): Boolean{
        try {
            JSONArray(string)
        } catch (e: JSONException) {
            return false
        }
        return true
    }
}