package com.mateusz.nalepa.communicationerrors.bulkhead

import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class BulkheadTest {

    @Test
    fun `processing reqeusts on one pool`() {
        // given
        val executor = Executors.newFixedThreadPool(10)

        val futuresSlow = mutableListOf<Future<String>>()
        val futuresFast = mutableListOf<Future<String>>()

        val measureStart = System.currentTimeMillis()

        // when
        (0..29).map {
            futuresSlow.add(CompletableFuture.supplyAsync({ SlowExternalService.longRunningTask(it) }, executor))
            futuresFast.add(CompletableFuture.supplyAsync({ FastExternalService.fastTask(it) }, executor))
        }

        // then
        val threadForSlow = Thread {
            futuresSlow.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW Processing took: $duration")
        }
        threadForSlow.start()

        val threadForFast = Thread {
            futuresFast.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("FAST Processing took: $duration")
        }
        threadForFast.start()

        Thread.sleep(Duration.ofSeconds(20))
    }

    @Test
    fun `processing reqeusts on two pools`() {
        // given
        val executorSlow = Executors.newFixedThreadPool(10)
        val executorFast = Executors.newFixedThreadPool(10)

        val futuresSlow = mutableListOf<Future<String>>()
        val futuresFast = mutableListOf<Future<String>>()

        val measureStart = System.currentTimeMillis()

        // when
        (0..29).map {
            futuresSlow.add(CompletableFuture.supplyAsync({ SlowExternalService.longRunningTask(it) }, executorSlow))
            futuresFast.add(CompletableFuture.supplyAsync({ FastExternalService.fastTask(it) }, executorFast))
        }

        // then
        val threadForSlow = Thread {
            futuresSlow.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW Processing took: $duration")
        }
        threadForSlow.start()

        val threadForFast = Thread {
            futuresFast.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("FAST Processing took: $duration")
        }
        threadForFast.start()

        Thread.sleep(Duration.ofSeconds(20))
    }


}

private object SlowExternalService {

    fun longRunningTask(i: Int): String {
        Thread.sleep(5_000)
        return "SLOW - OK. Index: $i"
    }

}

private object FastExternalService {

    fun fastTask(i: Int): String {
        return "FAST - OK. Index: $i"
    }

}