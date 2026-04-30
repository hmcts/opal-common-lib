package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimNames;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserStateClientService")
public class UserStateClientService {

    public static final long AUTHENTICATED_USER_SPECIAL_CODE = 0L;

    private final UserClient userClient;
    private final UserStateMapper userStateMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<UserState> getUserState(Long userId) {
        return fetchUserState(userId);
    }

    @Cacheable(value = "userState",
        key = "T(org.springframework.security.core.context.SecurityContextHolder)"
            + ".getContext()?.getAuthentication()?.getName() ?: 'anonymous'")
    public Optional<UserState> getUserStateByAuthenticatedUser() {
        return fetchUserState(AUTHENTICATED_USER_SPECIAL_CODE);
    }

    private Optional<UserState> fetchUserState(Long userId) {

        log.info(":getUserState: Fetching user state for specific userId: {}", userId);

        // Call the Feign client. Auth intercepted  - used to get authenticated user state if userId is 0.

        try {
            log.info(":getUserState: Fetching user state for userId: {}", userId);

            UserStateDto userStateDto = userClient.getUserStateById(userId);
            UserState userState = userStateMapper.toUserState(userStateDto);

            log.debug(":getUserState: Mapped UserState for userId {}: {}", userId, userState);
            return Optional.of(userState);

        } catch (FeignException.NotFound e) {
            log.warn(":getUserState: User not found in User Service for userId: {}", userId);
            return Optional.empty();
        }
    }

    public Optional<UserStateV2> getUserStateByAuthenticationToken(Jwt jwt) {

        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        log.debug(":getUserStateByAuthenticationToken: Fetching user state for subject: {}", tokenSubject);

        Optional<UserStateV2Dto> userStateV2Dto = getUserStateFromCache(jwt);
        if (userStateV2Dto.isEmpty()) {
            userStateV2Dto = getUserStateFromUserService(jwt);
        }
        if (userStateV2Dto.isPresent()) {
            UserStateV2 userState = userStateMapper.toUserStateV2(userStateV2Dto.get());
            return Optional.of(userState);
        }
        return Optional.empty();
    }

    private Optional<UserStateV2Dto> getUserStateFromCache(Jwt jwt) {

        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        String cacheKey = "USER_STATE_" + tokenSubject;
        String cachedUserState;
        try {
            cachedUserState = redisTemplate.opsForValue().get(cacheKey);
        } catch (DataAccessException e) {
            log.warn("Could not access Redis", e);
            return Optional.empty();
        }

        if (cachedUserState == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cachedUserState, UserStateV2Dto.class));
        } catch (JsonProcessingException e) {
            log.warn(":getUserState: could not parse user state from cache: {}", tokenSubject);
            return Optional.empty();
        }
    }

    private Optional<UserStateV2Dto> getUserStateFromUserService(Jwt jwt) {
        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        try {
            UserStateV2Dto userStateV2Dto = userClient.getUserStateByIdWithAuthToken("Bearer " + jwt.getTokenValue());
            return Optional.of(userStateV2Dto);
        } catch (FeignException.NotFound e) {
            log.warn(":getUserState: User not found in User Service for subject: {}", tokenSubject);
            return Optional.empty();
        }
    }
}
