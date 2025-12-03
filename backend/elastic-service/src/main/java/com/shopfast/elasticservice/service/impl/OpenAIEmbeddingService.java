package com.shopfast.elasticservice.service.impl;

import com.shopfast.elasticservice.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAIEmbeddingService implements EmbeddingService {


    @Override
    public float[] embed(String text) {
        // TODO: call OpenAI embeddings and convert to float[]
        // For now, just throw to remind you to implement
        throw new UnsupportedOperationException("Implement OpenAI embeddings here");
    }
}
