package com.vcard.vchat.utils

import android.content.Context
import com.vcard.mesh.sdk.crypto.Aes256
import com.vcard.mesh.sdk.utils.NumberUtil
import com.vcard.vchat.utils.StringUtil.decodeHex
import im.vector.app.R
import java.lang.Exception

class MeshSharedPref(private val context: Context) {

    //Key -> account address
    //Encrypt value before storing
    fun storePp(key: String, value: String){
        val sharedPref = context.getSharedPreferences(context.getString(R.string.mesh_shared_pref_key), Context.MODE_PRIVATE) ?: return
        val launchPp = GlobalData.launchPp
        val encryptValue = Aes256.encryptGcm(value.toByteArray(), launchPp)

        with (sharedPref.edit()) {
            putString(key, NumberUtil.bytesToHexStr(encryptValue))
            apply()
        }
    }

    //get decrypted value
    fun getPp(key: String): String{
        val sharedPref = context.getSharedPreferences(context.getString(R.string.mesh_shared_pref_key), Context.MODE_PRIVATE) ?: throw Exception("No shared pref found")
        val defaultValue = ""
        var pp = sharedPref.getString(key, defaultValue)
        if (pp != ""){
            val launchPp = GlobalData.launchPp
            val decryptValue = Aes256.decryptGcm(NumberUtil.hexStrToBytes(pp), launchPp)
            pp = decryptValue.decodeHex()
        }
        return pp
    }

    fun clearSp() {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.mesh_shared_pref_key), Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }
}
