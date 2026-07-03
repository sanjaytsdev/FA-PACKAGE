package com.spam.financialaccounting.infrastructure.persistence.mapper;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.presentation.dto.JournalDetailDTO;

public class JournalDetailDTOMapper {

    public static JournalDetail toEntity(JournalDetailDTO dto) {
        return new JournalDetail(
                dto.getJId(),
                dto.getJCode(),
                dto.getJDrCr(),
                dto.getJAmount());
    }

    public static JournalDetailDTO toDTO(JournalDetail entity) {
        return new JournalDetailDTO(
                entity.getJId(),
                entity.getJCode(),
                entity.getJDrCr(),
                entity.getJAmount());
    }

}
