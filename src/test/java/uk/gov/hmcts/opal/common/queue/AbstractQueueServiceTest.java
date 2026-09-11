package uk.gov.hmcts.opal.common.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;

class AbstractQueueServiceTest {

    private static final String QUEUE_NAME = "interface-jobs";

    @Test
    void send_publishesSerializedPayload() {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AbstractQueueService<String> queueService = new AbstractQueueService<>(jmsTemplate, objectMapper, QUEUE_NAME);

        when(objectMapper.writeValueAsString("payload")).thenReturn("\"payload\"");

        queueService.send("payload");

        verify(objectMapper).writeValueAsString("payload");
        verify(jmsTemplate).convertAndSend(QUEUE_NAME, "\"payload\"");
    }

    @Test
    void send_onError_throwsInternalServerErrorExceptionWithTitleAndDetail() {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AbstractQueueService<String> queueService = new AbstractQueueService<>(jmsTemplate, objectMapper, QUEUE_NAME);
        RuntimeException exception = new RuntimeException("cannot serialize");

        when(objectMapper.writeValueAsString("payload")).thenThrow(exception);

        assertThatThrownBy(() -> queueService.send("payload"))
            .isInstanceOf(InternalServerErrorException.class)
            .extracting(ex -> (InternalServerErrorException) ex)
            .satisfies(ex -> {
                assertThat(ex.getTitle()).isEqualTo("Internal Queue Error");
                assertThat(ex.getDetail()).isEqualTo("Unable to publish interface job messages");
                assertThat(ex.getCause()).isSameAs(exception);
            });

        verify(jmsTemplate, never()).convertAndSend(QUEUE_NAME, "\"payload\"");
    }

    @Test
    void sendBatch_publishesAllMessagesAndCommitsSession() throws JMSException {
        Session session = mock(Session.class);
        Queue queue = mock(Queue.class);
        MessageProducer producer = mock(MessageProducer.class);
        TextMessage firstTextMessage = mock(TextMessage.class);
        TextMessage secondTextMessage = mock(TextMessage.class);
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        final AbstractQueueService<String> queueService =
            new AbstractQueueService<>(jmsTemplate, objectMapper, QUEUE_NAME);

        when(session.createQueue(QUEUE_NAME)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(producer);
        when(objectMapper.writeValueAsString("first")).thenReturn("\"first\"");
        when(objectMapper.writeValueAsString("second")).thenReturn("\"second\"");
        when(session.createTextMessage("\"first\"")).thenReturn(firstTextMessage);
        when(session.createTextMessage("\"second\"")).thenReturn(secondTextMessage);

        queueService.sendBatch(session, List.of("first", "second"));

        verify(producer).send(firstTextMessage);
        verify(producer).send(secondTextMessage);
        verify(session).commit();
        verify(session, never()).rollback();
        verify(producer).close();
    }

    @Test
    void sendBatch_whenJmsFailureOccurs_rollsBackAndThrowsInternalServerErrorExceptionWithTitleAndDetail()
        throws JMSException {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AbstractQueueService<String> queueService = new AbstractQueueService<>(jmsTemplate, objectMapper, QUEUE_NAME);
        Session session = mock(Session.class);
        JMSException publishException = new JMSException("cannot create producer");
        Queue queue = mock(Queue.class);
        List<String> payloads = List.of("payload");

        when(session.createQueue(QUEUE_NAME)).thenReturn(queue);
        when(session.createProducer(queue)).thenThrow(publishException);

        assertThatThrownBy(() -> queueService.sendBatch(session, payloads))
            .isInstanceOf(InternalServerErrorException.class)
            .extracting(ex -> (InternalServerErrorException) ex)
            .satisfies(ex -> {
                assertThat(ex.getTitle()).isEqualTo("Internal Queue Error");
                assertThat(ex.getDetail()).isEqualTo("Unable to publish interface job messages");
                assertThat(ex.getCause()).isSameAs(publishException);
            });

        verify(session).rollback();
        verify(session, never()).commit();
    }

    @Test
    void rollback_whenRollbackFails_addsSuppressedExceptionToCause() throws JMSException {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AbstractQueueService<String> queueService = new AbstractQueueService<>(jmsTemplate, objectMapper, QUEUE_NAME);
        Session session = mock(Session.class);
        JMSException cause = new JMSException("publish failed");
        JMSException rollbackFailure = new JMSException("rollback failed");

        doThrow(rollbackFailure).when(session).rollback();

        queueService.rollback(session, cause);

        assertThat(cause.getSuppressed()).containsExactly(rollbackFailure);
    }
}


