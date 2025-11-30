package com.mateusz.nalepa.communicationerrors.bulkhead_isolation

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class BulkheadIsolationTest {

    @Test
    fun `should process slow and fast requests in one poll`() {
        // given
        val executor = Executors.newFixedThreadPool(1)

        val futuresForSlowRunningService = mutableListOf<Future<String>>()
        val futuresForFastRunningService = mutableListOf<Future<String>>()

        var timeToEndRequestsForSlowService: Duration = Duration.ZERO
        var timeToEndRequestsForFastService: Duration = Duration.ZERO

        val measureStart = System.currentTimeMillis()

        // when
        (1..3).map {
            futuresForSlowRunningService.add(
                CompletableFuture.supplyAsync(
                    { SlowExternalService.longRunningTask(it) },
                    executor
                )
            )
            futuresForFastRunningService.add(
                CompletableFuture.supplyAsync(
                    { FastExternalService.fastTask(it) },
                    executor
                )
            )
        }

        // then
        val threadForSlow = Thread {
            futuresForSlowRunningService.forEach { println(it.get(1, TimeUnit.DAYS)) }
            timeToEndRequestsForSlowService = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW requests.  Processing took: $timeToEndRequestsForSlowService")
        }
        threadForSlow.start()

        val threadForFast = Thread {
            futuresForFastRunningService.forEach { println(it.get(1, TimeUnit.DAYS)) }
            timeToEndRequestsForFastService = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("FAST requests.  Processing took: $timeToEndRequestsForFastService")
        }
        threadForFast.start()

        threadForSlow.join()
        threadForFast.join()

        // THEN
        timeToEndRequestsForSlowService shouldBeGreaterThan Duration.ofSeconds(6)
        timeToEndRequestsForFastService shouldBeGreaterThan Duration.ofSeconds(6) // it should be fast, but it's slow, due to one pool
    }

    @Test
    fun `should process slow and fast requests in two poll`() {
        // given
        val executorSlow = Executors.newFixedThreadPool(1)
        val executorFast = Executors.newFixedThreadPool(1)

        val futuresForSlowRunningService = mutableListOf<Future<String>>()
        val futuresForFastRunningService = mutableListOf<Future<String>>()

        var timeToEndRequestsForSlowService: Duration = Duration.ZERO
        var timeToEndRequestsForFastService: Duration = Duration.ZERO

        val measureStart = System.currentTimeMillis()

        // when
        (1..3).map {
            futuresForSlowRunningService.add(
                CompletableFuture.supplyAsync(
                    { SlowExternalService.longRunningTask(it) },
                    executorSlow
                )
            )
            futuresForFastRunningService.add(
                CompletableFuture.supplyAsync(
                    { FastExternalService.fastTask(it) },
                    executorFast
                )
            )
        }

        // then
        val threadForSlow = Thread {
            futuresForSlowRunningService.forEach { println(it.get(1, TimeUnit.DAYS)) }
            timeToEndRequestsForSlowService = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW requests. Processing took: $timeToEndRequestsForSlowService")
        }
        threadForSlow.start()

        val threadForFast = Thread {
            futuresForFastRunningService.forEach { println(it.get(1, TimeUnit.DAYS)) }
            timeToEndRequestsForFastService = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("Fast requests. Processing took: $timeToEndRequestsForFastService")
        }
        threadForFast.start()

        threadForSlow.join()
        threadForFast.join()

        // THEN
        timeToEndRequestsForSlowService shouldBeGreaterThan Duration.ofSeconds(6)
        timeToEndRequestsForFastService shouldBeLessThan Duration.ofSeconds(1)
    }

}
