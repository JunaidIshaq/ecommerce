//package com.shopfast.adminservice.model;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.Id;
//import jakarta.persistence.Index;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.Instant;
//import java.util.UUID;
//
//@Entity
//@Table(name = "processed_commands", indexes = {
//        @Index(name = "idx_cmd_id", columnList = "commandId", unique = true)
//})
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ProcessedCommand {
//
//    @Id
//    @GeneratedValue(generator = "UUID")
//    private UUID id;
//
//    @Column(nullable = false, unique = true)
//    private String commandId;
//
//    @CreationTimestamp
//    @JsonFormat(shape = JsonFormat.Shape.STRING)
//    private Instant processedAt;
//
//}
