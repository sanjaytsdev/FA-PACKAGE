package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
public class CreateFASubGroupTest {

    @Mock
    private FASubGroupRepository subGroupRepository;

    @Mock
    private FAGroupRepository groupRepository;

    @InjectMocks
    private CreateFASubGroup createFASubGroup;

    private FAGroup parentGroup;
    private FASubGroup validSubGroup;

    @BeforeEach
    void setUp() {
        parentGroup = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        validSubGroup = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
    }

    @Test
    @DisplayName("Should successfully create FASubGroup when input is valid")
    void shouldCreateFASubGroup_WhenValidInput() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(validSubGroup)).thenReturn(validSubGroup);

        // ACT
        FASubGroup result = createFASubGroup.execute(validSubGroup);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getSCode()).isEqualTo("10001");
        assertThat(result.getSDesc()).isEqualTo("Cash");
        assertThat(result.getACode()).isEqualTo("01");
        verify(subGroupRepository, times(1)).save(validSubGroup);
    }

    @Test
    @DisplayName("Should throw FAGroupNotFoundException when parent group does not exist")
    void shouldThrow_WhenParentGroupNotFound() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> createFASubGroup.execute(validSubGroup))
                .isInstanceOf(FAGroupNotFoundException.class)
                .hasMessageContaining("Parent Group with code 01 does not exist.");

        verify(subGroupRepository, never()).existsByCode(any());
        verify(subGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set default balance to ZERO when balance is null")
    void shouldDefaultBalance_WhenBalanceIsNull() {
        // ARRANGE
        FASubGroup subGroupWithNullBalance = new FASubGroup("10001", "Cash", "01", "00", null, "DR", "T");
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroupWithNullBalance)).thenReturn(subGroupWithNullBalance);

        // ACT
        createFASubGroup.execute(subGroupWithNullBalance);

        // ASSERT
        assertThat(subGroupWithNullBalance.getSOpbal()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should set default flag to 'T' when flag is null")
    void shouldDefaultFlag_WhenFlagIsNull() {
        // ARRANGE
        FASubGroup subGroupWithNullFlag = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", null);
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroupWithNullFlag)).thenReturn(subGroupWithNullFlag);

        // ACT
        createFASubGroup.execute(subGroupWithNullFlag);

        // ASSERT
        assertThat(subGroupWithNullFlag.getSFlag()).isEqualTo("T");
    }

    @Test
    @DisplayName("Should throw FASubGroupAlreadyExistsException when ledger code already exists")
    void shouldThrow_WhenCodeAlreadyExists() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> createFASubGroup.execute(validSubGroup))
                .isInstanceOf(FASubGroupAlreadyExistsException.class)
                .hasMessageContaining("Ledger Account already exist with code: 10001");

        verify(subGroupRepository, never()).save(any());
    }
}
