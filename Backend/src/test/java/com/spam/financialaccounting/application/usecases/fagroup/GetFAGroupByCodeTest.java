package com.spam.financialaccounting.application.usecases.fagroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetFAGroupByCodeTest {

    @Mock
    private FAGroupRepository repository;

    @InjectMocks
    private GetFAGroupByCode getFAGroupByCode;

    @Test
    @DisplayName("Should return FAGroup when valid code is found")
    void shouldReturnFAGroup_WhenCodeExists() {
        FAGroup group = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        when(repository.findByCode("01")).thenReturn(Optional.of(group));

        FAGroup result = getFAGroupByCode.execute("01");

        assertThat(result).isNotNull();
        assertThat(result.getAccountCode()).isEqualTo("01");
        assertThat(result.getAccountDescription()).isEqualTo("Asset");
    }

    @Test
    @DisplayName("Should throw FAGroupNotFoundException when code does not exist")
    void shouldThrow_WhenCodeNotFound() {
        // ARRANGE — Optional.empty() = nothing found
        when(repository.findByCode("99")).thenReturn(Optional.empty());
        // ASSERT — exception thrown with correct message
        assertThatThrownBy(() -> getFAGroupByCode.execute("99"))
                .isInstanceOf(FAGroupNotFoundException.class)
                .hasMessageContaining("FAGroup not found with code: 99");
    }

    @Test
    @DisplayName("Should call findByCode exactly once")
    void shouldCallFindByCodeOnce() {
        FAGroup group = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        when(repository.findByCode("01")).thenReturn(Optional.of(group));
        getFAGroupByCode.execute("01");
        // Verify the repo method was called once with the correct argument
        verify(repository, times(1)).findByCode("01");
    }

}
