package uk.gov.hmcts.opal.common.user.authorisation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.opal.common.user.authorisation.client.config.UserTokenRelayConfig;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;

@FeignClient(
    name = "azureActiveDirectoryClient",
    url = "${opal.common.system-users.token-url}",
    configuration = UserTokenRelayConfig.class
)
public interface AzureActiveDirectoryClient {

    @GetMapping
    AzureToken getSystemUser(@RequestParam("client_id") String clientId,
        @RequestParam("client_secret") String clientSecret,
        @RequestParam("scope") String scope,
        @RequestParam("grant_type") String grantType);
}
