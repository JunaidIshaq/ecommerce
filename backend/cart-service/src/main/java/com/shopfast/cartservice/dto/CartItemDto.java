package com.shopfast.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @JsonProperty("productId")
    private UUID productId;

    @NotNull
    private String title;

    @NotNull
    private BigDecimal price;

    @Min(1)
    private int quantity;

    private List<String> images;

}
