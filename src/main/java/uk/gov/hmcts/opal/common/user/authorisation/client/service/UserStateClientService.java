package uk.gov.hmcts.opal.common.user.authorisation.client.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.opal.common.user.authorisation.client.UserClient;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.UserStateMapper;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
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

    private Optional<UserState> fetchUserState(Long userId) {

    @Cacheable(value = "userState",
        key = "T(org.springframework.security.core.context.SecurityContextHolder)"
            + ".getContext()?.getAuthentication()?.getName() ?: 'anonymous'")
    public Optional<UserState> getUserStateByAuthenticatedUser() {
        return fetchUserState(AUTHENTICATED_USER_SPECIAL_CODE);
    }

    private Optional<UserState> fetchUserState(Long userId) {
        log.info(":getUserState: Fetching user state for specific userId: {}", userId);

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
}
