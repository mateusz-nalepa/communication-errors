package com.mateusz.nalepa.communicationerrors.timeout.queue

import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.*

class QueueTests {

    @Test
    fun `allow execute right now, without queue`() {
        // threads in pool
        val corePoolSize = 10
        val maximumPoolSize = corePoolSize
        val keepAliveTime = 10L
        val unit = TimeUnit.SECONDS

        val waitingTaskQueue = SynchronousQueue<Runnable>()

        val executor = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            waitingTaskQueue,
            ThreadPoolExecutor.AbortPolicy() // what to do when queue is full
        )

        val futuresSlow = mutableListOf<Future<String>>()
        val measureStart = System.currentTimeMillis()


        (1..30).map {
            try {
                futuresSlow.add(CompletableFuture.supplyAsync({ SlowExternalService.longRunningTask(it) }, executor))
            } catch (t: RejectedExecutionException) {
                println("Every thread is working right now. Unable to process: $it")
            }
        }

        // then
        val threadForSlow = Thread {
            futuresSlow.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW Processing took: $duration")
        }
        threadForSlow.start()
        Thread.sleep(Duration.ofSeconds(20))
    }

    @Test
    fun `allow only 10 tasks in queue`() {
        // threads in pool
        val corePoolSize = 10
        val maximumPoolSize = corePoolSize
        val keepAliveTime = 10L
        val unit = TimeUnit.SECONDS
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
        val measureStart = System.currentTimeMillis()


        (1..30).map {
            try {
                futuresSlow.add(CompletableFuture.supplyAsync({ SlowExternalService.longRunningTask(it) }, executor))
            } catch (t: RejectedExecutionException) {
                println("Queue is full. Unable to process: $it")
            }
        }

        // then
        val threadForSlow = Thread {
            futuresSlow.forEach { println(it.get(1, TimeUnit.DAYS)) }
            val duration = Duration.ofMillis(System.currentTimeMillis() - measureStart)
            println("SLOW Processing took: $duration")
        }
        threadForSlow.start()
        Thread.sleep(Duration.ofSeconds(20))
    }

}

private object SlowExternalService {

    fun longRunningTask(i: Int): String {
        Thread.sleep(5_000)
        return "SLOW - OK. Index: $i"
    }

}
