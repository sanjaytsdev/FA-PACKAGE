package com.spam.financialaccounting.infrastructure.persistence.mapper;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;

public class JournalMasterDTOMapper {

    public static JournalMasterDTO toDTO(JournalMaster entity) {
        if (entity == null) {
            return null;
        }
        return new JournalMasterDTO(
                entity.getJId(),
                entity.getJDoc(),
                entity.getJDate(),
                entity.getJAmount(),
                entity.getJNarr());
    }

    public static JournalMaster toEntity(JournalMasterDTO dto) {
        if (dto == null) {
            return null;
        }
        return new JournalMaster(
                dto.getJId(),
                dto.getJDoc(),
                dto.getJDate(),
                dto.getJAmount(),
                dto.getJNarr());
    }

}
