package com.vcard.vchat.mesh

import org.komputing.khash.keccak.Keccak
import org.komputing.khash.keccak.KeccakParameter

object HashUtils {
    fun meshHash(rawData: ByteArray): ByteArray{
        val hashBytes = Keccak.digest(rawData, KeccakParameter.SHA3_256)
        return hashBytes
    }

    fun computeChecksum(rawData: ByteArray): ByteArray{
        val meshHashData = meshHash(rawData)

        val crc = ByteArray(Constants.MaximumAddressChecksumBytesLength)
        System.arraycopy(meshHashData, 0, crc, 0, Constants.MaximumAddressChecksumBytesLength)

        return crc
    }
}
