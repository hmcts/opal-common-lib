package uk.gov.hmcts.opal.common.user.authorisation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.opal.common.user.authentication.model.SecurityToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.config.UserTokenRelayConfig;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(
    name = "userService",
    url = "${user.service.url}",
    configuration = UserTokenRelayConfig.class
)
public interface UserClient {

    String X_USER_EMAIL = "X-User-Email";

    @GetMapping("/users/{id}/state")
    UserStateDto getUserStateByIdWithAuthToken(
        @RequestHeader(AUTHORIZATION) String bearerAuth,
        @PathVariable("id") Long id);

    @GetMapping("/testing-support/token/test-user")
    SecurityToken getTestUserToken();

    @GetMapping("/testing-support/token/user")
    SecurityToken getTestUserToken(@RequestHeader(value = X_USER_EMAIL) String userEmail);
}
