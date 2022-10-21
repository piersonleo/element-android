package com.vcard.vchat.mesh.data

import java.math.BigInteger

data class MutxoList(
        val o: MeshAddress, //ownerAddress
        val t: BigInteger, //total
        val l: Map<String, MUnspentTransactionObject> //mutxoList
)
