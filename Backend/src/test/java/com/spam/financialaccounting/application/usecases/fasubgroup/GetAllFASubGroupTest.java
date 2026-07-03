package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;

@ExtendWith(MockitoExtension.class)
public class GetAllFASubGroupTest {

    @Mock
    private FASubGroupRepository repository;

    @InjectMocks
    private GetAllFASubGroup getAllFASubGroup;

    @Test
    @DisplayName("Should return list of all FASubGroups when records exist")
    void shouldReturnAllSubGroups_WhenRecordsExist() {
        // ARRANGE
        FASubGroup sub1 = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T");
        FASubGroup sub2 = new FASubGroup("10002", "Bank", "01", "00", BigDecimal.ZERO, "DR", "T");
        when(repository.findAll()).thenReturn(Arrays.asList(sub1, sub2));

        // ACT
        List<FASubGroup> result = getAllFASubGroup.execute();

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FASubGroup::getSCode).containsExactly("10001", "10002");
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no records exist")
    void shouldReturnEmptyList_WhenNoRecordsExist() {
        // ARRANGE
        when(repository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        List<FASubGroup> result = getAllFASubGroup.execute();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(repository, times(1)).findAll();
    }
}
