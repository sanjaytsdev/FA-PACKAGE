package com.spam.financialaccounting.application.usecases.fagroup;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;

@ExtendWith(MockitoExtension.class)
public class GetAllFAGroupTest {

    @Mock
    private FAGroupRepository repository;

    @InjectMocks
    private GetAllFAGroup getAllFAGroup;

    @Test
    @DisplayName("Should return all FAGroup from repository")
    void shouldReturnAllFAGroups() {

        List<FAGroup> fakeGroups = Arrays.asList(
                new FAGroup("01", "Asset", "0", BigDecimal.ZERO),
                new FAGroup("02", "Liability", "1", BigDecimal.ZERO));

        when(repository.findAll()).thenReturn(fakeGroups);
        List<FAGroup> result = getAllFAGroup.execute();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountCode()).isEqualTo("01");
        assertThat(result.get(1).getAccountCode()).isEqualTo("02");

        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no FAGroups exist")
    void shouldReturnEmptyList_WhenNoneExist() {
        // ARRANGE — mock returns empty list
        when(repository.findAll()).thenReturn(Collections.emptyList());
        // ACT
        List<FAGroup> result = getAllFAGroup.execute();
        // ASSERT
        assertThat(result).isEmpty();
    }

}
