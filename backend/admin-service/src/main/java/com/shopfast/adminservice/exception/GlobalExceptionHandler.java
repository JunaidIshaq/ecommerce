package com.shopfast.adminservice.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Handle Feign exceptions and return proper JSON format
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        
        try {
            // Try to parse the response body from downstream service
            String responseBody = ex.contentUTF8();
            if (responseBody != null && !responseBody.isEmpty()) {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // If downstream already returns JSON, use it
                errorResponse.put("success", jsonNode.has("success") ? jsonNode.get("success").asBoolean() : false);
                errorResponse.put("status", jsonNode.has("status") ? jsonNode.get("status").asInt() : ex.status());
                errorResponse.put("message", jsonNode.has("message") ? jsonNode.get("message").asText() : ex.getMessage());
                errorResponse.put("timestamp", jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : Instant.now().toString());
            } else {
                // Fallback if no response body
                errorResponse.put("success", false);
                errorResponse.put("status", ex.status());
                errorResponse.put("message", "Downstream service error: " + ex.getMessage());
                errorResponse.put("timestamp", Instant.now().toString());
            }
        } catch (Exception e) {
            // Fallback for any parsing errors
            errorResponse.put("success", false);
            errorResponse.put("status", ex.status());
            errorResponse.put("message", "Downstream service error: " + ex.getMessage());
            errorResponse.put("timestamp", Instant.now().toString());
        }
        
        return ResponseEntity.status(ex.status()).body(errorResponse);
    }

}
