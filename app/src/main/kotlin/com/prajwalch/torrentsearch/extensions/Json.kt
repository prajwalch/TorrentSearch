package com.prajwalch.torrentsearch.extensions

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Returns the value of a given key as a [JsonArray]. */
fun JsonObject.getArray(key: String): JsonArray? = this[key]?.asArray()

/** Returns the value of a given key as a [JsonObject]. */
fun JsonObject.getObject(key: String): JsonObject? = this[key]?.asObject()

/** Returns the value of a given key as a [String]. */
fun JsonObject.getString(key: String): String? = this[key]?.asString()

/** Returns the value of a given key as an [UInt]. */
fun JsonObject.getUInt(key: String): UInt? = this[key]?.asUInt()

/** Returns the value of a given key as a [Long]. */
fun JsonObject.getLong(key: String): Long? = this[key]?.asLong()

/** Returns the json element as a [JsonArray]. */
fun JsonElement.asArray(): JsonArray = this.jsonArray

/** Returns the json element as a [JsonObject]. */
fun JsonElement.asObject(): JsonObject = this.jsonObject

/** Returns the json element as string by properly removing the quotes. */
fun JsonElement.asString(): String = this.toString().trim('"')

/** Returns the json element as [Int]. */
fun JsonElement.asInt(): Int = this.jsonPrimitive.int

/** Returns the json element as [UInt]. */
fun JsonElement.asUInt(): UInt = this.asInt().toUInt()

/** Returns the json element as [Long]. */
fun JsonElement.asLong(): Long = this.jsonPrimitive.long