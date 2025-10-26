package com.sorinwork.tickets.common;

/** A request that is well formed but not allowed in the current state, shown to the user. */
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }
}
