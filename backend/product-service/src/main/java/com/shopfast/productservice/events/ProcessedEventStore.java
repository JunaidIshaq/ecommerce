package com.shopfast.productservice.events;

public interface ProcessedEventStore {

    boolean isProcessed(String eventId);

    boolean markProcessed(String eventId);

}
