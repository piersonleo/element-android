package com.vcard.vchat.mesh.data

import com.vcard.vchat.mesh.Constants
import java.math.BigInteger

data class MeshCurrency(
        val currencyCode: String,
        val meshFeePercentage: BigInteger,
        val meshFeeCap: BigInteger,
        val meshFeeMinimum: BigInteger,
        val communityFeePercentage: BigInteger,
        val communityFeeCap: BigInteger,
        val communityFeeMinimum: BigInteger,
        val nodeFeePercentage: BigInteger,
        val nodeFeeCap: BigInteger,
        val nodeFeeMinimum: BigInteger
)
