package com.spam.financialaccounting.application.usecases.fagroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
public class UpdateFAGroupTest {

    @Mock
    private FAGroupRepository repository;

    @InjectMocks
    private UpdateFAGroup updateFAGroup;

    @Test
    @DisplayName("Should successfully update FAGroup when it exists")
    void shouldUpdateFAGroup_WhenExists() {
        FAGroup existingGroup = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        FAGroup updatedGroup = new FAGroup("01", "Updated Asset", "0", new BigDecimal("500.00"));

        when(repository.findByCode("01")).thenReturn(Optional.of(existingGroup));
        when(repository.update(updatedGroup)).thenReturn(updatedGroup);

        FAGroup result = updateFAGroup.execute(updatedGroup);

        assertThat(result).isNotNull();
        assertThat(result.getAccountDescription()).isEqualTo("Updated Asset");
        assertThat(result.getAccountCurrentBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Should throw FAGroupNotFoundException when group to update does not exist")
    void shouldThrow_WhenGroupNotFound() {
        // ARRANGE
        FAGroup group = new FAGroup("99", "Ghost", "0", BigDecimal.ZERO);
        when(repository.findByCode("99")).thenReturn(Optional.empty());
        // ACT + ASSERT
        assertThatThrownBy(() -> updateFAGroup.execute(group))
                .isInstanceOf(FAGroupNotFoundException.class)
                .hasMessageContaining("Account Group with code 99 not found.");
        // VERIFY — update() should never be called if group doesn't exist
        verify(repository, never()).update(any());
    }

    @Test
    @DisplayName("Should call findByCode and update exactly once")
    void shouldCallRepoMethodsExactlyOnce() {
        FAGroup group = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        when(repository.findByCode("01")).thenReturn(Optional.of(group));
        when(repository.update(group)).thenReturn(group);
        updateFAGroup.execute(group);
        verify(repository, times(1)).findByCode("01");
        verify(repository, times(1)).update(group);
    }

}
