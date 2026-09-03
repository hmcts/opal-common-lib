package uk.gov.hmcts.opal.common.user.authentication.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.AzureActiveDirectoryClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.AzureToken;
import uk.gov.hmcts.opal.common.util.SecurityUtil;

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

    public Optional<SystemUserEnum> getCurrentSystemUser() {
        OpalJwtAuthenticationToken token = SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser();
        //get appid from jwt claims
        String appId = token.getToken().getClaimAsString("appid");
        if (appId == null) {
            return Optional.empty();
        }
        return opalCommonConfiguration.getSystemUsers()
            .getUsers()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getClientId().equals(appId))
            .findFirst()
            .flatMap(entry -> SystemUserEnum.fromConfigKey(entry.getKey()));
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
