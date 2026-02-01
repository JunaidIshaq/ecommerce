package com.shopfast.cartservice.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductInternalResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String title;

    private BigDecimal price;

    public List<String> images = new ArrayList<>();

    private Boolean active;

}
