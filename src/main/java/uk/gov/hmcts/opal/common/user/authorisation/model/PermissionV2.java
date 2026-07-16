package uk.gov.hmcts.opal.common.user.authorisation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.launchdarkly.shaded.org.checkerframework.checker.nullness.qual.NonNull;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum PermissionV2 implements PermissionDescriptorV2 {

    //  Enumerations:
    ACCOUNT_ENQUIRY("ACCOUNT_ENQUIRY", "Account Enquiry"),
    ACCOUNT_ENQUIRY_NOTES("ACCOUNT_ENQUIRY_NOTES", "Account Enquiry - Account Notes"),
    ACCOUNT_MAINTENANCE("ACCOUNT_MAINTENANCE", "Account Maintenance"),
    ADD_ACCOUNT_ACTIVITY_NOTES("ADD_ACCOUNT_ACTIVITY_NOTES", "Add Account Activity Notes"),
    ADD_AND_REMOVE_PAYMENT_HOLD("ADD_AND_REMOVE_PAYMENT_HOLD", "Add and Remove payment hold"),
    AMEND_PAYMENT_TERMS("AMEND_PAYMENT_TERMS", "Amend Payment Terms"),
    CHECK_VALIDATE_DRAFT_ACCOUNTS("CHECK_VALIDATE_DRAFT_ACCOUNTS", "Check and Validate Draft Accounts"),
    COLLECTION_ORDER("COLLECTION_ORDER", "Collection Order"),
    CONSOLIDATE("CONSOLIDATE", "Consolidate"),
    CREATE_MANAGE_DRAFT_ACCOUNTS("CREATE_MANAGE_DRAFT_ACCOUNTS", "Create and Manage Draft Accounts"),
    ENTER_ENFORCEMENT("ENTER_ENFORCEMENT", "Enter Enforcement"),
    OPERATIONAL_REPORT_BY_ENFORCEMENT("OPERATIONAL_REPORT_BY_ENFORCEMENT", "Operational Report by Enforcement"),
    OPERATIONAL_REPORT_BY_PAYMENTS("OPERATIONAL_REPORT_BY_PAYMENTS", "Operational report by payments"),
    SEARCH_AND_VIEW_ACCOUNTS("SEARCH_AND_VIEW_ACCOUNTS", "Search and view accounts"),
    VIEW_CREDITOR_BACS("VIEW_CREDITOR_BACS", "View creditor BACS"),
    AUTO_ENFORCEMENT("AUTO_ENFORCEMENT", "Auto Enforcement");

    //  Properties:
    @JsonProperty("permission_code")
    @NonNull
    private final String permissionCode;

    @JsonProperty("permission_name")
    @NonNull
    private final String permissionName;

    /**
     * Enumeration constructor.
     *
     * @param permissionCode                The enumeration code
     * @param permissionName                The enumeration name
     */
    private PermissionV2(String permissionCode, String permissionName) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
    }

    boolean matchesPermissions(PermissionDescriptorV2 candidate) {
        return Objects.equals(candidate.getPermissionCode(), permissionCode);
    }

    public static PermissionV2 fromPermissionCode(String permissionCode) {
        return Stream.of(PermissionV2.values())
                    .filter(permission -> permission.permissionCode.equals(permissionCode))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown PermissionV2 code: " + permissionCode));
    }

    public static PermissionV2 fromPermissionName(String permissionName) {
        return Stream.of(PermissionV2.values())
            .filter(permission -> permission.permissionName.equals(permissionName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PermissionV2 code: " + permissionName));
    }
}
