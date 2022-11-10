package com.vcard.vchat.mesh

import com.vcard.vchat.mesh.data.MeshAddessTxd
import com.vcard.vchat.mesh.data.MeshAddress
import org.apache.tuweni.crypto.SECP256K1
import java.io.ByteArrayOutputStream

enum class PrefixEnum(val prefix: String){
    UndefinedPrefix("m1"),
    DefaultPrefix("m1"),
    VaultPrefix("v1"),
    DogPrefix("g1")
}

object Address {

    /**
     * Create address hash from SECP256K1 public key. Returns byte array of the address hash
     * @param publicKey SECP256K1 public key used to hash
     */
    fun computeAddressHash(publicKey: SECP256K1.PublicKey): ByteArray{

        val rawData = publicKey.bytesArray()
        val outputStream = ByteArrayOutputStream()

        //prefix byte of 4 is needed to be in sync with go-mesh
        val prefixByte = byteArrayOf(4)
        outputStream.write(prefixByte)
        outputStream.write(rawData)

        val hashData = HashUtils.meshHash(outputStream.toByteArray())

        val addressData = ByteArray(Constants.MaximumAddressBytesLength)
        System.arraycopy(hashData, 12, addressData, 0, Constants.MaximumAddressBytesLength)

        return addressData
    }

    /**
     * Construct a mesh address
     * @param prefixEnum The address prefix
     * @param addressData The address hash
     */
    fun setAddress(prefixEnum: PrefixEnum, addressData: ByteArray): MeshAddress{
        val meshAddress = MeshAddress(
                getPrefix(prefixEnum),
                addressData,
                computeAddressChecksum(getPrefix(prefixEnum), addressData)
        )

        return meshAddress
    }

    /**
     * Get prefix string from given PrefixEnum parameter
     * @param prefixEnum The prefix enum to b
     */
    private fun getPrefix(prefixEnum: PrefixEnum): String{
        return when (prefixEnum){
            PrefixEnum. UndefinedPrefix -> "m1"
            PrefixEnum.DefaultPrefix -> "m1"
            PrefixEnum.VaultPrefix -> "v1"
            PrefixEnum.DogPrefix -> "g1"
        }
    }

    /**
     * Construct MeshAddress from given address string. The address string must be the complete address
     * @param fullAddress The address string that'll be constructed into MeshAddress
     */
    fun getMeshAddressFromString(fullAddress: String): MeshAddress{

        if (!isValidMeshAddressString(fullAddress)) throw Exception("$fullAddress is not a valid mesh address string")

        val prefix = fullAddress.substring(0, Constants.MaximumPrefixHexLength)

        val address = fullAddress.substring(Constants.MaximumPrefixHexLength, Constants.MaximumPrefixHexLength+Constants.MaximumAddressHexLength)

        val checksum = fullAddress.substring(Constants.MaximumFullAddressHexLength-Constants.MaximumAddressChecksumHexLength)


        return MeshAddress(
                prefix,
                address.decodeHex(),
                checksum.decodeHex()
        )
    }

    /**
     * Construct MeshAddressTxd from given address string. The address string must be the complete address
     * Use this to create transaction receipt
     * @param fullAddress The address string that'll be constructed into MeshAddress
     */
    fun getMeshAddressTxdFromString(fullAddress: String): MeshAddessTxd{

        if (!isValidMeshAddressString(fullAddress)) throw Exception("$fullAddress is not a valid mesh address string")

        val prefix = fullAddress.substring(0, Constants.MaximumPrefixHexLength)

        val address = fullAddress.substring(Constants.MaximumPrefixHexLength, Constants.MaximumPrefixHexLength+Constants.MaximumAddressHexLength)

        val checksum = fullAddress.substring(Constants.MaximumFullAddressHexLength-Constants.MaximumAddressChecksumHexLength)


        return MeshAddessTxd(
                prefix,
                toUnsignedIntArray(address.decodeHex()),
                toUnsignedIntArray(checksum.decodeHex())
        )
    }

    /**
     * Construct a complete mesh address string from given prefix, address, and checksum
     * @param prefix The prefix of the address
     * @param address The address hash
     * @param checksum The checksum of the prefix and address hash
     */
    fun createFullAddress(prefix: String, address: ByteArray, checksum: ByteArray): String {
        return "${prefix}${address.toHex()}${checksum.toHex()}"
    }

