package com.shopfast.adminservice.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CheckoutRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String couponCode;

    private String fullName;

    private String street;

    private String city;

    private String state;

    private String zip;

    private String country;

    private String phone;

}
