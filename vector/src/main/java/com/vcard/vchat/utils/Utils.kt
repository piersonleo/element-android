package com.vcard.vchat.utils

import android.content.Context
import java.io.IOException

class Utils {
    companion object{
        fun removeUrlSuffix(param: String?): String?{
            if (param == null){
                return null
            }
            return param.removeSuffix(Constants.URL_SUFFIX)
        }

        fun getJsonDataFromAsset(context: Context, fileName: String): String {
            val jsonString: String
            try {
                jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                return ""
            }
            return jsonString
        }
    }
}
