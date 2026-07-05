package com.prajwalch.torrentsearch.torznab

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** An XML parser for the error response. */
class TorznabErrorResponseXmlParser {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null

    fun parse(xml: String): Int {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        return readError()
    }

    private fun readError(): Int {
        parser.require(XmlPullParser.START_TAG, namespace, "error")

        val code = parser.getAttributeValue(null, "code").toInt()

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "error")

        return code
    }
}