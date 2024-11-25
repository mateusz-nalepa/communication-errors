package com.mateusz.nalepa.communicationerrors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.apache.hc.client5.http.HttpHostConnectException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.SocketTimeoutException
import java.time.Duration.ofHours

class TimeoutsTests : BaseTest() {

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    // in normal world, it can last a moment before returning an error
    fun `connection error`() {
        // given
        val restClient = restClientBuilder.build()

        // when
        val resourceAccessException =
            assertThrows<ResourceAccessException> {
                restClient
                    .get()
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val socketTimeoutException = resourceAccessException.rootCause as HttpHostConnectException
        socketTimeoutException.message shouldContain "Connection refused"
    }

    @Test
    // commented, cause it lasts 1 hour :D
    fun `default - read timeout`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // and stub
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/oferty"))
                .willReturn(
                    aResponse().withFixedDelay(ofHours(1).toMillis().toInt())
                )
        )
        // when
        restClient
            .get()
            .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
            .retrieve()
            .toEntity(String::class.java)
    }

//    @Test
//    // don't know why it doesn't work
//    // @Timeout(value = 5, unit = TimeUnit.SECONDS)
//    fun `with defaults and test timeout`() {
//        val executor = Executors.newSingleThreadExecutor()
//        val givenTimeoutSeconds = 5L
//        val restClient = restClientBuilder.build()
//
//        // and stub
//        WireMockRunner.wireMockServer.stubFor(
//            get(urlEqualTo("/oferty"))
//                .willReturn(
//                    aResponse().withFixedDelay(ofHours(1).toMillis().toInt())
//                )
//        )
//        // when
//        val callableTask =
//            Callable {
//                restClient
//                    .get()
//                    .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
//                    .retrieve()
//                    .toEntity(String::class.java)
//            }
//        val future: Future<ResponseEntity<String>> = executor.submit(callableTask)
//
//
//        var timeoutOccurred = false
//        val measureStart = System.currentTimeMillis()
//
//        try {
//            future.get(givenTimeoutSeconds, TimeUnit.SECONDS)
//        } catch (t: TimeoutException) {
//            timeoutOccurred = true
//            future.cancel(true)
//        } finally {
//            executor.shutdownNow()
//        }
//        timeoutOccurred shouldBe true
//
//        val duration = System.currentTimeMillis() - measureStart
//        duration shouldBeGreaterThanOrEqual givenTimeoutSeconds
//        println("Duration is: $duration ms")
//    }

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
            get(urlEqualTo("/oferty"))
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
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
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
            get(urlEqualTo("/oferty"))
                .willReturn(
                    aResponse().withFixedDelay(500)
                )
        )

        // when
        // only this is added
        assertThrows<ResourceAccessException> {
            restClient
                .get()
                .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
                .retrieve()
                .toEntity(String::class.java)
        }
        val measureStart = System.currentTimeMillis()

        val resourceAccessException =
            assertThrows<ResourceAccessException> {
                restClient
                    .get()
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/oferty")
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
    fun `ADVANCED - connection request timeout`() {}

}
