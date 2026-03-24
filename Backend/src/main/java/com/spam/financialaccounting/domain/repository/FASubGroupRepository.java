package com.spam.financialaccounting.domain.repository;

import java.util.List;

import com.spam.financialaccounting.domain.entity.FASubGroup;

public interface FASubGroupRepository {
    void save(FASubGroup subGroup);

    FASubGroup findByCode(String sCode);

    List<FASubGroup> findAll();

    void update(FASubGroup subGroup);

    void delete(String sCode);
}
