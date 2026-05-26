package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class OpalMethodSecurityExpressionTestService {

    @PreAuthorize("hasPermission('TEST_PERM')")
    public String requiresPermission() {
        return "ok";
    }

    @PreAuthorize("hasBusinessUnit(#businessUnitId)")
    public String requiresBusinessUnit(String businessUnitId) {
        return "ok " + businessUnitId;
    }

    @PreAuthorize("hasPermissionInBusinessUnit('TEST_PERM', #businessUnitId)")
    public String requiresPermissionInBusinessUnit(String businessUnitId) {
        return "ok " + businessUnitId;
    }
}
