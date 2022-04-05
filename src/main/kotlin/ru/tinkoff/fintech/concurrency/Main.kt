package ru.tinkoff.fintech.concurrency

import java.lang.Thread.sleep

fun main() {
    val threadPool = ThreadPool(4)
    for (i in 1..20) {
        threadPool.execute {
            println("Task num=$i, threadName=${Thread.currentThread().name}")
        }
    }

    println("All tasks are passed to executor")

    sleep(1000)

    val exitCode = threadPool.shutdown()
    println("Thread pool has shut down ${if (exitCode) "successfully" else "with errors"}")
}
