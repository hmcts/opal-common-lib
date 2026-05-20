package uk.gov.hmcts.opal.common.user.authorisation.exception;

import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

import java.util.Arrays;
import java.util.Collection;

@Getter
public class PermissionNotAllowedException extends RuntimeException {

    private final PermissionDescriptor[] permission;
    private final Short businessUnitId;

    protected PermissionNotAllowedException(String message, Short businessUnitId, PermissionDescriptor... value) {
        super(message);
        this.permission = value;
        this.businessUnitId = businessUnitId;
    }

    public PermissionNotAllowedException(PermissionDescriptor... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user.", null, value);
    }

    public PermissionNotAllowedException(Short businessUnitId, PermissionDescriptor... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user in business unit: "
            + businessUnitId, businessUnitId, value);
    }

    public PermissionNotAllowedException(Collection<Short> businessUnitIds, PermissionDescriptor... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user in business units: "
            + businessUnitIds, null, value);
    }
}
