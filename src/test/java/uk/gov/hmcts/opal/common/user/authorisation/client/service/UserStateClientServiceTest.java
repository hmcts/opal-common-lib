package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimNames;
import feign.FeignException;
import feign.Request;
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
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService.USER_STATE_CACHE_PREFIX;

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
        when(userClient.getUserStateByIdWithAuthToken("Bearer " + tokenValue)).thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        assertEquals("HMCTS User", userState.get().getUsername());
        assertEquals("HMCTS User Name", userState.get().getName());
        assertEquals("ACTIVE", userState.get().getStatus());
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
        when(userClient.getUserStateByIdWithAuthToken("Bearer " + tokenValue))
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
            .thenThrow(new DataAccessException("redis unavailable") { });
        when(userClient.getUserStateByIdWithAuthToken("Bearer " + tokenValue)).thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        verify(userClient).getUserStateByIdWithAuthToken("Bearer " + tokenValue);
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
            .thenThrow(new JsonProcessingException("invalid json") { });
        when(userClient.getUserStateByIdWithAuthToken("Bearer " + tokenValue)).thenReturn(userStateV2Dto);
        when(userStateMapper.toUserStateV2(userStateV2Dto)).thenReturn(mappedUserStateV2);

        // Act
        Optional<UserStateV2> userState = userStateClientService.getUserStateByAuthenticationToken(jwt);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals(777L, userState.get().getUserId());
        verify(userClient).getUserStateByIdWithAuthToken("Bearer " + tokenValue);
    }

    @Test
    void getUserState_returnsUserWhenPresent() {
        // Arrange
        UserStateDto dto = UserStateDto.builder()
            .username("HMCTS User")
            .userId(777L)
            .build();
        UserState mappedUserState = createMappedUserState();
        when(userClient.getUserStateById(any())).thenReturn(dto);
        when(userStateMapper.toUserState(dto)).thenReturn(mappedUserState);

        // Act
        Optional<UserState> userState = userStateClientService.getUserState(0L);

        //Assert
        assertTrue(userState.isPresent());
        assertEquals("HMCTS User", userState.get().getUserName());
        assertEquals(777L, userState.get().getUserId());
    }

    @Test
    void getUserState_returnsEmptyWhenNotFound() {
        // Arrange
        Request request = Mockito.mock(Request.class);
        when(userClient.getUserStateById(any()))
            .thenThrow(new FeignException.NotFound("not found", request, null, null));

        // Act
        Optional<UserState> userState = userStateClientService.getUserState(0L);

        //Assert
        assertTrue(userState.isEmpty());
    }

    @Test
    void getUserStateByAuthenticatedUser_returnsUserWhenPresent() {
        // Arrange
        UserStateDto dto = UserStateDto.builder()
            .username("HMCTS User")
            .userId(777L)
            .build();
        UserState mappedUserState = createMappedUserState();
        when(userClient.getUserStateById(any())).thenReturn(dto);
        when(userStateMapper.toUserState(dto)).thenReturn(mappedUserState);

        // Act
        Optional<UserState> userState = userStateClientService.getUserStateByAuthenticatedUser();

        //Assert
        assertTrue(userState.isPresent());
        assertEquals("HMCTS User", userState.get().getUserName());
        assertEquals(777L, userState.get().getUserId());
    }

    private UserStateV2Dto createUserStateV2Dto() {
        return UserStateV2Dto.builder()
            .userId(777L)
            .username("HMCTS User")
            .name("HMCTS User Name")
            .status("ACTIVE")
            .version(4L)
            .cacheName("user-state-cache")
            .build();
    }

    private UserStateV2 createMappedUserStateV2() {
        return UserStateV2.builder()
            .userId(777L)
            .username("HMCTS User")
            .name("HMCTS User Name")
            .status("ACTIVE")
            .version(4L)
            .cacheName("user-state-cache")
            .domains(Map.of())
            .build();
    }

    private UserState createMappedUserState() {
        return UserState.builder()
            .userId(777L)
            .userName("HMCTS User")
            .businessUnitUser(Set.of())
            .build();
    }
}
