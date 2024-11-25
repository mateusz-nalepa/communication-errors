package com.mateusz.nalepa.communicationerrors.domain

data class BeerHolder(
    val beers: List<Beer>,
) {
    companion object {
        fun all(): BeerHolder =
            BeerHolder(
                beers =
                listOf(
                    Beer(
                        name = "beer1",
                        details = "najlepsze",
                    ),
                    Beer(
                        name = "beer2",
                        details = "dobre",
                    ),
                    Beer(
                        name = "beer3",
                        details = "najgorsze",
                    )
                )
            )
    }
}


data class Beer(
    val name: String,
    val details: String,
)