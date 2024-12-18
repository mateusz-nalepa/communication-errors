package com.mateusz.nalepa.communicationerrors.timeout

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.SocketTimeoutException
import java.time.Duration.ofSeconds

class ReadSocketTimeout : BaseTest() {

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    fun `default - no specific read timeout`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // and stub
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/offers"))
                .willReturn(
                    aResponse().withFixedDelay(ofSeconds(10).toMillis().toInt())
                )
        )
        // when
        val measureStart = System.currentTimeMillis()

        restClient
            .get()
            .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
            .retrieve()
            .toEntity(String::class.java)

        // then
        val duration = System.currentTimeMillis() - measureStart
        println("Duration is: $duration ms")
    }

    @Test
    fun `read,socket timeout`() {
        // given
        WireMockRunner.start()
        val givenReadTimeoutMillis = 100
        val requestFactory =
            HttpComponentsClientHttpRequestFactory()
                .apply {
                    setReadTimeout(givenReadTimeoutMillis)
                }
        val restClient = restClientBuilder.requestFactory(requestFactory).build()

        // and stub
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/offers"))
                .willReturn(
                    aResponse().withFixedDelay(500)
                )
        )

        // when
        val measureStart = System.currentTimeMillis()

        val resourceAccessException =
            assertThrows<ResourceAccessException> {
                restClient
                    .get()
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
                    .retrieve()
                    .toEntity(String::class.java)
            }

        val socketTimeoutException = resourceAccessException.rootCause as SocketTimeoutException
        socketTimeoutException.message shouldBe "Read timed out"

        val duration = System.currentTimeMillis() - measureStart
        duration shouldBeGreaterThanOrEqual givenReadTimeoutMillis.toLong()
        println("Duration is: $duration ms")
    }

    @Test
    fun `read,socket timeout with initialization`() {
        // given
        WireMockRunner.start()
        val givenReadTimeoutMillis = 100
        val requestFactory =
            HttpComponentsClientHttpRequestFactory()
                .apply {
                    setReadTimeout(givenReadTimeoutMillis)
                }
        val restClient = restClientBuilder.requestFactory(requestFactory).build()

        // and stub
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/offers"))
                .willReturn(
                    aResponse().withFixedDelay(500)
                )
        )

        // when
        // only this is added
        assertThrows<ResourceAccessException> {
            restClient
                .get()
                .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
                .retrieve()
                .toEntity(String::class.java)
        }
        val measureStart = System.currentTimeMillis()

        val resourceAccessException =
            assertThrows<ResourceAccessException> {
                restClient
                    .get()
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
                    .retrieve()
                    .toEntity(String::class.java)
            }

        val socketTimeoutException = resourceAccessException.rootCause as SocketTimeoutException
        socketTimeoutException.message shouldBe "Read timed out"

        val duration = System.currentTimeMillis() - measureStart
        duration shouldBeGreaterThanOrEqual givenReadTimeoutMillis.toLong()
        println("Duration is: $duration ms")
    }

}