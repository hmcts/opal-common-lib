package uk.gov.hmcts.opal.common.user.authorisation.model;

public interface PermissionDescriptorV2 {

    /**
     * The permission code.
     */
    String getPermissionCode();

    /**
     * The permission name.
     */
    String getPermissionName();
}
