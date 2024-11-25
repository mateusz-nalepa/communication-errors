package com.mateusz.nalepa.communicationerrors.api

import com.mateusz.nalepa.communicationerrors.domain.BeerHolder
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ListBeersEndpoint {

    private val beerHolder = BeerHolder.all()

    @GetMapping(
        value = ["/beers"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun beers(): ResponseEntity<BeersListResponse> {
        return ResponseEntity.ok(
            BeersListResponse(
                beers = beerHolder.beers.map { it.name }
            )
        )
    }

}


data class BeersListResponse(
    val beers: List<String>,
)
