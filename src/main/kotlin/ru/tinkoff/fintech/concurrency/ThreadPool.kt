package ru.tinkoff.fintech.concurrency

import java.lang.Thread.sleep
import java.util.Collections
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue

class ThreadPool(
    private val poolSize: Int
) : Executor {
    private val workers: MutableList<WorkerThread> = ArrayList(poolSize)
    private val queue: BlockingQueue<Runnable> = LinkedBlockingQueue()
    private val terminatedWorkers: MutableList<String> = Collections.synchronizedList(ArrayList(poolSize))

    private val checkCount = 10

    @Volatile
    private var termination: Boolean = false

    init {
        if (poolSize <= 0) {
            throw IllegalArgumentException("poolSize must be grater than zero")
        }
        repeat(poolSize) {
            workers.add(WorkerThread())
            workers[workers.size - 1].start()
        }
        println("Thread pool created with $poolSize threads")
    }

    inner class WorkerThread : Thread() {
        override fun run() {
            var task: Runnable?
            while (true) {
                synchronized(queue) {
                    while (queue.isEmpty() && !termination) {
                        runCatching {
                            (queue as Object).wait()
                        }.onFailure {
                            println("Thread worker is interrupted while waiting task, threadName=${currentThread().name}")
                        }
                    }
                    task = queue.poll()
                }
                if (task != null) {
                    task?.run()
                } else if (termination) {
                    break
                }
            }
            terminatedWorkers.add(currentThread().name)
            println("Thread worker is terminated, threadName=${currentThread().name}")
        }
    }

    override fun execute(task: Runnable) {
        synchronized (queue) {
            queue.add(task)
            (queue as Object).notify()
        }
    }

    fun shutdown(): Boolean {
        termination = true
        workers.forEach {
            it.interrupt()
        }
        var counter = 0
        while (true) {
            if (terminatedWorkers.size == poolSize) {
                return true
            }
            if (counter++ == checkCount) {
                return false
            }
            sleep(10)
        }
    }
}
