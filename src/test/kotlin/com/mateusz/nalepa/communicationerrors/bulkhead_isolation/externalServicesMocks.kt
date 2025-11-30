package com.mateusz.nalepa.communicationerrors.bulkhead_isolation

object SlowExternalService {

    fun longRunningTask(i: Int): String {
        Thread.sleep(2_000)
        return "SLOW - OK. Index: $i"
    }

}

object FastExternalService {

    fun fastTask(i: Int): String {
        return "FAST - OK. Index: $i"
    }

}