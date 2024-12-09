package com.mateusz.nalepa.communicationerrors.httpstatuscodes

import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.api.BuyBeerRequest
import com.mateusz.nalepa.communicationerrors.api.BuyBeerResponse
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class Client2xxTest(
    @LocalServerPort private val port: Int,
    @Autowired private val restClientBuilder: RestClient.Builder,
) : BaseTest() {

    @Test
    fun `expects 200`() {
        // given
        WireMockRunner.start()
        val restClient = restClientBuilder.build()

        // when
        val request = BuyBeerRequest(age = 20)

        val response =
                restClient
                    .post()
                    .uri("http://localhost:$port/beer/buy")
                    .body(request)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(BuyBeerResponse::class.java)

        // then
        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe BuyBeerResponse("Beer Granted!")
    }

}