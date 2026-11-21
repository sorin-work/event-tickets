package com.sorinwork.tickets.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runs several calls at the same moment, each on its own thread and so in its own transaction. */
public final class Race {

  private Race() {}

  /** The outcome of one call: what it returned, or the exception it threw. */
  public record Outcome<T>(T value, Throwable error) {

    public boolean succeeded() {
      return error == null;
    }
  }

  public static <T> List<Outcome<T>> run(List<Callable<T>> calls) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(calls.size());
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<T>> futures = new ArrayList<>();
      for (Callable<T> call : calls) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return call.call();
                }));
      }
      start.countDown();
      List<Outcome<T>> outcomes = new ArrayList<>();
      for (Future<T> future : futures) {
        try {
          outcomes.add(new Outcome<>(future.get(60, TimeUnit.SECONDS), null));
        } catch (java.util.concurrent.ExecutionException e) {
          outcomes.add(new Outcome<>(null, e.getCause()));
        } catch (java.util.concurrent.TimeoutException e) {
          throw new AssertionError("A racing call did not finish within a minute", e);
        }
      }
      return outcomes;
    } finally {
      pool.shutdownNow();
    }
  }
}
