package com.vcard.vchat.mesh.data

data class MeshServicePayload(
        val c: String,
        val d: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshServicePayload

        if (c != other.c) return false
        if (d != null) {
            if (other.d == null) return false
            if (!d.contentEquals(other.d)) return false
        } else if (other.d != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = c.hashCode()
        result = 31 * result + (d?.contentHashCode() ?: 0)
        return result
    }
}
