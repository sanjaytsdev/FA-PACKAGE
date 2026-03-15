package com.spam.financialaccounting.infrastructure.persistence.mapper;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;

public class FAGroupDTOMapper {
    public static FAGroup toEntity(FAGroupDTO dto) {
        return new FAGroup(
            dto.getAccountCode(),
            dto.getAccountDescription(),
            dto.getAccountType(),
            dto.getAccountCurrentBalance()
        );
    }

    public static FAGroupDTO toDTO(FAGroup entity) {
        return new FAGroupDTO(
                entity.getAccountCode(),
                entity.getAccountDescription(),
                entity.getAccountType(),
                entity.getAccountCurrentBalance()
        );
    }
}
