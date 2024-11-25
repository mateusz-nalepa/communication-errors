package com.mateusz.nalepa.communicationerrors

import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

class Server5xxTest(
    @LocalServerPort private val port: Int,
    @Autowired private val restClientBuilder: RestClient.Builder,
) : BaseTest() {

    @Test
    fun `expects 500, when getting beer details`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val throwable =
            assertThrows<Throwable> {
                restClient
                    .get()
                    .uri("http://localhost:$port/beers/invalidBeer")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpServerErrorException
        httpClientError.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
    }

}