package com.spam.financialaccounting.domain.repository;

import com.spam.financialaccounting.domain.entity.FAGroup;

public interface FAGroupRepository {
    void save(FAGroup faGroup);  // Save a new FAGroup
}
