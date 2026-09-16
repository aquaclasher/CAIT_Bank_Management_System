package com.bank.concurrency;

import com.bank.exception.BankingException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker abstraction to execute concurrent transactions with latch coordination
 * and metrics tracking.
 */
public class ConcurrentTransactionWorker implements Runnable {
    @FunctionalInterface
    public interface TransactionTask {
        void execute() throws BankingException;
    }

    private final String workerId;
    private final TransactionTask task;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;
    private final AtomicInteger successCounter;
    private final AtomicInteger failureCounter;

    public ConcurrentTransactionWorker(
            String workerId,
            TransactionTask task,
            CountDownLatch startSignal,
            CountDownLatch doneSignal,
            AtomicInteger successCounter,
            AtomicInteger failureCounter
    ) {
        this.workerId = workerId;
        this.task = task;
        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
        this.successCounter = successCounter;
        this.failureCounter = failureCounter;
    }

    @Override
    public void run() {
        try {
            if (startSignal != null) {
                // Synchronize all threads at the starting barrier
                startSignal.await();
            }
            task.execute();
            if (successCounter != null) {
                successCounter.incrementAndGet();
            }
        } catch (BankingException be) {
            if (failureCounter != null) {
                failureCounter.incrementAndGet();
            }
            // Domain exception caught safely in concurrent harness
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (failureCounter != null) {
                failureCounter.incrementAndGet();
            }
        } catch (Exception e) {
            if (failureCounter != null) {
                failureCounter.incrementAndGet();
            }
        } finally {
            if (doneSignal != null) {
                doneSignal.countDown();
            }
        }
    }

    public String getWorkerId() {
        return workerId;
    }
}
