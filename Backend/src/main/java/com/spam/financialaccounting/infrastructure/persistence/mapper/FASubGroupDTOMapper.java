package com.spam.financialaccounting.infrastructure.persistence.mapper;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.presentation.dto.FASubGroupDTO;

public class FASubGroupDTOMapper {

    public static FASubGroup toEntity(FASubGroupDTO dto) {
        return new FASubGroup(
                dto.getSCode(),
                dto.getSDesc(),
                dto.getACode(),
                dto.getSType(),
                dto.getSOpbal(),
                dto.getSDrCr(),
                dto.getSFlag());
    }

    public static FASubGroupDTO toDTO(FASubGroup entity) {
        FASubGroupDTO dto=new FASubGroupDTO();
        dto.setSCode(entity.getSCode());
        dto.setSDesc(entity.getSDesc());
        dto.setACode(entity.getACode());
        dto.setSType(entity.getSType());
        dto.setSOpbal(entity.getSOpbal());
        dto.setSDrCr(entity.getSDrCr());
        dto.setSFlag(entity.getSFlag());
        return dto;
    }
}
