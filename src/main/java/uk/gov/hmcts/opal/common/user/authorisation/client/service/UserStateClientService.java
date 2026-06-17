package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import com.nimbusds.jwt.JWTClaimNames;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.exception.DownstreamServiceUnavailableException;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserStateClientService")
public class UserStateClientService {
    public static final String USER_STATE_CACHE_PREFIX = "USER_STATE_";
    public static final long CURRENT_USER_ID = 0L;
    public static final String FEATURE_DISABLED_PROBLEM_TYPE = "https://hmcts.gov.uk/problems/feature-disabled";
    public static final String FEATURE_DISABLED_PROBLEM_TITLE = "Feature Disabled";
    public static final String USER_SERVICE_ENDPOINT_DISABLED_DETAIL =
        "The required user-service endpoint is disabled.";

    private final UserClient userClient;
    private final UserStateMapper userStateMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    public Optional<UserStateV2> getUserStateByAuthenticatedUser() {
        log.info(":getUserState: Fetching user state for specific userId: 0");

        // Call the Feign client. Auth intercepted  - used to get authenticated user state if userId is 0.

        try {
            log.info(":getUserState: Fetching user state for userId: 0");

            UserStateV2Dto userStateDto = userClient.getUserState(CURRENT_USER_ID);
            UserStateV2 userState = userStateMapper.toUserStateV2(userStateDto);

            log.debug(":getUserState: Mapped UserState for userId 0: {}", userState);
            return Optional.of(userState);

        } catch (FeignException e) {
            Optional<UserStateV2Dto> userStateDto = handleUserServiceLookupException(
                e,
                String.format(":getUserState: User not found in User Service for userId: %d", CURRENT_USER_ID),
                String.format(":getUserState: User service endpoint is disabled for userId: %d", CURRENT_USER_ID)
            );
            return userStateDto.map(userStateMapper::toUserStateV2);
        }
    }

    public Optional<UserStateV2> getUserStateByAuthenticationToken(Jwt jwt) {

        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        log.debug(":getUserStateByAuthenticationToken: Fetching user state for subject: {}", tokenSubject);

        Optional<UserStateV2Dto> userStateV2Dto = getUserStateFromCache(jwt);
        if (userStateV2Dto.isPresent()) {
            log.debug("User state fetched from cache");
        } else {
            userStateV2Dto = getUserStateFromUserService(jwt);
            log.debug("User state fetched from user service");
        }

        if (userStateV2Dto.isPresent()) {
            UserStateV2 userState = userStateMapper.toUserStateV2(userStateV2Dto.get());
            return Optional.of(userState);
        }
        return Optional.empty();
    }

    private Optional<UserStateV2Dto> getUserStateFromCache(Jwt jwt) {

        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        String cacheKey = USER_STATE_CACHE_PREFIX + tokenSubject;
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
        } catch (JacksonException e) {
            log.warn(":getUserState: could not parse user state from cache: {}", tokenSubject);
            return Optional.empty();
        }
    }

    private Optional<UserStateV2Dto> getUserStateFromUserService(Jwt jwt) {
        String tokenSubject = jwt.getClaim(JWTClaimNames.SUBJECT);
        try {
            UserStateV2Dto userStateV2Dto = userClient.getUserStateByIdWithAuthToken(
                CURRENT_USER_ID,
                "Bearer " + jwt.getTokenValue()
            );
            if (userStateV2Dto == null) {
                log.warn(":getUserStateFromUserService: Null response for subject: {}", tokenSubject);
            }
            return Optional.ofNullable(userStateV2Dto);
        } catch (FeignException e) {
            return handleUserServiceLookupException(
                e,
                String.format(":getUserStateFromUserService: User not found in User Service for subject: %s",
                    tokenSubject),
                String.format(":getUserStateFromUserService: User service endpoint is disabled for subject: %s",
                    tokenSubject)
            );
        }
    }

    private Optional<UserStateV2Dto> handleUserServiceLookupException(FeignException exception,
                                                                      String notFoundMessage,
                                                                      String featureDisabledMessage) {
        if (isFeatureDisabledProblem(exception)) {
            log.warn(featureDisabledMessage);
            throw new DownstreamServiceUnavailableException(USER_SERVICE_ENDPOINT_DISABLED_DETAIL, exception);
        }

        if (exception instanceof FeignException.NotFound) {
            log.warn(notFoundMessage);
            return Optional.empty();
        }

        throw exception;
    }

    private boolean isFeatureDisabledProblem(FeignException exception) {
        if (exception.status() != 404 && exception.status() != 405) {
            return false;
        }

        String responseBody = exception.contentUTF8();
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        try {
            JsonNode problemJson = objectMapper.readTree(responseBody);
            return FEATURE_DISABLED_PROBLEM_TYPE.equals(problemJson.path("type").asText())
                || FEATURE_DISABLED_PROBLEM_TITLE.equals(problemJson.path("title").asText());
        } catch (JacksonException parsingFailure) {
            log.debug(":isFeatureDisabledProblem: could not parse downstream problem detail", parsingFailure);
            return false;
        }
    }
}
