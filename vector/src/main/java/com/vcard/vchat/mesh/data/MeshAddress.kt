package com.vcard.vchat.mesh.data

import com.vcard.vchat.mesh.Constants

data class MeshAddress(
        var prefix: String,
        val addressData: ByteArray = ByteArray(Constants.MaximumAddressBytesLength),
        val addressCheckSum: ByteArray = ByteArray(Constants.MaximumAddressChecksumBytesLength)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshAddress

        if (prefix != other.prefix) return false
        if (!addressData.contentEquals(other.addressData)) return false
        if (!addressCheckSum.contentEquals(other.addressCheckSum)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.hashCode()
        result = 31 * result + addressData.contentHashCode()
        result = 31 * result + addressCheckSum.contentHashCode()
        return result
    }
}
