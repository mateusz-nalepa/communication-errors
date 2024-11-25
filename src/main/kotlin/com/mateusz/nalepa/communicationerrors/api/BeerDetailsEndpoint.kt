package com.mateusz.nalepa.communicationerrors.api

import com.mateusz.nalepa.communicationerrors.domain.BeerHolder
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class BeerDetailsEndpoint {

    private val beerHolder = BeerHolder.all()

    @GetMapping(
        value = ["/beers/{beerName}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun beers(@PathVariable beerName: String): ResponseEntity<*> {
        if (beerName == "invalidBeer") {
            // something wrong with that beer
            throw RuntimeException("error when getting details")
        }

        return beerHolder
            .beers
            .find { it.name == beerName }
            ?.let {
                BeerDetailsResponse(
                    name = it.name,
                    details = it.details,
                )
            }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build<Unit>()
    }
}

data class BeerDetailsResponse(
    val name: String,
    val details: String,
)
