package uk.gov.hmcts.opal.common.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import java.util.Objects;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.opal.common.config.ServiceBusConnectionStringParser;
import uk.gov.hmcts.opal.common.config.ServiceBusProperties;

@ExtendWith(MockitoExtension.class)
class QueueBeansTest {

    @Mock
    ServiceBusConnectionStringParser parser;


    @InjectMocks
    QueueBeans queueBeans;

    @Test
    void interfaceJobPublisherConnectionFactory_configuresQpidAndCachingConnectionFactory() {
        ServiceBusProperties properties = new ServiceBusProperties();
        properties.setConnectionString(
            "Endpoint=sb://ignored.servicebus.windows.net/;SharedAccessKeyName=name;SharedAccessKey=key");
        properties.setProtocol("amqps");
        properties.setSendTimeoutMs(2345);
        properties.setIdleTimeoutMs(6789);

        ServiceBusConnectionStringParser.ConnectionDetails details =
            new ServiceBusConnectionStringParser.ConnectionDetails(
                "namespace.servicebus.windows.net",
                "RootManageSharedAccessKey",
                "secret-value"
            );
        when(parser.parse(properties.getConnectionString())).thenReturn(details);

        ConnectionFactory connectionFactory = queueBeans.interfaceJobPublisherConnectionFactory(properties);

        assertThat(connectionFactory).isInstanceOf(CachingConnectionFactory.class);
        CachingConnectionFactory cachingConnectionFactory = (CachingConnectionFactory) connectionFactory;
        assertThat(cachingConnectionFactory.isCacheProducers()).isTrue();
        assertThat((Boolean) ReflectionTestUtils.getField(cachingConnectionFactory, "reconnectOnException")).isTrue();
        assertThat(cachingConnectionFactory.getTargetConnectionFactory()).isInstanceOf(JmsConnectionFactory.class);

        JmsConnectionFactory qpidConnectionFactory =
            (JmsConnectionFactory) cachingConnectionFactory.getTargetConnectionFactory();
        String remoteUri = Objects.requireNonNull(qpidConnectionFactory.getRemoteURI());
        assertThat(remoteUri).isEqualTo("amqps://namespace.servicebus.windows.net?amqp.idleTimeout=6789");
        assertThat(qpidConnectionFactory.getSendTimeout()).isEqualTo(2345L);
        assertThat(qpidConnectionFactory.getUsername()).isEqualTo("RootManageSharedAccessKey");
        assertThat(qpidConnectionFactory.getPassword()).isEqualTo("secret-value");

        verify(parser).parse(properties.getConnectionString());
    }

    @Test
    void interfaceJobPublisherJmsTemplate_configuresExpectedDefaults() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        JmsTemplate jmsTemplate = queueBeans.interfaceJobPublisherJmsTemplate(connectionFactory);

        assertThat(jmsTemplate.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(jmsTemplate.getDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
        assertThat(jmsTemplate.isExplicitQosEnabled()).isTrue();
        assertThat(jmsTemplate.isSessionTransacted()).isTrue();
    }
}




