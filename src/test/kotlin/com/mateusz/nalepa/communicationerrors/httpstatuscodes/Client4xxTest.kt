package com.mateusz.nalepa.communicationerrors.httpstatuscodes

import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

class Client4xxTest(
    @LocalServerPort private val port: Int,
    @Autowired private val restClientBuilder: RestClient.Builder,
) : BaseTest() {

    @Test
    fun `expects 400, due to bad request`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val request =
            """
                age
                10
            """.trimIndent()

        val throwable =
            assertThrows<Throwable> {
                restClient
                    .post()
                    .uri("http://localhost:$port/beer/buy")
                    .body(request)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpClientErrorException
        httpClientError.statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `expects 404, due to not found`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val throwable =
            assertThrows<Throwable> {
                restClient
                    .get()
                    .uri("http://localhost:$port/beers/someNonExistingBeerId")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpClientErrorException
        httpClientError.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `expects 405, due to method not allowed`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val throwable =
            assertThrows<Throwable> {
                restClient
                    .post()
                    .uri("http://localhost:$port/beers")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpClientErrorException
        httpClientError.statusCode shouldBe HttpStatus.METHOD_NOT_ALLOWED
    }


    @Test
    fun `expects 415, due to unsupported media type`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val request =
            """
                <person>
                    <age>10</age>
                </person>
            """.trimIndent()

        val throwable =
            assertThrows<Throwable> {
                restClient
                    .post()
                    .uri("http://localhost:$port/beer/buy")
                    .body(request)
                    .accept(MediaType.APPLICATION_XML)
                    .contentType(MediaType.APPLICATION_XML)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpClientErrorException
        httpClientError.statusCode shouldBe HttpStatus.UNSUPPORTED_MEDIA_TYPE
    }

    @Test
    fun `expects 422, due to underage person`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val request =
            """
                {
                    "age": 10
                }
            """.trimIndent()

        val throwable =
            assertThrows<Throwable> {
                restClient
                    .post()
                    .uri("http://localhost:$port/beer/buy")
                    .body(request)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val httpClientError = throwable as HttpClientErrorException
        httpClientError.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
    }

}