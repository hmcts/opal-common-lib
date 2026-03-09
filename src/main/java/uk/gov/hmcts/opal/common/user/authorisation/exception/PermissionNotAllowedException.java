package uk.gov.hmcts.opal.common.user.authorisation.exception;

import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

import java.util.Arrays;

@Getter
public class PermissionNotAllowedException extends RuntimeException {

    private final PermissionDescriptor[] permission;
    private final Short businessUnitId;

    public PermissionNotAllowedException(PermissionDescriptor... value) {
        super(Arrays.toString(value) + " permission(s) are not enabled for the user.");
        this.permission = value;
        this.businessUnitId = null;
    }

    public PermissionNotAllowedException(Short businessUnitId, PermissionDescriptor... value) {
        super(Arrays.toString(value)
            + " permission(s) are not enabled for the user in business unit: " + businessUnitId);
        this.permission = value;
        this.businessUnitId = businessUnitId;
    }
}
