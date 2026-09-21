package uk.gov.hmcts.opal.common.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.opal.common.config.ServiceBusConnectionStringParser.ConnectionDetails;

class ServiceBusConnectionStringParserTest {

    private static final String BASE_CONNECTION_STRING = "%s=%s;%s=%s;%s=%s";
    private static final String ENDPOINT_KEY = "Endpoint";
    private static final String KEY_VALUE_KEY = "SharedAccessKey";
    private static final String KEY_NAME_KEY = "SharedAccessKeyName";
    private static final String VALID_ENDPOINT = "sb://localhost/;";
    private static final String VALID_SHARED_ACCESS_KEY_NAME = "AccessKey";
    private static final String VALID_SHARED_ACCESS_KEY_VALUE = "keyValue";

    ServiceBusConnectionStringParser serviceBusConnectionStringParser;

    @BeforeEach
    void setUp() {
        serviceBusConnectionStringParser = new ServiceBusConnectionStringParser(new ServiceBusProperties());
    }

    static Stream<Arguments> provideNullEmptyAndBlankConnectionStrings() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of(" "),
            Arguments.of("\t"),
            Arguments.of("\n")
        );
    }

    static Stream<Arguments> provideMissingOrBlankEndpoint() {
        return Stream.of(
            Arguments.of("SharedAccessKeyName=AccessKey;SharedAccessKey=keyValue"),
            Arguments.of("Endpoint= ;SharedAccessKeyName=AccessKey;SharedAccessKey=keyValue")
        );
    }

    static Stream<Arguments> provideMissingOrBlankSharedAccessKeyName() {
        return Stream.of(
            Arguments.of("Endpoint=sb://localhost/;SharedAccessKey=keyValue"),
            Arguments.of("Endpoint=sb://localhost/;SharedAccessKeyName= ;SharedAccessKey=keyValue")
        );
    }

    static Stream<Arguments> provideMissingOrBlankSharedAccessKeyValue() {
        return Stream.of(
            Arguments.of("Endpoint=sb://localhost/;SharedAccessKeyName=AccessKey"),
            Arguments.of("Endpoint=sb://localhost/;SharedAccessKeyName=AccessKey;SharedAccessKey= ")
        );
    }

    @Test
    void parse_validConnectionString_returnsConnectionDetails() {
        ConnectionDetails parsed = serviceBusConnectionStringParser.parse(
            String.format(BASE_CONNECTION_STRING,
                ENDPOINT_KEY, VALID_ENDPOINT,
                KEY_NAME_KEY, VALID_SHARED_ACCESS_KEY_NAME,
                KEY_VALUE_KEY, VALID_SHARED_ACCESS_KEY_VALUE));
        assertThat(parsed.fullyQualifiedNamespace()).isEqualTo("localhost");
        assertThat(parsed.sharedAccessKeyName()).isEqualTo(VALID_SHARED_ACCESS_KEY_NAME);
        assertThat(parsed.sharedAccessKey()).isEqualTo(VALID_SHARED_ACCESS_KEY_VALUE);
    }

    @Test
    void parse_connectionStringWithEmptySegments_returnsConnectionDetails() {
        ConnectionDetails parsed = serviceBusConnectionStringParser.parse(
            "Endpoint=sb://servicebus.windows.net/;;SharedAccessKeyName=AccessKey;SharedAccessKey=keyValue;"
        );

        assertThat(parsed.fullyQualifiedNamespace()).isEqualTo("servicebus.windows.net");
        assertThat(parsed.sharedAccessKeyName()).isEqualTo("AccessKey");
        assertThat(parsed.sharedAccessKey()).isEqualTo("keyValue");
    }

    @ParameterizedTest
    @MethodSource("provideNullEmptyAndBlankConnectionStrings")
    void parse_noConnectionStringPassed_throwsIllegalArgumentException(String connectionString) {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));
        assertThat(ex).hasMessageContaining("must not be blank");
    }

    @ParameterizedTest
    @MethodSource("provideMissingOrBlankEndpoint")
    void parse_endpointMissingOrBlank_throwsIllegalArgumentException(String connectionString) {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));

        assertThat(ex).hasMessageContaining("missing endpoint segment");
    }

    @Test
    void parse_invalidUriInEndpoint_throwsIllegalArgumentException() {
        String connectionString = String.format(BASE_CONNECTION_STRING,
            ENDPOINT_KEY, "invalid uri",
            KEY_NAME_KEY, VALID_SHARED_ACCESS_KEY_NAME,
            KEY_VALUE_KEY, VALID_SHARED_ACCESS_KEY_VALUE);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));

        assertThat(ex).hasMessageContaining("invalid uri");
    }

    @Test
    void parse_noHostInEndpoint_throwsIllegalArgumentException() {
        String connectionString = String.format(BASE_CONNECTION_STRING,
            ENDPOINT_KEY, "sb:///",
            KEY_NAME_KEY, VALID_SHARED_ACCESS_KEY_NAME,
            KEY_VALUE_KEY, VALID_SHARED_ACCESS_KEY_VALUE);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));

        assertThat(ex).hasMessageContaining("missing host");
    }

    @ParameterizedTest
    @MethodSource("provideMissingOrBlankSharedAccessKeyName")
    void parse_sharedAccessKeyNameMissingOrBlank_throwsIllegalArgumentException(String connectionString) {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));

        assertThat(ex).hasMessageContaining("missing " + KEY_NAME_KEY);
    }

    @ParameterizedTest
    @MethodSource("provideMissingOrBlankSharedAccessKeyValue")
    void parse_sharedAccessKeyMissingOrBlank_throwsIllegalArgumentException(String connectionString) {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> serviceBusConnectionStringParser.parse(connectionString));

        assertThat(ex).hasMessageContaining("missing " + KEY_VALUE_KEY);
    }

    @Test
    void toRemoteUri_withNamespace_returnsExpectedUri() {
        ServiceBusProperties properties = new ServiceBusProperties();
        properties.setProtocol("amqps");
        properties.setSendTimeoutMs(1234);
        properties.setIdleTimeoutMs(5678);

        String remoteUri = serviceBusConnectionStringParser
            .toRemoteUri(properties, "namespace.servicebus.windows.net");

        assertThat(remoteUri)
            .isEqualTo("amqps://namespace.servicebus.windows.net?jms.sendTimeout=1234&amqp.idleTimeout=5678");
    }

    @Test
    void toRemoteUri_withConnectionDetails_returnsExpectedUri() {
        ServiceBusProperties properties = new ServiceBusProperties();
        properties.setProtocol("amqp");
        properties.setSendTimeoutMs(10000);
        properties.setIdleTimeoutMs(30000);
        ConnectionDetails details = new ConnectionDetails(
            "my-namespace.servicebus.windows.net",
            VALID_SHARED_ACCESS_KEY_NAME,
            VALID_SHARED_ACCESS_KEY_VALUE
        );

        String remoteUri = serviceBusConnectionStringParser.toRemoteUri(properties, details);

        assertThat(remoteUri)
            .isEqualTo("amqp://my-namespace.servicebus.windows.net?jms.sendTimeout=10000&amqp.idleTimeout=30000");
    }

}