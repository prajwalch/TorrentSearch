package com.prajwalch.torrentsearch.extension

import org.xmlpull.v1.XmlPullParser

/** Reads the current tag and calls the callback on each child tag. */
fun XmlPullParser.readParentTag(tagName: String, onStartTag: () -> Unit) {
    this.require(XmlPullParser.START_TAG, namespace, tagName)

    while (this.next() != XmlPullParser.END_TAG) {
        if (this.eventType != XmlPullParser.START_TAG) {
            continue
        }
        onStartTag()
    }
}

/** Skips the current tag including all of its child. */
fun XmlPullParser.skipCurrentTag() {
    if (this.eventType != XmlPullParser.START_TAG) {
        return
    }

    var depth = 1

    while (depth != 0) {
        when (this.next()) {
            XmlPullParser.END_TAG -> depth -= 1
            XmlPullParser.START_TAG -> depth += 1
        }
    }
}