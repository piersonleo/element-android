package com.vcard.vchat.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object StringUtil {

    fun formatBalanceForDisplay(value: Double): String {
        val df = DecimalFormat("###,###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        df.maximumFractionDigits = 340 //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

        return df.format(value)
    }

    fun formatBalanceForDisplayBigDecimal(value: BigDecimal): String {
        val df = DecimalFormat("###,###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        df.maximumFractionDigits = 340 //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

        return df.format(value)
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

    //To Ascii
    fun String.decodeHex(): String {
        require(length % 2 == 0) {"Must have an even length"}
        return String(
                chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
        )
    }
}
