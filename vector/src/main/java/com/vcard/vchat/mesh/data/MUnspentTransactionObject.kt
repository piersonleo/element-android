package com.vcard.vchat.mesh.data

import com.vcard.vchat.mesh.CurrencyEnum
import java.math.BigInteger

data class MUnspentTransactionObject(
        val k :String, //mutxoKey
        val o: MeshAddress, //ownerAddress
        val c: String, //currency
        val a: Long, //amount
        val s: String, //source
        val st: BigInteger, //sourceType
        val r: String //reference
)
