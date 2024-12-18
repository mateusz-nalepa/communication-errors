package com.mateusz.nalepa.communicationerrors.timeout

import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.string.shouldContain
import org.apache.hc.client5.http.HttpHostConnectException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

class ConnectTimeout : BaseTest() {

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    // in normal world, it can last a moment before returning an error
    fun `connection error`() {
        // given
        val requestFactory =
            HttpComponentsClientHttpRequestFactory()
                .apply {
                    setConnectTimeout(100)
                }
        val restClient = restClientBuilder.requestFactory(requestFactory).build()

        // when
        val resourceAccessException =
            assertThrows<ResourceAccessException> {
                restClient
                    .get()
                    // There is no server which accepts connections
                    // We are trying to reach some non-existing server
                    .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
                    .retrieve()
                    .toEntity(String::class.java)
            }

        // then
        val socketTimeoutException = resourceAccessException.rootCause as HttpHostConnectException
        socketTimeoutException.message shouldContain "Connection refused"
    }


}
