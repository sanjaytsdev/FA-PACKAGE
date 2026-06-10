package com.spam.financialaccounting.application.usecases.fagroup;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FAGroupWithSubGroupsDTO;
import com.spam.financialaccounting.presentation.dto.FASubGroupDTO;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;

@Service
public class GetFAGroupWithSubGroups {

    private final FAGroupRepository groupRepository;
    private final FASubGroupRepository subGroupRepository;

    public GetFAGroupWithSubGroups(FAGroupRepository groupRepository, FASubGroupRepository subGroupRepository) {
        this.groupRepository = groupRepository;
        this.subGroupRepository = subGroupRepository;
    }

    public FAGroupWithSubGroupsDTO execute(String code) {
        FAGroup group = groupRepository.findByCode(code).orElseThrow(()->new FAGroupNotFoundException("No account group found with code '"+code+"'."));
        List<FASubGroup> subGroupEntities=subGroupRepository.findByACode(code);
        List<FASubGroupDTO> subGroupDTOs=subGroupEntities.stream().map(FASubGroupDTOMapper::toDTO).collect(Collectors.toList());
        return new FAGroupWithSubGroupsDTO(group.getAccountCode(), group.getAccountDescription(), group.getAccountType(), group.getAccountCurrentBalance(), subGroupDTOs);
    }

    
    
}
