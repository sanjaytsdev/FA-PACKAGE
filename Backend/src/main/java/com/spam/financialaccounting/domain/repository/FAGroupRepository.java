package com.spam.financialaccounting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.spam.financialaccounting.domain.entity.FAGroup;

public interface FAGroupRepository {
    FAGroup save(FAGroup faGroup); // Save a new FAGroup

    Optional<FAGroup> findByCode(String aCode); // Find FAGroup by its code

    List<FAGroup> findAll(); // Find all FAGroup records

    FAGroup update(FAGroup faGroup); // Update existing FAGroup

    boolean delete(String aCode); // Delete FAGroup by its code

    boolean existsByCode(String code); // Needed for "already exists" check
}
