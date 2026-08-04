package com.shopfast.orderservice.exception;

/**
 * Raised when a downstream service is unreachable, timed out, or its circuit
 * breaker is open.
 *
 * <p>This is deliberately distinct from a business validation failure: it means
 * "we do not know the answer", not "the answer is no". Callers must never treat
 * it as a successful negative result.</p>
 */
public class RemoteServiceUnavailableException extends RuntimeException {

    private final String service;

    public RemoteServiceUnavailableException(String service, Throwable cause) {
        super(service + " is unavailable: " + (cause == null ? "unknown cause" : cause.getMessage()), cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
