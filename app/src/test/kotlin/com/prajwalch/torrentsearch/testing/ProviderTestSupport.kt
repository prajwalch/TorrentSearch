package com.prajwalch.torrentsearch.testing

import com.prajwalch.torrentsearch.network.NetworkClient

import io.mockk.coEvery
import io.mockk.mockk

/**
 * Reads a test fixture from `src/test/resources/fixtures/<path>`.
 */
fun fixture(path: String): String {
    val resource = Thread.currentThread().contextClassLoader!!.getResource("fixtures/$path")
        ?: error("Fixture not found: fixtures/$path")
    return resource.readText()
}

/**
 * Creates a [NetworkClient] whose [NetworkClient.getText] serves the given
 * response bodies keyed by exact request URL.
 *
 * Requesting any other URL fails the test, so a test also detects requests
 * a provider was not expected to make.
 */
fun fakeNetworkClient(vararg responses: Pair<String, String>): NetworkClient {
    val responsesByUrl = responses.toMap()
    val networkClient = mockk<NetworkClient>()

    coEvery { networkClient.getText(any(), any()) } answers {
        val url = firstArg<String>()
        responsesByUrl[url] ?: error("Unexpected request: $url\nStubbed: ${responsesByUrl.keys}")
    }

    return networkClient
}
