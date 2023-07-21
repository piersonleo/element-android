//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.mutxo.data

import com.vcard.mesh.sdk.address.data.MeshAddress
import java.math.BigInteger

data class Mutxo(
        val k :String, //mutxoKey
        val o: MeshAddress, //ownerAddress
        val c: String, //currency
        val a: Long, //amount
        val s: String, //source
        val st: BigInteger, //sourceType
        val r: String //reference
)
