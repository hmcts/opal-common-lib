package uk.gov.hmcts.opal.common.user.authorisation.exception;

import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

import java.util.Arrays;
import java.util.Collection;

@Getter
public class PermissionNotAllowedException extends RuntimeException {

    private final PermissionDescriptor[] permission;
    private final BusinessUnitUser businessUnitUser;

    public PermissionNotAllowedException(PermissionDescriptor... value) {
        super(Arrays.toString(value) + " permission(s) are not enabled for the user.");
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(Short buIds, PermissionDescriptor... value) {
        super(Arrays.toString(value) + " permission(s) are not enabled in business unit: " + buIds);
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(Collection<Short> buIds, PermissionDescriptor... value) {
        super(Arrays.toString(value) + " permission(s) are not enabled in business units: " + buIds);
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(PermissionDescriptor permission,
                                         BusinessUnitUser businessUnitUser) {
        super(permission + " permission is not enabled for the business unit user: "
                  + businessUnitUser.getBusinessUnitUserId());
        this.permission = new PermissionDescriptor[] {permission};
        this.businessUnitUser = businessUnitUser;
    }
}
