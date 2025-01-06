package com.mateusz.nalepa.communicationerrors.timeout.queue

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.mateusz.nalepa.communicationerrors.BaseTest
import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.observation.Observation
import org.apache.hc.client5.http.classic.ExecChain
import org.apache.hc.client5.http.classic.ExecChainHandler
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Duration.ofMillis
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ConnectionRequestTimeoutWaitingInQueue(
    @Autowired
    private val meterRegistry: MeterRegistry,
) : BaseTest() {

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
        println("START CLIENT WARMUP")
        executeRq(restClient)
        println("WARMUP DONE")

        // when
        (1..givenNumberOfRequests).map { number ->
            futures.add(CompletableFuture.supplyAsync({
                val measureStart = System.currentTimeMillis()
                executeRq(restClient)
                    .also {
                        val duration = System.currentTimeMillis() - measureStart
//                        println("Processing requestNumber $number with queue wait took $duration ms")
                    }
            }, executorSlow))
        }

        val thread = Thread {
            futures.forEach { it.get(1, TimeUnit.DAYS) }
        }
        thread.start()

        Thread.sleep(Duration.ofSeconds(2))

        meterRegistry.get("http.client.requests")
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


@Configuration
class CustomMetricConfig(
    private val meterRegistry: MeterRegistry,
) {

    @Bean
    fun customDefaultMeterObservationHandler() =
        CustomDefaultMeterObservationHandler(meterRegistry)
}

class CustomDefaultMeterObservationHandler(private val meterRegistry: MeterRegistry) :
    DefaultMeterObservationHandler(meterRegistry) {

    override fun onStop(context: Observation.Context) {
        if (context.name == "http.client.requests") {
            val sample = context.getRequired<Timer.Sample>(Timer.Sample::class.java)
            val duration  = sample.stop(Timer.builder(context.name).register(this.meterRegistry))
            println("http.client metric duration value: " + duration.toDuration(DurationUnit.NANOSECONDS))
        }
    }
}