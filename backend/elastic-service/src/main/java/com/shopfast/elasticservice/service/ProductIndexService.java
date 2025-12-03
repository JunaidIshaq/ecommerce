package com.shopfast.elasticservice.service;

import com.shopfast.elasticservice.document.ProductDocument;

public interface ProductIndexService {

    ProductDocument index(ProductDocument product);

}
