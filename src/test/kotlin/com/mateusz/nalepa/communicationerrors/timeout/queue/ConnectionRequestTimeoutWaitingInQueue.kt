package com.mateusz.nalepa.communicationerrors.timeout.queue

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import org.apache.hc.client5.http.classic.ExecChain
import org.apache.hc.client5.http.classic.ExecChainHandler
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Duration.ofMillis
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ConnectionRequestTimeoutWaitingInQueue : BaseTest() {

    @Autowired
    lateinit var restClientBuilder: RestClient.Builder

    @Test
    fun `connection request timeout happened`() {
        // given
        val responseTimeFromService = 50
        val givenConnectionRequestTimeout = responseTimeFromService - 30
        val givenNumberOfConnections = 2
        val givenNumberOfRequests = givenNumberOfConnections * 2

        val connectionManager =
            PoolingHttpClientConnectionManager().apply {
                this.maxTotal = givenNumberOfConnections
            }

        val httpClient =
            HttpClients
                .custom()
                .disableAutomaticRetries()
                .setConnectionManager(connectionManager)
                .build()

        val requestFactory =
            HttpComponentsClientHttpRequestFactory(httpClient)
                .apply {
                    setReadTimeout(500)
                    setConnectTimeout(500)
                    setConnectionRequestTimeout(givenConnectionRequestTimeout)
                }


        val restClient = restClientBuilder.requestFactory(requestFactory).build()


        WireMockRunner.start()
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/offers"))
                .willReturn(
                    aResponse().withFixedDelay(ofMillis(responseTimeFromService.toLong()).toMillis().toInt())
                )
        )

        val executorSlow = Executors.newFixedThreadPool(givenNumberOfRequests)
        val futures = mutableListOf<Future<String>>()

        // warmup
        executeRq(restClient)
        println("WARMUP DONE")

        // when
        (1..givenNumberOfRequests).map { number ->
            futures.add(CompletableFuture.supplyAsync({
                val measureStart = System.currentTimeMillis()
                executeRq(restClient)
                    .also {
                        val duration = System.currentTimeMillis() - measureStart
                        println("Processing requestNumber $number took $duration ms")
                    }
            }, executorSlow))
        }

        val thread = Thread {
            futures.forEach { it.get(1, TimeUnit.DAYS) }
        }
        thread.start()

        Thread.sleep(Duration.ofSeconds(10))
    }

    @Test
    fun `connection request timeout - we are waiting in queue`() {
        // given
        val responseTimeFromService = 50
        val givenConnectionRequestTimeout = responseTimeFromService * 2
        val givenNumberOfConnections = 2
        val givenNumberOfRequests = givenNumberOfConnections * 2

        val connectionManager =
            PoolingHttpClientConnectionManager().apply {
                this.maxTotal = givenNumberOfConnections
            }

        val singleRequestChainHandler = object : ExecChainHandler {
            override fun execute(
                request: ClassicHttpRequest?,
                scope: ExecChain.Scope?,
                chain: ExecChain?
            ): ClassicHttpResponse {
                val measureStart = System.currentTimeMillis()
                return chain!!.proceed(request, scope)
                    .also {
                        val duration = System.currentTimeMillis() - measureStart
                        println("Single request took: $duration ms")
                    }
            }
        }

        val httpClient =
            HttpClients
                .custom()
                .disableAutomaticRetries()
                .setConnectionManager(connectionManager)
                .addExecInterceptorLast("singleRequestHandler", singleRequestChainHandler)
                .build()

        val requestFactory =
            HttpComponentsClientHttpRequestFactory(httpClient)
                .apply {
                    setReadTimeout(500)
                    setConnectTimeout(500)
                    setConnectionRequestTimeout(givenConnectionRequestTimeout)
                }

        val restClient =
            restClientBuilder
                .requestFactory(requestFactory)
                .build()



        WireMockRunner.start()
        WireMockRunner.wireMockServer.stubFor(
            get(urlEqualTo("/offers"))
                .willReturn(
                    aResponse().withFixedDelay(ofMillis(responseTimeFromService.toLong()).toMillis().toInt())
                )
        )

        val executorSlow = Executors.newFixedThreadPool(givenNumberOfRequests)
        val futures = mutableListOf<Future<String>>()

        // warmup
        executeRq(restClient)
        println("WARMUP DONE")

        // when
        (1..givenNumberOfRequests).map { number ->
            futures.add(CompletableFuture.supplyAsync({
                val measureStart = System.currentTimeMillis()
                executeRq(restClient)
                    .also {
                        val duration = System.currentTimeMillis() - measureStart
                        println("Processing requestNumber $number with queue wait took $duration ms")
                    }
            }, executorSlow))
        }

        val thread = Thread {
            futures.forEach { it.get(1, TimeUnit.DAYS) }
        }
        thread.start()

        Thread.sleep(Duration.ofSeconds(10))
    }

    private fun executeRq(restClient: RestClient): String? {
        return restClient
            .get()
            .uri("http://localhost:${WireMockRunner.wireMockPort}/offers")
            .retrieve()
            .toEntity(String::class.java)
            .body
    }

}
