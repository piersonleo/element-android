package com.vcard.vchat.mesh.data

import java.math.BigInteger

data class MeshTransactionFee (
        val meshAccountAddress: String,
        val meshFee: BigInteger,
        val meshCommunityAddress: String,
        val communityFee: BigInteger,
        val nodeAddress: String,
        val nodeFee: BigInteger,
        val timestamp: String
)
