package com.mateusz.nalepa.communicationerrors.retry

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RetryTest {

    @Test
    fun `should retry 3 times`() {
        // given
        val retryHelper = RetryHelper(successOnAttempt = 3)

        // when
        lateinit var response: String
        for (i in 1..10) {
            try {
                println("Attempt: $i of 10")
                response = retryHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                // do nothing
            }
        }

        response shouldBe retryHelper.successResponseValue()
    }

    @Test
    fun `should retry with sleep 3 times`() {
        // given
        val givenSuccessOnAttempt = 3
        val retryHelper = RetryHelper(successOnAttempt = givenSuccessOnAttempt)

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                println("Attempt: $i of 10")
                response = retryHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                Thread.sleep(100)
            }
        }

        response shouldBe retryHelper.successResponseValue()

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeGreaterThanOrEqual (givenSuccessOnAttempt.toLong() - 1) * 100
        duration shouldBeLessThanOrEqual (givenSuccessOnAttempt.toLong()) * 100
    }

    @Test
    fun `should retry with exponential backoff 3 times`() {
        // given
        val givenSuccessOnAttempt = 3
        val retryHelper = RetryHelper(successOnAttempt = givenSuccessOnAttempt)

        val initialSleepMs: Long = 100

        var sleepMs: Long = initialSleepMs
        val backOffMultiplier = 0.5

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                println("Attempt: $i of 10")
                response = retryHelper.provideResponse(i)
                break
            } catch (t: Throwable) {
                println("Error, will sleep: $sleepMs ms until next retry")
                Thread.sleep(sleepMs)
                sleepMs = sleepMs + (sleepMs * backOffMultiplier).toLong()
            }
        }

        response shouldBe retryHelper.successResponseValue()

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeLessThanOrEqual 300
    }

    @Test
    fun `should retry only retryableException`() {
        // given
        val givenSuccessOnAttempt = 3
        val retryHelper = RetryHelper(successOnAttempt = givenSuccessOnAttempt)

        val retryableExceptionClass = RetryableException::class.java.name

        // when
        val measureStart = System.currentTimeMillis()
        lateinit var response: String
        for (i in 1..10) {
            try {
                println("Attempt: $i of 10")
                response = retryHelper.provideResponse(i, RetryableException())
                break
            } catch (t: Throwable) {
                if (t::class.java.name == retryableExceptionClass) {
                    Thread.sleep(100)
                } else {
                    throw t
                }
            }
        }

        response shouldBe retryHelper.successResponseValue()

        val duration = System.currentTimeMillis() - measureStart
        println("Duration: $duration ms")
        duration shouldBeGreaterThanOrEqual 200
        duration shouldBeLessThanOrEqual 300
    }

    @Test
    fun `should not retry for unknown error`() {
        // given
        val givenSuccessOnAttempt = 3
        val retryHelper = RetryHelper(successOnAttempt = givenSuccessOnAttempt)

        val retryableExceptionClass = RetryableException::class.java.name

        // when
        val measureStart = System.currentTimeMillis()
        assertThrows<SomeUnknownException> {
            for (i in 1..10) {
                try {
                    println("Attempt: $i of 10")
                    retryHelper.provideResponse(i, SomeUnknownException())
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

class RetryHelper(
    private var successOnAttempt: Int = 5,
    private var attempts: Int = 1,
) {

    fun successResponseValue(): String {
        return "OK"
    }

    fun provideResponse(actualAttemptNumber: Int, exception: RuntimeException = RuntimeException("XDD")): String {
        if (attempts == successOnAttempt) {
            return successResponseValue()
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