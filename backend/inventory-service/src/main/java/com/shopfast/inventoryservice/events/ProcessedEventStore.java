package com.shopfast.inventoryservice.events;

public interface ProcessedEventStore {

    boolean isProcessed(String eventId);

    void markProcessed(String eventId);

}
