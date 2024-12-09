package com.mateusz.nalepa.communicationerrors

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.Test
import java.time.Duration


// TODO: poogarniaj ogólnie te testy XD
class CircuitBreakerTest {

    @Test
    fun `should open circuit breaker`() {
        // given
        val config =
            CircuitBreakerConfig
                .custom()
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build()

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(config)


        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("testInstance")

        circuitBreaker.eventPublisher.onStateTransition { event ->
            val from = event.stateTransition.fromState
            val to = event.stateTransition.toState

            println("\n#################################################")
            println("!!! Circuit breaker changed from: $from to: $to")
        }

        println("Start tests! Service throws exceptions :(")
        ExternalService.willThrowException = true
        (1..4).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }
    }


    @Test
    fun `should open, halfOpen and the close circuit breaker`() {
        // given
        val config =
            CircuitBreakerConfig
                .custom()
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build()

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(config)


        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("testInstance")

        circuitBreaker.eventPublisher.onStateTransition { event ->
            val from = event.stateTransition.fromState
            val to = event.stateTransition.toState

            println("\n#################################################")
            println("!!! Circuit breaker changed from: $from to: $to")
        }


        println("Start tests! Service throws exceptions :(")
        ExternalService.willThrowException = true
        (1..4).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }

        // then regenerate
        println("Time to heal!")
        Thread.sleep(2000)
        println("Start tests! Service is healthy now! :)")
        ExternalService.willThrowException = false
        (5..9).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }

    }

    @Test
    fun `should open, halfOpen and open again circuit breaker`() {
        // given
        val config =
            CircuitBreakerConfig
                .custom()
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build()

        val circuitBreakerRegistry = CircuitBreakerRegistry.of(config)


        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("testInstance")

        circuitBreaker.eventPublisher.onStateTransition { event ->
            val from = event.stateTransition.fromState
            val to = event.stateTransition.toState

            println("\n#################################################")
            println("!!! Circuit breaker changed from: $from to: $to")
        }


        println("Start tests! Service throws exceptions :(")
        ExternalService.willThrowException = true
        (1..4).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }

        // then regenerate
        println("\n\nTime to heal!")
        Thread.sleep(2000)
        println("Continue tests. Service still throws exceptions :(")
        ExternalService.willThrowException = true
        (5..6).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }

        println("\n\nTime to heal again!")
        Thread.sleep(2000)
        println("Continue tests. Service is healthy now!")
        ExternalService.willThrowException = false
        (7..10).forEach {
            try {
                println("\nWill call external service. Attempt: $it")
                val number = circuitBreaker.executeSupplier { ExternalService.call() }
            } catch (t: Throwable) {
                println("Got error ${t.message} when calling external service for attempt: $it")
            }
        }
    }


}

object ExternalService {
    var willThrowException = true

    fun call(): String {
        println("External service has been called.")

        if (willThrowException) {
            throw RuntimeException("Exception for attempt")
        } else {
            return "OK"
        }
    }

}
