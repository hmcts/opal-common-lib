package uk.gov.hmcts.opal.common.user.authorisation.exception;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionNotAllowedExceptionTest {

    private enum TestPermission implements PermissionDescriptor {
        SAMPLE(1L, "Sample Permission");

        private final long id;
        private final String description;

        TestPermission(long id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    @Test
    void constructor_ShouldSetPermission() {
        PermissionNotAllowedException exception = new PermissionNotAllowedException(TestPermission.SAMPLE);

        assertEquals(TestPermission.SAMPLE, exception.getPermission()[0]);
    }

    @Test
    void constructor_ShouldSetMessage() {
        PermissionNotAllowedException exception = new PermissionNotAllowedException(TestPermission.SAMPLE);

        assertEquals("[" + TestPermission.SAMPLE + "] permission(s) are not enabled for the user.",
                     exception.getMessage());
    }

    @Test
    void constructorWithBusinessUnit_ShouldSetMessage() {
        BusinessUnitUser user = BusinessUnitUser.builder().businessUnitUserId("BU123").build();
        PermissionNotAllowedException exception = new PermissionNotAllowedException(TestPermission.SAMPLE, user);

        assertEquals(TestPermission.SAMPLE + " permission is not enabled for the business unit user: BU123",
                     exception.getMessage());
    }
}
