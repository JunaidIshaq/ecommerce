package com.shopfast.elasticservice.config;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Consumer error policy for this service.
 *
 * <p>Before this existed, a listener that threw simply retried forever with the container
 * default, blocking the partition and stalling every later event for the same key. Now:</p>
 * <ul>
 *   <li>transient failures are retried with exponential backoff (bounded), and</li>
 *   <li>anything still failing - or that can never succeed, such as an unparseable payload -
 *       is published to a {@code <topic>.DLT} topic so the partition keeps moving and the
 *       bad record stays available for inspection.</li>
 * </ul>
 *
 * <p>The template is injected as {@code ObjectProvider<KafkaOperations<?, ?>>} rather than a
 * concrete generic type on purpose. Services here declare their producer as
 * {@code KafkaTemplate<String, Object>}, which is a {@code KafkaOperations<String, Object>} and
 * therefore does <em>not</em> satisfy a {@code KafkaOperations<Object, Object>} parameter -
 * Spring treats the generics as part of the type match and startup fails with
 * "no qualifying bean". The wildcard matches any declared template, {@code getIfUnique} keeps
 * it unambiguous when several exist, and if a service has no producer at all we still return a
 * working handler that retries and then logs, rather than refusing to start.</p>
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    @ConditionalOnMissingBean(DefaultErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(ObjectProvider<KafkaOperations<?, ?>> templates) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(60_000L);

        KafkaOperations<?, ?> template =
                templates.getIfUnique(() -> templates.orderedStream().findFirst().orElse(null));

        DefaultErrorHandler handler = (template != null)
                ? new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff)
                : new DefaultErrorHandler(backOff);

        // Poison-pill payloads and programming errors will never succeed on retry:
        // send them straight to the DLT instead of burning the retry budget.
        handler.addNotRetryableExceptions(
                SerializationException.class,
                ConversionException.class,
                IllegalArgumentException.class,
                NullPointerException.class);

        return handler;
    }
}
