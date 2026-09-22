package uk.gov.hmcts.opal.common.user.authorisation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;

@FeignClient(
    name = "azureActiveDirectoryClient",
    url = "${opal.common.system-users.token-url}"
)
public interface AzureActiveDirectoryClient {

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    AzureToken getSystemUser(@RequestBody GetSystemUserFormData formData);

    /**
     * Strongly-typed form payload for Azure system-user token requests.
     */
    final class GetSystemUserFormData extends LinkedMultiValueMap<String, String> {

        public GetSystemUserFormData(String clientId, String clientSecret, String scope, String grantType) {
            super(4);
            add("client_id", clientId);
            add("client_secret", clientSecret);
            add("scope", scope);
            add("grant_type", grantType);
        }
    }
}

