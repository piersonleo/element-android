//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.currency.data

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
