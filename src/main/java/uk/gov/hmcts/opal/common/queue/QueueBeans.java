package uk.gov.hmcts.opal.common.queue;

import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.opal.common.config.ServiceBusConnectionStringParser;
import uk.gov.hmcts.opal.common.config.ServiceBusProperties;

@ConditionalOnProperty("opal.common.service-bus.enabled")
@Slf4j
@EnableJms
@Configuration
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class QueueBeans {

    private final ServiceBusConnectionStringParser serviceBusConnectionStringParser;

    @Bean("commonServiceBusConnectionFactory")
    public ConnectionFactory interfaceJobPublisherConnectionFactory(ServiceBusProperties properties) {
        ServiceBusConnectionStringParser.ConnectionDetails details =
            serviceBusConnectionStringParser.parse(properties.getConnectionString());
        String remoteUri = "%s://%s?jms.sendTimeout=%d&amqp.idleTimeout=%d".formatted(
            properties.getProtocol(),
            details.fullyQualifiedNamespace(),
            properties.getSendTimeoutMs(),
            properties.getIdleTimeoutMs());

        JmsConnectionFactory qpidFactory = new JmsConnectionFactory(remoteUri);
        qpidFactory.setUsername(details.sharedAccessKeyName());
        qpidFactory.setPassword(details.sharedAccessKey());

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(qpidFactory);
        cachingFactory.setCacheProducers(true);
        cachingFactory.setReconnectOnException(true);
        return cachingFactory;
    }

    @Bean("commonServiceBusJmsTemplate")
    public JmsTemplate interfaceJobPublisherJmsTemplate(
        @Qualifier("commonServiceBusConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setSessionTransacted(true);
        return jmsTemplate;
    }
}
