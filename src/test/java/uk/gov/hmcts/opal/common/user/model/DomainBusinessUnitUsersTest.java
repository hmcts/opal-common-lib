package uk.gov.hmcts.opal.common.user.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainBusinessUnitUsersTest {
    @Test
    void getBusinessUnitUserForBusinessUnit_shouldReturnBusinessUnitUser_whenOneIsPresent() {
        BusinessUnitUserV2 businessUnitUser1 = mock(BusinessUnitUserV2.class);
        when(businessUnitUser1.matchesBusinessUnitId((short) 2)).thenReturn(false);
        BusinessUnitUserV2 businessUnitUser2 = mock(BusinessUnitUserV2.class);
        when(businessUnitUser2.matchesBusinessUnitId((short) 2)).thenReturn(true);

        Optional<BusinessUnitUserV2> result =
            new DomainBusinessUnitUsers(
                List.of(businessUnitUser1, businessUnitUser2)
            ).getBusinessUnitUserForBusinessUnit((short) 2);

        assertTrue(result.isPresent());
        assertEquals(businessUnitUser2, result.get());
        verify(businessUnitUser1).matchesBusinessUnitId((short) 2);
        verify(businessUnitUser2).matchesBusinessUnitId((short) 2);
    }

    @Test
    void getBusinessUnitUserForBusinessUnit_shouldReturnEmpty_whenUserNotPresent() {
        BusinessUnitUserV2 businessUnitUser1 = mock(BusinessUnitUserV2.class);
        when(businessUnitUser1.matchesBusinessUnitId((short) 2)).thenReturn(false);
        BusinessUnitUserV2 businessUnitUser2 = mock(BusinessUnitUserV2.class);
        when(businessUnitUser2.matchesBusinessUnitId((short) 2)).thenReturn(false);

        Optional<BusinessUnitUserV2> result =
            new DomainBusinessUnitUsers(
                List.of(businessUnitUser1, businessUnitUser2)
            ).getBusinessUnitUserForBusinessUnit((short) 3);

        assertTrue(result.isEmpty());
        verify(businessUnitUser1).matchesBusinessUnitId((short) 3);
        verify(businessUnitUser2).matchesBusinessUnitId((short) 3);
    }
}
