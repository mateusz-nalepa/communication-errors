package com.mateusz.nalepa.communicationerrors.api

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BuyBeerEndpoint {
    @PostMapping(
        value = ["/beer/buy"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun buyBeer(@RequestBody buyBeerRequest: BuyBeerRequest): ResponseEntity<*> {
        if (buyBeerRequest.age < 18) {
            return ResponseEntity.unprocessableEntity().build<Unit>()
        }
        return ResponseEntity.ok(BuyBeerResponse("Beer Granted!"))
    }

}

data class BuyBeerRequest(
    val age: Int,
)

data class BuyBeerResponse(
    val text: String,
)