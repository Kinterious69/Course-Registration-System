package com.university.registration.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * EnrollmentQueue — Producer-Consumer concurrency implementation.
 *
 * PRODUCERS: threads that submit enrollment requests (e.g. many students
 *            clicking "enroll" at the same time).
 *
 * CONSUMERS: worker threads that process each request one at a time,
 *            preventing race conditions on seat counts.
 *
 * Demonstrates:
 *   - BlockingQueue (thread-safe queue — no manual wait/notify needed)
 *   - Semaphore     (limits how many requests can be processed at once)
 *   - AtomicBoolean (thread-safe flag to shut the queue down cleanly)
 *   - Runnable      (worker thread logic)
 */
public class EnrollmentQueue {

    private static final Logger log = Logger.getLogger(EnrollmentQueue.class.getName());

    //  Core data structures 

    /** Thread-safe queue — producers put(), consumers take() */
    private final BlockingQueue<EnrollmentRequest> queue;

    /**
     * Semaphore limits concurrent processing to N worker threads.
     * Even if 100 students submit at once, only N are handled simultaneously.
     */
    private final Semaphore semaphore;

    /** Signals worker threads to stop after current work is done. */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** Callback interface — what to DO with each request once dequeued. */
    public interface RequestProcessor {
        void process(EnrollmentRequest request);
    }

    //  Constructor

    /**
     * @param capacity      max requests waiting in the queue at once
     * @param workerThreads number of concurrent processor threads
     * @param processor     the logic to run on each request (injected)
     */
    public EnrollmentQueue(int capacity, int workerThreads, RequestProcessor processor) {
        this.queue     = new LinkedBlockingQueue<>(capacity);
        this.semaphore = new Semaphore(workerThreads);

        // Start worker (consumer) threads
        for (int i = 0; i < workerThreads; i++) {
            Thread worker = new Thread(new Worker(processor), "enrollment-worker-" + i);
            worker.setDaemon(true);
            worker.start();
        }

        log.info("EnrollmentQueue started — capacity=" + capacity
            + ", workers=" + workerThreads);
    }

    // Producer method

    /**
     * Submit an enrollment request to the queue.
     * Called by the producer side (e.g. REST controller or Main).
     * Blocks if the queue is full until space is available.
     *
     * @return true if successfully queued, false if queue is shut down
     */
    public boolean submit(EnrollmentRequest request) {
        if (!running.get()) {
            log.warning("Queue is shut down — request rejected: " + request);
            return false;
        }
        try {
            queue.put(request);   // blocks if full — backpressure
            log.info("Queued: " + request);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gracefully shuts down the queue.
     * Worker threads finish their current request then stop.
     */
    public void shutdown() {
        running.set(false);
        log.info("EnrollmentQueue shutdown requested.");
    }

    public int getPendingCount() { return queue.size(); }

    // Worker (Consumer) inner class 
    /**
     * Each Worker thread loops: take a request → acquire semaphore →
     * process it → release semaphore → repeat.
     */
    private class Worker implements Runnable {

        private final RequestProcessor processor;

        Worker(RequestProcessor processor) {
            this.processor = processor;
        }

        @Override
        public void run() {
            while (running.get() || !queue.isEmpty()) {
                try {
                    // take() blocks until something is in the queue
                    EnrollmentRequest request = queue.take();

                    // Acquire semaphore slot — limits concurrency
                    semaphore.acquire();
                    try {
                        processor.process(request);
                    } finally {
                        semaphore.release();   // always release, even on error
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info(Thread.currentThread().getName() + " stopped.");
        }
    }

    //  EnrollmentRequest (inner record) 

    /**
     * A simple value object representing one enrollment request in the queue.
     * Immutable — safe to pass between threads without synchronization.
     */
    public static class EnrollmentRequest {

        private final String studentId;
        private final String courseId;
        private final long submittedAt;

        public EnrollmentRequest(String studentId, String courseId) {
            this.studentId   = studentId;
            this.courseId    = courseId;
            this.submittedAt = System.currentTimeMillis();
        }

        public String getStudentId()  { return studentId; }
        public String getCourseId()   { return courseId; }
        public long getSubmittedAt()  { return submittedAt; }

        @Override
        public String toString() {
            return "Request{student='" + studentId + "', course='" + courseId + "'}";
        }
    }
}
