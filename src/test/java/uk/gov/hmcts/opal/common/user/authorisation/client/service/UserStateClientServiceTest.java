package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStateClientServiceTest {

    @Mock
    private UserClient userClient;

    @Spy
    private UserStateMapper userStateMapper = Mappers.getMapper(UserStateMapper.class);

    @InjectMocks
    private UserStateClientService userStateClientService;

    @Test
    void getUserState_returnsUserWhenPresent() {
        UserStateDto dto = UserStateDto.builder()
            .username("HMCTS User")
            .userId(777L)
            .build();
        when(userClient.getUserStateByIdWithAuthToken(any(), any())).thenReturn(dto);

        Optional<UserState> userState = userStateClientService.getUserState(0L);

        assertTrue(userState.isPresent());
        assertEquals("HMCTS User", userState.get().getUserName());
        assertEquals(777L, userState.get().getUserId());
    }

    @Test
    void getUserState_returnsEmptyWhenNotFound() {
        Request request = Mockito.mock(Request.class);
        when(userClient.getUserStateByIdWithAuthToken(any(), any()))
            .thenThrow(new FeignException.NotFound("not found", request, null, null));

        Optional<UserState> userState = userStateClientService.getUserState(0L);

        assertTrue(userState.isEmpty());
    }

    @Test
    void getUserStateByAuthenticatedUser_returnsUserWhenPresent() {
        UserStateDto dto = UserStateDto.builder()
            .username("HMCTS User")
            .userId(777L)
            .build();
        when(userClient.getUserStateByIdWithAuthToken(any(), any())).thenReturn(dto);

        Optional<UserState> userState = userStateClientService.getUserStateByAuthenticatedUser();

        assertTrue(userState.isPresent());
        assertEquals("HMCTS User", userState.get().getUserName());
        assertEquals(777L, userState.get().getUserId());
    }
}
