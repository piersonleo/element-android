//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.transaction.data

import com.vcard.mesh.sdk.mutxo.data.Mutxo
import java.math.BigInteger

data class MeshTransactionFee (
        val m: String? = null,
        val mf: BigInteger? = null,
        val c: String? = null,
        val cf: BigInteger? = null,
        val n: String? = null,
        val nf: BigInteger? = null,
        val mxo: Array<Mutxo>? = null,
        val utc: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshTransactionFee

        if (m != other.m) return false
        if (mf != other.mf) return false
        if (c != other.c) return false
        if (cf != other.cf) return false
        if (n != other.n) return false
        if (nf != other.nf) return false
        if (!mxo.contentEquals(other.mxo)) return false
        if (utc != other.utc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = m.hashCode()
        result = 31 * result + mf.hashCode()
        result = 31 * result + c.hashCode()
        result = 31 * result + cf.hashCode()
        result = 31 * result + n.hashCode()
        result = 31 * result + nf.hashCode()
        result = 31 * result + mxo.contentHashCode()
        result = 31 * result + utc.hashCode()
        return result
    }
}

