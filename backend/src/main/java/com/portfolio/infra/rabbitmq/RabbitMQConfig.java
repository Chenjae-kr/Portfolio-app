package com.portfolio.infra.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host", matchIfMissing = false)
@org.springframework.context.annotation.Profile("!dev & !test")  // 개발/테스트 모드에서는 비활성화
public class RabbitMQConfig {

    @Value("${app.backtest.queue-name}")
    private String backtestQueueName;

    public static final String VALUATION_QUEUE = "valuation-recompute";
    public static final String EXCHANGE_NAME = "portfolio-exchange";

    @Bean
    public Queue backtestQueue() {
        return QueueBuilder.durable(backtestQueueName)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME + ".dlx")
                .build();
    }

    @Bean
    public Queue valuationQueue() {
        return QueueBuilder.durable(VALUATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME + ".dlx")
                .build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding backtestBinding(Queue backtestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(backtestQueue)
                .to(exchange)
                .with("backtest");
    }

    @Bean
    public Binding valuationBinding(Queue valuationQueue, DirectExchange exchange) {
        return BindingBuilder.bind(valuationQueue)
                .to(exchange)
                .with("valuation");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
