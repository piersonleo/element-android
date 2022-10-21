package com.vcard.vchat.mesh

import com.vcard.vchat.mesh.data.MeshQrCode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.lang.Error
import java.lang.Exception
import java.lang.IllegalArgumentException

object QrCode {

    private val ns: String? = null

    //version 1
    fun generateQrCodeContent(address: String, accountName: String): String {

        return "<mesh>\n" +
                "<version>1.0</version>\n" +
                "<name>$accountName</name>\n" +
                "<address>$address</address>\n" +
                "</mesh>"
    }

    //version 1
    fun parseQrCodeContent(qrCodeContent: String): MeshQrCode{

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            xpp.setInput(StringReader(qrCodeContent))

            //Skip START_DOCUMENT
            xpp.next()

            //validate whether qr code is from mesh
            if (xpp.eventType == XmlPullParser.START_TAG && xpp.name == "mesh") {
                Timber.d("valid start qr")

                xpp.require(XmlPullParser.START_TAG, ns, "mesh")
                var version: String? = null
                var name: String? = null
                var address: String? = null
                while (xpp.next() != XmlPullParser.END_TAG) {
                    if (xpp.eventType != XmlPullParser.START_TAG) {
                        continue
                    }

                    when (xpp.name) {
                        "version" -> version = readTextByTag(xpp, "version")
                        "name" -> name = readTextByTag(xpp, "name")
                        "address" -> address = readTextByTag(xpp, "address")
                        else -> skip(xpp)
                    }
                }


                if (xpp.eventType == XmlPullParser.END_TAG && xpp.name == "mesh") {
                    if (version != null && name != null && address != null) {
                        Timber.d("valid end qr")
                        return MeshQrCode(version, name, address)
                    } else {
                        throw IllegalArgumentException("Not a valid Mesh QR code")
                    }
                } else {
                    throw IllegalArgumentException("Not a valid Mesh QR code")
                }
            } else {
                Timber.d("invalid mesh qr")
                throw IllegalArgumentException("Not a valid Mesh QR code")
            }
        }catch (e: Exception){
            throw IllegalArgumentException("Not a valid Mesh QR code")
        }
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTextByTag(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, ns, tag)
        val text = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, tag)
        return text
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}
