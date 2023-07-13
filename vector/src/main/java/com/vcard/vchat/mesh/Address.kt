package com.vcard.vchat.mesh

import com.vcard.vchat.mesh.data.MeshAddress
import org.apache.tuweni.crypto.SECP256K1
import timber.log.Timber
import java.io.ByteArrayOutputStream

enum class PrefixEnum(val prefix: String){
    UndefinedPrefix("m1"),
    DefaultPrefix("m1"),
    VaultPrefix("v1"),
    DogPrefix("g1")
}

object Address {


    fun computeAddressHash(publicKey: SECP256K1.PublicKey): ByteArray{

        val rawData = publicKey.bytesArray()
        val outputStream = ByteArrayOutputStream()
        val prefixByte = byteArrayOf(4)
        outputStream.write(prefixByte)
        outputStream.write(rawData)

        val hashData = HashUtils.meshHash(outputStream.toByteArray())

        val addressData = ByteArray(Constants.MaximumAddressBytesLength)
        System.arraycopy(hashData, 12, addressData, 0, Constants.MaximumAddressBytesLength)

        return addressData
    }

    fun setAddress(prefixEnum: PrefixEnum, addressData: ByteArray): MeshAddress{
        val meshAddress = MeshAddress(
                getPrefix(prefixEnum),
                addressData,
                computeAddressChecksum(getPrefix(prefixEnum), addressData)
        )

        return meshAddress
    }

    private fun getPrefix(prefixEnum: PrefixEnum): String{
        return when (prefixEnum){
            PrefixEnum. UndefinedPrefix -> "m1"
            PrefixEnum.DefaultPrefix -> "m1"
            PrefixEnum.VaultPrefix -> "v1"
            PrefixEnum.DogPrefix -> "g1"
        }
    }

    fun getMeshAddressFromString(fullAddress: String): MeshAddress{


        val prefix = fullAddress.substring(0, Constants.MaximumPrefixHexLength)

        val address = fullAddress.substring(Constants.MaximumPrefixHexLength, Constants.MaximumPrefixHexLength+Constants.MaximumAddressHexLength)

        val checksum = fullAddress.substring(Constants.MaximumFullAddressHexLength-Constants.MaximumAddressChecksumHexLength)


        return MeshAddress(
                prefix,
                address.decodeHex(),
                checksum.decodeHex()
        )
    }

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

    fun isValidAddressPrefix(prefix: String): Boolean{
        if (prefix != PrefixEnum.DefaultPrefix.prefix && prefix != PrefixEnum.VaultPrefix.prefix && prefix != PrefixEnum.DogPrefix.prefix){
            return false
        }

        return true
    }


    // Verify checksum of a raw address.
    fun isValidAddressChecksum(prefix: String, address: ByteArray, checksum: ByteArray): Boolean {
        if (prefix == "" || address.isEmpty() || checksum.isEmpty()) {
            return false
        }

        val crc = computeAddressChecksum(prefix, address)
        return crc.contentEquals(checksum)
    }

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

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
    }
}
