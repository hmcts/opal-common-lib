package uk.gov.hmcts.opal.common.queue;

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;

@Slf4j
public class AbstractQueueService<T> {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;


    public AbstractQueueService(
        @Qualifier("commonServiceBusJmsTemplate") JmsTemplate jmsTemplate,
        ObjectMapper objectMapper,
        String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }


    /**
     * Send a single message to the queue.
     */
    public void send(T message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend(queueName, payload);
        } catch (Exception e) {
            throw new InternalServerErrorException(
                "Internal Queue Error",
                "Unable to publish interface job messages", e);
        }
    }

    /**
     * Send a batch of messages to the queue. All or nothing is published, if any message fails to publish, the entire
     * batch is rolled back.
     */
    public void sendBatch(Session session, List<T> payloads) {
        try {
            sendMessages(session, payloads);
            session.commit();
        } catch (Exception e) {
            rollback(session, e);
            throw new InternalServerErrorException(
                "Internal Queue Error",
                "Unable to publish interface job messages", e);
        }
    }

    void sendMessages(Session session, List<T> messages) throws JMSException {
        try (MessageProducer producer =
            session.createProducer(session.createQueue(queueName))) {
            for (T message : messages) {
                producer.send(session.createTextMessage(objectMapper.writeValueAsString(message)));
            }
        }
    }

    void rollback(Session session, Exception cause) {
        try {
            session.rollback();
        } catch (Exception rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
