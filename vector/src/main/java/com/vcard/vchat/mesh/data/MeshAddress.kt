package com.vcard.vchat.mesh.data

import com.vcard.vchat.mesh.Constants

data class MeshAddress(
        var p: String, //prefix
        val a: ByteArray = ByteArray(Constants.MaximumAddressBytesLength), //address
        val c: ByteArray = ByteArray(Constants.MaximumAddressChecksumBytesLength) //checksum
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

data class MeshAddessTxd(
        var p: String, //prefix
        val a: IntArray, //address
        val c: IntArray //checksum
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshAddessTxd

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
