//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.utils

import com.vcard.mesh.sdk.MeshConstants
import org.komputing.khash.keccak.Keccak
import org.komputing.khash.keccak.KeccakParameter

object HashUtil {

    //hash a byte array data with Keccak
    fun meshHash(rawData: ByteArray): ByteArray{
        val hashBytes = Keccak.digest(rawData, KeccakParameter.SHA3_256)
        return hashBytes
    }

    //create a checksum by hashing the byte array data
    fun computeChecksum(rawData: ByteArray): ByteArray{
        val meshHashData = meshHash(rawData)

        val crc = ByteArray(MeshConstants.MaximumAddressChecksumBytesLength)
        System.arraycopy(meshHashData, 0, crc, 0, MeshConstants.MaximumAddressChecksumBytesLength)

        return crc
    }
}
