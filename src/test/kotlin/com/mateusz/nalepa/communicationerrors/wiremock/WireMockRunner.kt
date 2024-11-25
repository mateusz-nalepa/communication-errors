package com.mateusz.nalepa.communicationerrors.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

object WireMockRunner {

    const val wireMockPort = 10200

    val wireMockServer =
        WireMockServer(
            options()
                .port(wireMockPort)
        )

    fun start() {
        if (!wireMockServer.isRunning) {
            wireMockServer.start()
        }
    }

    fun reset() {
        wireMockServer.resetAll()
    }

}