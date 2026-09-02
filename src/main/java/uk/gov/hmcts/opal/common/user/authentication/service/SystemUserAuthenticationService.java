package uk.gov.hmcts.opal.common.user.authentication.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.user.authorisation.client.AzureActiveDirectoryClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;

@Service
@AllArgsConstructor
public class SystemUserAuthenticationService {

    private final OpalCommonConfiguration opalCommonConfiguration;
    private final AzureActiveDirectoryClient azureActiveDirectoryClient;

    public String getSystemUserAuthenticationToken(SystemUserEnum systemUserEnum) {
        OpalCommonConfiguration.SystemUser systemUser = getSystemUser(systemUserEnum);
        return getAzureToken(systemUser).getAccessToken();
    }

    public String getClientId(SystemUserEnum systemUserEnum) {
        return getSystemUser(systemUserEnum).getClientId();
    }

    private OpalCommonConfiguration.SystemUser getSystemUser(SystemUserEnum systemUserEnum) {
        OpalCommonConfiguration.SystemUser systemUser =
            opalCommonConfiguration.getSystemUsers().getUsers().get(systemUserEnum.getConfigKey());
        if (systemUser == null) {
            throw new UnauthorizedException("Unauthorized",
                "System user not found for enum: " + systemUserEnum.getConfigKey());
        }
        return systemUser;
    }

    private AzureToken getAzureToken(OpalCommonConfiguration.SystemUser systemUser) {
        return azureActiveDirectoryClient.getSystemUser(
            systemUser.getClientId(),
            systemUser.getClientSecret(),
            systemUser.getScope(),
            systemUser.getGrantType()
        );
    }
}
