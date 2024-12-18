package com.mateusz.nalepa.communicationerrors.bulkhead_isolation

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

class DifferentHttpClientTest : BaseTest() {

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    fun `different services has different clients`() {
        // given
        WireMockRunner.start()
        val restClientForPersonalizedOffers =
            restClientBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory().apply { setReadTimeout(500) })
                .build()

        val restClientForMostRecentOffers =
            restClientBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory().apply { setReadTimeout(200) })
                .build()

        // and stub
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/personalized-offers"))
                .willReturn(
                    aResponse().withFixedDelay(300).withBody("personalizedOffers")
                )
        )

        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/most-recent-offers"))
                .willReturn(
                    aResponse().withFixedDelay(100).withBody("mostRecentOffers")
                )
        )

        // when
        val personalizedOffers =
            restClientForPersonalizedOffers
                .get()
                .uri("http://localhost:${WireMockRunner.wireMockPort}/personalized-offers")
                .retrieve()
                .toEntity(String::class.java)
        personalizedOffers.statusCode shouldBe HttpStatus.OK
        println(personalizedOffers.body)

        val mostRecentOffers =
            restClientForMostRecentOffers
                .get()
                .uri("http://localhost:${WireMockRunner.wireMockPort}/most-recent-offers")
                .retrieve()
                .toEntity(String::class.java)
        mostRecentOffers.statusCode shouldBe HttpStatus.OK
        println(mostRecentOffers.body)
    }

}