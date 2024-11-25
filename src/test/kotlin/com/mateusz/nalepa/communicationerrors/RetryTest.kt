package com.mateusz.nalepa.communicationerrors

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// TODO: poogarniaj ogólnie te testy XD
// zawsze mierz liczbe retry
class RetryTest {

    @Test
    fun `should retry 3 times`() {
        // given
        val attemptHelper = AttemptHelper(successOnAttempt = 3)

        // when
        lateinit var response: String
        for (i in 1..10) {
            try {
                println("Attempt: $i of 3")
                response = attemptHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                // do nothing
            }
        }

        response shouldBe "OK"
    }

    @Test
    fun `should retry with sleep 3 times`() {
        // given
        val givenSuccessOnAttempt = 3
        val attemptHelper = AttemptHelper(successOnAttempt = givenSuccessOnAttempt)

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                response = attemptHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                Thread.sleep(100)
            }
        }

        response shouldBe "OK"

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeGreaterThanOrEqual givenSuccessOnAttempt.toLong() * 100
        duration shouldBeLessThanOrEqual (givenSuccessOnAttempt.toLong() + 1) * 100
    }

    @Test
    fun `should retry with exponential backoff 3 times`() {
        // given
        val givenSuccessOnAttempt = 3
        val attemptHelper = AttemptHelper(successOnAttempt = givenSuccessOnAttempt)

        val initialSleepMs: Long = 100

        var sleepMs: Long = initialSleepMs
        val backOffMultiplier = 0.5

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                response = attemptHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                println("Error, will sleep: $sleepMs ms until next retry")
                Thread.sleep(sleepMs)
                sleepMs = sleepMs + (sleepMs * backOffMultiplier).toLong()
            }
        }

        response shouldBe "OK"

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeLessThanOrEqual 550
    }

    @Test
    fun `should retry only retryableException`() {
        // given
        val givenSuccessOnAttempt = 3
        val attemptHelper = AttemptHelper(successOnAttempt = givenSuccessOnAttempt)

        val retryableExceptionClass = RetryableException::class.java.name

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                response = attemptHelper.provideResponse(i, RetryableException())
                break
            } catch (t: Throwable) {
                if (t::class.java.name == retryableExceptionClass) {
                    Thread.sleep(100)
                } else {
                    throw t
                }
            }
        }

        response shouldBe "OK"

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeGreaterThanOrEqual 300
        duration shouldBeLessThanOrEqual 400
    }

    @Test
    fun `should not retry for unknown error`() {
        // given
        val givenSuccessOnAttempt = 3
        val attemptHelper = AttemptHelper(successOnAttempt = givenSuccessOnAttempt)

        val retryableExceptionClass = RetryableException::class.java.name

        // when
        val measureStart = System.currentTimeMillis()
        assertThrows<SomeUnknownException> {
            for (i in 1..10) {
                try {
                    attemptHelper.provideResponse(i, SomeUnknownException())
                    break
                } catch (t: Throwable) {
                    if (t::class.java.name == retryableExceptionClass) {
                        Thread.sleep(100)
                    } else {
                        throw t
                    }
                }
            }
        }

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeGreaterThanOrEqual 0
        duration shouldBeLessThanOrEqual 100
    }


}

class AttemptHelper(
    private var successOnAttempt: Int = 5,
    private var attempts: Int = 0,
) {

    fun provideResponse(actualAttemptNumber: Int, exception: RuntimeException = RuntimeException("XDD")): String {
        if (attempts == successOnAttempt) {
            return "OK"
        }

        if (attempts <= actualAttemptNumber) {
            attempts++
            throw exception
        }
        return "OK"
    }

}

class RetryableException : RuntimeException()
class SomeUnknownException : RuntimeException()