//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.address.data

import com.vcard.mesh.sdk.MeshConstants

data class MeshAddress(
    var p: String, //prefix
    val a: ByteArray = ByteArray(MeshConstants.MaximumAddressBytesLength), //address
    val c: ByteArray = ByteArray(MeshConstants.MaximumAddressChecksumBytesLength) //checksum
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshAddress

        if (p != other.p) return false
        if (!a.contentEquals(other.a)) return false
        if (!c.contentEquals(other.c)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = p.hashCode()
        result = 31 * result + a.contentHashCode()
        result = 31 * result + c.contentHashCode()
        return result
    }
}

