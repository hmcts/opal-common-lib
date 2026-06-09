package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimNames;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.opal.common.exception.DownstreamServiceUnavailableException;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService.CURRENT_USER_ID;
import static uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService.USER_STATE_CACHE_PREFIX;
import static uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService.USER_SERVICE_ENDPOINT_DISABLED_DETAIL;

@ExtendWith(MockitoExtension.class)
class UserStateClientServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserStateMapper userStateMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserStateClientService userStateClientService;

    @Mock
    private Jwt jwt;

    @Test
    void getUserStateByAuthenticationToken_returnStateFromServiceWhenCacheMiss() {
        // Arrange
        String subject = "subject-123";
        String tokenValue = "token-abc";
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();
        UserStateV2 mappedUserStateV2 = createMappedUserStateV2();

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject)).thenReturn(null);
        when(userClient.getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue))
            .thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        assertEquals("HMCTS User", userState.get().getUsername());
        assertEquals("Pedro Display Name", userState.get().getName());
        assertEquals(UserStatus.ACTIVE, userState.get().getStatus());
        assertEquals(4L, userState.get().getVersion());
        assertEquals("user-state-cache", userState.get().getCacheName());
    }

    @Test
    void getUserStateByAuthenticationToken_returnsStateFromCacheWhenPresent() throws Exception {
        // Arrange
        String subject = "subject-123";
        String cachedUserState = "{\"cached\":true}";
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();
        UserStateV2 mappedUserStateV2 = createMappedUserStateV2();

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject)).thenReturn(cachedUserState);
        when(objectMapper.readValue(cachedUserState, UserStateV2Dto.class)).thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        assertEquals("HMCTS User", userState.get().getUsername());
        verifyNoInteractions(userClient);
    }

    @Test
    void getUserStateByAuthenticationToken_returnsEmptyWhenCacheMissAndServiceReturnsNotFound() {
        // Arrange
        String subject = "subject-123";
        String tokenValue = "token-abc";
        Request request = Mockito.mock(Request.class);

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject)).thenReturn(null);
        when(userClient.getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue))
            .thenThrow(new FeignException.NotFound("not found", request, null, null));

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isEmpty());
    }

    @Test
    void getUserStateByAuthenticationToken_fallsBackToServiceWhenCacheAccessFails() {
        // Arrange
        String subject = "subject-123";
        String tokenValue = "token-abc";
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();
        UserStateV2 mappedUserStateV2 = createMappedUserStateV2();

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject))
            .thenThrow(new DataAccessException("redis unavailable") {
            });
        when(userClient.getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue))
            .thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        verify(userClient).getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue);
    }

    @Test
    void getUserStateByAuthenticationToken_fallsBackToServiceWhenCachedStateCannotBeParsed() throws Exception {
        // Arrange
        String subject = "subject-123";
        String tokenValue = "token-abc";
        String cachedUserState = "{invalid-json}";
        UserStateV2Dto userStateV2Dto = createUserStateV2Dto();
        UserStateV2 mappedUserStateV2 = createMappedUserStateV2();

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject)).thenReturn(cachedUserState);
        when(objectMapper.readValue(cachedUserState, UserStateV2Dto.class))
            .thenThrow(new JacksonException("invalid json") {
            });
        when(userClient.getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue))
            .thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        verify(userClient).getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue);
    }

    @Test
    void getUserStateByAuthenticationToken_throwsDownstreamServiceUnavailableWhenUserServiceEndpointDisabled()
        throws Exception {
        String subject = "subject-123";
        String tokenValue = "token-abc";

        when(jwt.getClaim(JWTClaimNames.SUBJECT)).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_STATE_CACHE_PREFIX + subject)).thenReturn(null);
        when(objectMapper.readTree(anyString())).thenReturn(featureDisabledProblemJson());
        when(userClient.getUserStateByIdWithAuthToken(CURRENT_USER_ID, "Bearer " + tokenValue))
            .thenThrow(buildFeatureDisabledException(404));

        DownstreamServiceUnavailableException exception = assertThrows(
            DownstreamServiceUnavailableException.class,
            () -> userStateClientService.getUserStateByAuthenticationToken(jwt)
        );

        assertEquals(USER_SERVICE_ENDPOINT_DISABLED_DETAIL, exception.getMessage());
    }

    @Test
    void getUserStateByAuthenticatedUser_throwsDownstreamServiceUnavailableWhenUserServiceEndpointDisabled()
        throws Exception {
        when(objectMapper.readTree(anyString())).thenReturn(featureDisabledProblemJson());
        when(userClient.getUserState(CURRENT_USER_ID)).thenThrow(buildFeatureDisabledException(404));

        DownstreamServiceUnavailableException exception = assertThrows(
            DownstreamServiceUnavailableException.class,
            () -> userStateClientService.getUserStateByAuthenticatedUser()
        );

        assertEquals(USER_SERVICE_ENDPOINT_DISABLED_DETAIL, exception.getMessage());
    }

    private static JsonNode featureDisabledProblemJson() {
        return new ObjectMapper().createObjectNode()
            .put("type", "https://hmcts.gov.uk/problems/feature-disabled")
            .put("title", "Feature Disabled")
            .put("status", 404)
            .put("detail", "The requested feature is not currently available");
    }

    private static FeignException buildFeatureDisabledException(int status) {
        Map<String, Collection<String>> headers = Collections.emptyMap();
        Request request = Request.create(
            Request.HttpMethod.GET,
            "/v2/users/0/state",
            headers,
            Request.Body.empty(),
            new RequestTemplate()
        );

        Response response = Response.builder()
            .request(request)
            .status(status)
            .reason("Feature Disabled")
            .headers(headers)
            .body(
                """
                {
                  "type":"https://hmcts.gov.uk/problems/feature-disabled",
                  "title":"Feature Disabled",
                  "status":404,
                  "detail":"The requested feature is not currently available"
                }
                """,
                java.nio.charset.StandardCharsets.UTF_8
            )
            .build();

        return FeignException.errorStatus("GET /v2/users/0/state", response);
    }


    private UserStateV2Dto createUserStateV2Dto() {
        return UserStateV2Dto.builder()
            .userId(777L)
            .username("HMCTS User")
            .name("Pedro Display Name")
            .status("ACTIVE")
            .version(4L)
            .cacheName("user-state-cache")
            .build();
    }

    private UserStateV2 createMappedUserStateV2() {
        return UserStateV2.builder()
            .userId(777L)
            .username("HMCTS User")
            .name("Pedro Display Name")
            .status(UserStatus.ACTIVE)
            .version(4L)
            .cacheName("user-state-cache")
            .domains(Map.of())
            .build();
    }
}
