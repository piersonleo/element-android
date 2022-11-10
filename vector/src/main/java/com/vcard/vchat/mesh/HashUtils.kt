package com.vcard.vchat.mesh

import org.komputing.khash.keccak.Keccak
import org.komputing.khash.keccak.KeccakParameter

object HashUtils {

    //hash a byte array data with Keccak
    fun meshHash(rawData: ByteArray): ByteArray{
        val hashBytes = Keccak.digest(rawData, KeccakParameter.SHA3_256)
        return hashBytes
    }

    //create a checksum by hashing the byte array data
    fun computeChecksum(rawData: ByteArray): ByteArray{
        val meshHashData = meshHash(rawData)

        val crc = ByteArray(Constants.MaximumAddressChecksumBytesLength)
        System.arraycopy(meshHashData, 0, crc, 0, Constants.MaximumAddressChecksumBytesLength)

        return crc
    }
}
