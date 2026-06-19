package com.studyroom.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "studyroom.violation";
    public static final String DELAY_QUEUE = "studyroom.violation.delay";
    public static final String TARGET_QUEUE = "studyroom.violation.check";
    public static final String ROUTING_KEY = "violation.check";

    // 延时队列：消息带 TTL，过期后投递到目标队列
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
            .deadLetterExchange(EXCHANGE)
            .deadLetterRoutingKey(ROUTING_KEY)
            .build();
    }

    // 目标队列：消费者实际监听
    @Bean
    public Queue targetQueue() {
        return QueueBuilder.durable(TARGET_QUEUE).build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(targetQueue()).to(exchange()).with(ROUTING_KEY);
    }

    // ================= AI 推荐异步队列配置 =================
    public static final String AI_EXCHANGE = "studyroom.ai";
    public static final String AI_RECOMMEND_QUEUE = "studyroom.ai.recommend";
    public static final String AI_RECOMMEND_ROUTING_KEY = "ai.recommend";

    @Bean
    public Queue aiRecommendQueue() {
        return QueueBuilder.durable(AI_RECOMMEND_QUEUE).build();
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public Binding aiRecommendBinding() {
        return BindingBuilder.bind(aiRecommendQueue()).to(aiExchange()).with(AI_RECOMMEND_ROUTING_KEY);
    }
}