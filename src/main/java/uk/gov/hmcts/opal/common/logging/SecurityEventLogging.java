package uk.gov.hmcts.opal.common.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityEventLogging {

    public class ActionOutcome {
        public static final String PASS = "Pass";
        public static final String FAIL = "Fail";
        public static final String SUCCESS = "Success";
        public static final String FAILURE = "Failure";
    }

    public class EventName {
        public static final String SERVICE_TO_SERVICE_AUTH = "Service to Service Authentication";
        public static final String AUTHORISATION_ACCESS_CONTROL = "Authorisation Access Control";
    }

    public class EventOpType {
        public static final String AUTHENTICATION = "Authentication";
    }

    public class EventDataKey {
        public static final String USER_IDENTIFIER = "UserIdentifier";
        public static final String DETAILS = "Details";
        public static final String RESOURCE = "Resource";
        public static final String AUTHENTICATING_SERVICE = "Authenticating Service";
        public static final String TARGET_SERVICE = "Target Service";
    }

    public class EventDataValue {
        public static final String FILE_HANDLER_SERVICE_NAME = "opal-filehandler-service";
        public static final String FINES_SERVICE_NAME = "opal-fines-service";
    }
}
