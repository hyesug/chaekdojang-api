package com.chaekdojang.api.domain.admin.audit;

import com.chaekdojang.api.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, String action, String targetType, Long targetId, String summary) {
        adminAuditLogRepository.save(AdminAuditLog.create(actor, action, targetType, targetId, summary));
    }
}
