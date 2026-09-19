package uk.gov.hmcts.opal.common.user.authorisation.exception;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptorV2;

@Getter
public class PermissionNotAllowedException extends RuntimeException {

    private final PermissionDescriptorV2[] permission;
    private final Short businessUnitId;

    protected PermissionNotAllowedException(String message, Short businessUnitId, PermissionDescriptorV2... value) {
        super(message);
        this.permission = value;
        this.businessUnitId = businessUnitId;
    }

    public PermissionNotAllowedException(PermissionDescriptorV2... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user.", null, value);
    }

    public PermissionNotAllowedException(Short businessUnitId, PermissionDescriptorV2... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user in business unit: "
            + businessUnitId, businessUnitId, value);
    }

    public PermissionNotAllowedException(Collection<Short> businessUnitIds, PermissionDescriptorV2... value) {
        this(Arrays.toString(value) + " permission(s) are not enabled for the user in business units: "
            + businessUnitIds, null, value);
    }
}
