package com.mateusz.nalepa.communicationerrors.bulkhead_isolation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.*

// the same rules are for ThreadPool, as well as ConnectionPool
// In order to get connection, first we need to have a thread
class PoolTests {

    val corePoolSize = 10
    val maximumPoolSize = corePoolSize
    val keepAliveTime = 10L
    val unit = TimeUnit.SECONDS

    @Test
    fun `should reject tasks, if there is no available thread`() {
        // threads in pool
        val waitingTaskQueue =
            SynchronousQueue<Runnable>() // SynchronousQueue - basically means no queue, either thread is free, or reject task

        val executor = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            waitingTaskQueue,
            ThreadPoolExecutor.AbortPolicy() // what to do when queue is full
        )

        val futuresForSlowRequests = mutableListOf<Future<String>>()
        var successCount = 0
        var failuresCount = 0

        // when
        (1..30).map {
            try {
                futuresForSlowRequests.add(
                    CompletableFuture.supplyAsync(
                        { SlowExternalService.longRunningTask(it) },
                        executor
                    )
                )
                successCount++
            } catch (t: RejectedExecutionException) {
                failuresCount++
                println("Every thread is working right now. Unable to process: $it")
            }
        }
        futuresForSlowRequests.forEach { println(it.get(1, TimeUnit.DAYS)) }

        // then
        successCount shouldBe corePoolSize
        failuresCount shouldBe 20
    }

    @Test
    fun `allow only 10 tasks in queue`() {
        // threads in pool
        val queueSize = 10

        val waitingTaskQueue = ArrayBlockingQueue<Runnable>(queueSize)

        val executor = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            waitingTaskQueue,
            ThreadPoolExecutor.AbortPolicy() // what to do when queue is full
        )

        val futuresSlow = mutableListOf<Future<String>>()
        var successCount = 0
        var failuresCount = 0

        // when
        (1..30).map {
            try {
                futuresSlow.add(CompletableFuture.supplyAsync({ SlowExternalService.longRunningTask(it) }, executor))
                successCount++
            } catch (t: RejectedExecutionException) {
                println("Queue is full. Unable to process: $it")
                failuresCount++
            }
        }
        futuresSlow.forEach { println(it.get(1, TimeUnit.DAYS)) }

        // then
        successCount shouldBe corePoolSize + queueSize
        failuresCount shouldBe 10
    }

}

