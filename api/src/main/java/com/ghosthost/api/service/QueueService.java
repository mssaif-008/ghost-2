package com.ghosthost.api.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-memory job queue for MVP.
 *
 * HOW IT WORKS:
 * - DeployService calls enqueue(deploymentId) when a new deploy is created
 * - BuildWorker calls poll() in a loop to grab the next job
 * - LinkedBlockingQueue is thread-safe (no synchronization needed)
 *
 * WHY IN-MEMORY?
 * For MVP, this is the simplest approach. It means:
 * - Jobs are lost if the server restarts
 * - Only one worker can consume jobs
 * - No external dependencies (Redis)
 *
 * HOW TO MIGRATE TO REDIS LATER:
 * Replace this class with a Redis-backed implementation:
 * - enqueue() → RPUSH to a Redis list
 * - poll() → BLPOP from the Redis list
 * - This allows multiple workers on different machines
 * See the "Scaling Beyond MVP" section in README.md
 */
@Service
public class QueueService {

    // Thread-safe queue — capacity of 100 jobs for MVP
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

    /**
     * Add a deployment ID to the build queue.
     * 
     * @return true if added, false if queue is full
     */
    public boolean enqueue(String deploymentId) {
        return queue.offer(deploymentId);
    }

    /**
     * Take the next deployment ID from the queue.
     * Returns null immediately if queue is empty (non-blocking).
     */
    public String poll() {
        return queue.poll();
    }

    /**
     * How many jobs are waiting in the queue.
     */
    public int size() {
        return queue.size();
    }
}
