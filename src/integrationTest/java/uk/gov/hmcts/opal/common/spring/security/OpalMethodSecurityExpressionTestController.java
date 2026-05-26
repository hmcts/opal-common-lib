package uk.gov.hmcts.opal.common.spring.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpalMethodSecurityExpressionTestController {

    private final OpalMethodSecurityExpressionTestService testService;

    OpalMethodSecurityExpressionTestController(OpalMethodSecurityExpressionTestService testService) {
        this.testService = testService;
    }

    @GetMapping("/test/permission")
    public String getPermissionProtected() {
        return testService.requiresPermission();
    }

    @GetMapping("/test/business-units/{businessUnitId}")
    public String getBusinessUnitProtected(@PathVariable String businessUnitId) {
        return testService.requiresBusinessUnit(businessUnitId);
    }

    @GetMapping("/test/business-units/{businessUnitId}/permission")
    public String getBusinessUnitPermissionProtected(@PathVariable String businessUnitId) {
        return testService.requiresPermissionInBusinessUnit(businessUnitId);
    }
}