    /**
     * Verify given string is a valid mesh address string
     * @param fullAddress The address string that'll be verified
     */
    fun isValidMeshAddressString(fullAddress: String): Boolean{
        if (fullAddress == "" || fullAddress.length != Constants.MaximumFullAddressHexLength){
            return false
        }

        val prefix = fullAddress.substring(0, Constants.MaximumPrefixHexLength)
        if (!isValidAddressPrefix(prefix)){
            return false
        }

        val address = fullAddress.substring(Constants.MaximumPrefixHexLength, Constants.MaximumPrefixHexLength+Constants.MaximumAddressHexLength)

        if (address.length != Constants.MaximumAddressHexLength){
            return false
        }

        val checksum = fullAddress.substring(Constants.MaximumFullAddressHexLength-Constants.MaximumAddressChecksumHexLength)

        if (checksum.length != Constants.MaximumAddressChecksumHexLength){
            return false
        }

        val addressData = ByteArray(Constants.MaximumAddressBytesLength)
        System.arraycopy(address.decodeHex(), 0, addressData, 0, Constants.MaximumAddressBytesLength)
        if (addressData.size != Constants.MaximumAddressBytesLength){
            return false
        }

        val checksumData = ByteArray(Constants.MaximumAddressChecksumBytesLength)

        System.arraycopy(checksum.decodeHex(), 0, checksumData, 0, Constants.MaximumAddressChecksumBytesLength)

        if (checksumData.size != Constants.MaximumAddressChecksumBytesLength){
            return false
        }

        if (!isValidAddressChecksum(prefix,addressData, checksumData)){
            return false
        }

        return true
    }

    /**
     * Verify given string is a valid mesh address prefix
     * @param prefix The prefix address string that'll be verified
     */
    fun isValidAddressPrefix(prefix: String): Boolean{
        if (prefix != PrefixEnum.DefaultPrefix.prefix && prefix != PrefixEnum.VaultPrefix.prefix && prefix != PrefixEnum.DogPrefix.prefix){
            return false
        }

        return true
    }


    /**
     * Verify whether address checksum is valid
     * @param prefix The prefix address string used to verify the checksum
     * @param address The address hash used to verify the checksum
     * @param checksum The checksum to be verified
     */
    fun isValidAddressChecksum(prefix: String, address: ByteArray, checksum: ByteArray): Boolean {
        if (prefix == "" || address.isEmpty() || checksum.isEmpty()) {
            return false
        }

        val crc = computeAddressChecksum(prefix, address)
        return crc.contentEquals(checksum)
    }

    /**
     * Create address checksum from given prefix and address hash
     * @param prefix The prefix address string used to verify the checksum
     * @param addressData The address hash used to verify the checksum
     */
    private fun computeAddressChecksum(prefix: String, addressData: ByteArray): ByteArray {
        if (prefix == "" || addressData.isEmpty()) {
            return byteArrayOf()
        }

        val prefixByteArray = prefix.toByteArray()

        val hashData = ByteArray(Constants.MaximumPrefixByteLength + Constants.MaximumAddressBytesLength)

        //put prefix into the byte array
        System.arraycopy(prefixByteArray, 0, hashData, 0, Constants.MaximumPrefixByteLength)

        //put address data at the end of the prefix
        System.arraycopy(addressData, 0, hashData, Constants.MaximumPrefixByteLength, Constants.MaximumAddressBytesLength)

        val meshHashData = HashUtils.meshHash(hashData)

        val crc = ByteArray(Constants.MaximumAddressChecksumBytesLength)
        System.arraycopy(meshHashData, 0, crc, 0, Constants.MaximumAddressChecksumBytesLength)

        return crc
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
    }

    private fun toUnsigned(b: Byte): Int {
        return (if (b >= 0) b else 256 + b).toInt()
    }

    private fun toUnsignedIntArray(bytes: ByteArray): IntArray{
        val unsignedIntArray = IntArray(bytes.size)

        for ((index, byte) in bytes.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntArray[index] = unsignedInt
        }

        return unsignedIntArray
    }
}
