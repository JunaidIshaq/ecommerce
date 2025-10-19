package com.shopfast.productservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDto {

    public String id;

    public String slug;

    public String name;

    public String description;

    public String category;

    public BigDecimal price;

    public String currency;

    public Integer stock;

    public Double rating;

    public List<String> images;

    public List<String> tags;

}
