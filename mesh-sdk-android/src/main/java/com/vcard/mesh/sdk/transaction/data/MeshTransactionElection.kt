package com.vcard.mesh.sdk.transaction.data

data class MeshTransactionElection(
        val cl: Map<String, String>, //candidateList
        val clsig: IntArray, //candidateList signature
        val vl: Map<String, MeshTransactionVote>, //voteList
        val vlsig: ByteArray //voteList signature
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshTransactionElection

        if (cl != other.cl) return false
        if (!clsig.contentEquals(other.clsig)) return false
        if (vl != other.vl) return false
        if (!vlsig.contentEquals(other.vlsig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cl.hashCode()
        result = 31 * result + clsig.contentHashCode()
        result = 31 * result + vl.hashCode()
        result = 31 * result + vlsig.contentHashCode()
        return result
    }
}

data class MeshTransactionVote(
        val v: String,
        val vsig: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshTransactionVote

        if (v != other.v) return false
        if (!vsig.contentEquals(other.vsig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = v.hashCode()
        result = 31 * result + vsig.contentHashCode()
        return result
    }
}
