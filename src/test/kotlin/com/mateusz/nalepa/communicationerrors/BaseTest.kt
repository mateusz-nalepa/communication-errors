package com.mateusz.nalepa.communicationerrors

import com.mateusz.nalepa.communicationerrors.wiremock.WireMockRunner
import org.junit.jupiter.api.AfterEach
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        CommunicationErrorsApplication::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class BaseTest {

    @AfterEach
    fun afterEach() {
        WireMockRunner.reset()
        WireMockRunner.stop()
    }

}

