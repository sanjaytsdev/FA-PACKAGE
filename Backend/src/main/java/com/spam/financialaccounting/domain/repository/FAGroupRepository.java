package com.spam.financialaccounting.domain.repository;

import java.util.List;

import com.spam.financialaccounting.domain.entity.FAGroup;

public interface FAGroupRepository {
    void save(FAGroup faGroup);  // Save a new FAGroup
    FAGroup findByCode(String aCode);  // Find FAGroup by its code
    List<FAGroup> findAll();  // Find all FAGroup records
    void update(FAGroup faGroup);  // Update existing FAGroup
    void delete(String aCode);  // Delete FAGroup by its code
}
