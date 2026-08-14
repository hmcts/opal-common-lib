package uk.gov.hmcts.opal.common.user.authorisation.exception;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptorV2;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionNotAllowedExceptionTest {

    private enum TestPermission implements PermissionDescriptorV2 {
        SAMPLE("SAMPLE", "Sample Permission");

        private final String permissionCode;
        private final String permissionName;

        TestPermission(String code, String name) {
            this.permissionCode = code;
            this.permissionName = name;
        }

        @Override
        public String getPermissionCode() {
            return permissionCode;
        }

        @Override
        public String getPermissionName() {
            return permissionName;
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
        Short businessUnitId = 4;
        PermissionNotAllowedException exception = new PermissionNotAllowedException(
            businessUnitId,
            TestPermission.SAMPLE);

        assertEquals(
            "[" + TestPermission.SAMPLE + "] permission(s) are not enabled for the user in business unit: 4",
            exception.getMessage());
    }
}
