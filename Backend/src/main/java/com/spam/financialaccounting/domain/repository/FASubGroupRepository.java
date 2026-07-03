package com.spam.financialaccounting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.spam.financialaccounting.domain.entity.FASubGroup;

public interface FASubGroupRepository {
    FASubGroup save(FASubGroup subGroup);

    Optional<FASubGroup> findByCode(String sCode);

    List<FASubGroup> findAll();
    List<FASubGroup> findByACode(String aCode);

    FASubGroup update(FASubGroup subGroup);

    boolean delete(String sCode);
    boolean existsByCode(String sCode);
}
