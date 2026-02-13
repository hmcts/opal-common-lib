package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserStateClientService")
public class UserStateClientService {

    public static final long AUTHENTICATED_USER_SPECIAL_CODE = 0L;

    private final UserClient userClient;
    private final UserStateMapper userStateMapper;


    public Optional<UserState> getUserState(Long userId) {
        return fetchUserState(userId);
    }

    @Cacheable(value = "userState",
               key = "T(org.springframework.security.core.context.SecurityContextHolder)"
                   + ".getContext()?.getAuthentication()?.getName() ?: 'anonymous'")
    public Optional<UserState> getUserStateByAuthenticatedUser() {
        return fetchUserState(AUTHENTICATED_USER_SPECIAL_CODE);
    }

    @Cacheable(value = "userStateJwt")
    public Optional<UserState> getUserStateByAuthenticationToken(Jwt jwt) {
        return fetchUserState(jwt.getTokenValue(), AUTHENTICATED_USER_SPECIAL_CODE);
    }

    private Optional<UserState> fetchUserState(Long userId) {
        return fetchUserState(null, userId);
    }

    private Optional<UserState> fetchUserState(String authenticationToken, Long userId) {

        log.info(":getUserState: Fetching user state for specific userId: {}", userId);

        // Call the Feign client. Auth intercepted  - used to get authenticated user state if userId is 0.

        try {
            log.info(":getUserState: Fetching user state for userId: {}", userId);

            final String authToken;
            if (authenticationToken == null) {
                authToken = getAuthTokenFromContext();
            } else {
                authToken = authenticationToken;
            }


            UserStateDto userStateDto = userClient.getUserStateByIdWithAuthToken("Bearer " + authToken, userId);
            UserState userState = userStateMapper.toUserState(userStateDto);

            log.debug(":getUserState: Mapped UserState for userId {}: {}", userId, userState);
            return Optional.ofNullable(userState);

        } catch (FeignException.NotFound e) {
            log.warn(":getUserState: User not found in User Service for userId: {}", userId);
            return Optional.empty();
        }
    }

    private String getAuthTokenFromContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug(":requestInterceptor: authentication: {}", authentication);
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String token = jwtAuth.getToken().getTokenValue();
            return token;
        } else {
            log.warn(":requestInterceptor: Authentication not of type Jwt.");
        }
        return null;
    }
}
