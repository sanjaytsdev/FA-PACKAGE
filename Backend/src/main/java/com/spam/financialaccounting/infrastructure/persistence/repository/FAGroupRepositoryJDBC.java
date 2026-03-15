package com.spam.financialaccounting.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.infrastructure.config.SQLAccess;

@Repository
public class FAGroupRepositoryJDBC implements FAGroupRepository{
    private final SQLAccess sqlAccess;

   public FAGroupRepositoryJDBC(SQLAccess sqlAccess) {
        this.sqlAccess = sqlAccess;
    }

    @Override
    public void save(FAGroup faGroup) {
        String sql = "INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?, ?, ?, ?)";
        sqlAccess.executeUpdate(sql,
                faGroup.getAccountCode(),
                faGroup.getAccountDescription(),
                faGroup.getAccountType(),
                faGroup.getAccountCurrentBalance());
    }
}
