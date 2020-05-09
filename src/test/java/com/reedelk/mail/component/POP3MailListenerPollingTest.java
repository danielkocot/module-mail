package com.reedelk.mail.component;

import com.icegreen.greenmail.util.ServerSetup;
import com.reedelk.mail.component.pop3.POP3Protocol;
import com.reedelk.mail.internal.CloseableService;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icegreen.greenmail.util.ServerSetup.PORT_POP3;
import static com.icegreen.greenmail.util.ServerSetup.PROTOCOL_POP3;
import static org.assertj.core.api.Assertions.assertThat;

public class POP3MailListenerPollingTest extends AbstractMailTest {

    private ServerSetup serverSetup = new ServerSetup(1000 + PORT_POP3, null, PROTOCOL_POP3);

    private CloseableService closeableService = new CloseableService();

    private POP3MailListener listener;

    @BeforeEach
    void setUp() {
        super.setUp();
        POP3Configuration configuration = new POP3Configuration();
        configuration.setProtocol(POP3Protocol.POP3);
        configuration.setUsername(username);
        configuration.setPassword(password);
        configuration.setHost(address);
        configuration.setPort(serverSetup.getPort());

        listener = new POP3MailListener();
        listener.closeableService = closeableService;
        listener.setConnection(configuration);
        listener.setPollInterval(1000);
    }

    @Test
    void shouldCorrectlyPollAndMessageContainsFromSubjectAndBodyAndShouldNotDeleteByDefault() throws MessagingException, InterruptedException {
        // Given
        String from = "my-test@mydomain.com";
        String subject = "My sample subject";
        String body = "My sample body\r\n";

        // When
        deliverMessage(from, subject, body);

        // Then
        Optional<Message> maybeInputMessage = TestUtils.poll(listener, context);
        assertThat(maybeInputMessage).isPresent();

        Message inputMessage = maybeInputMessage.get();

        String payload = inputMessage.payload();
        assertThat(payload).isEqualTo(body);

        MessageAttributes attributes = inputMessage.getAttributes();
        assertThat(attributes).containsEntry("subject", subject);
        assertThat(attributes).containsEntry("from", from);

        MimeMessage mimeMessage = firstReceivedMessage();
        assertThat(mimeMessage).isNotNull();
    }

    @Test
    void shouldCorrectlyPollAndMarkDeleteMessageAfterPoll() throws MessagingException, InterruptedException {
        // Given
        String from = "my-test@mydomain.com";
        String subject = "My sample subject";
        String body = "My sample body";

        listener.setDeleteOnSuccess(true);

        // When
        deliverMessage(from, subject, body);

        // Then
        Optional<Message> maybeInputMessage = TestUtils.poll(listener, context);
        assertThat(maybeInputMessage).isPresent();

        assertReceivedMessagesIsEmpty();
    }

    @Test
    void shouldCorrectlyPollAndNotDeleteMessageWhenError() throws MessagingException, InterruptedException {
        // Given
        String from = "my-test@mydomain.com";
        String subject = "My sample subject";
        String body = "My sample body";

        listener.setDeleteOnSuccess(true);

        // When
        deliverMessage(from, subject, body);

        // Then
        Optional<Message> maybeInputMessage = TestUtils.pollAndOnResultError(listener, context);
        assertThat(maybeInputMessage).isPresent();

        MimeMessage mimeMessage = firstReceivedMessage();
        assertThat(mimeMessage).isNotNull();
    }

    @Test
    void shouldLimitNumberOfMessagesCorrectly() throws MessagingException, InterruptedException {
        // Given
        listener.setLimit(3);
        listener.setBatch(true);

        // When
        deliverRandomMessage();
        deliverRandomMessage();
        deliverRandomMessage();
        deliverRandomMessage();
        deliverRandomMessage();

        // Then
        Optional<Message> maybeInputMessage = TestUtils.poll(listener, context);
        assertThat(maybeInputMessage).isPresent();

        Message message = maybeInputMessage.get();
        List<Map<String,Object>> emails = message.payload();
        assertThat(emails).hasSize(3);
    }

    @Override
    protected ServerSetup serverSetup() {
        return serverSetup;
    }
}
