package com.codeinsight.web.audit;

import com.codeinsight.model.entity.AuditLog;
import com.codeinsight.model.repository.AuditLogRepository;
import com.codeinsight.security.jwt.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
                return;
            }

            HttpServletRequest request = getRequest();

            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .action(auditable.action())
                    .resourceType(auditable.resourceType())
                    .resourceId(extractResourceId(joinPoint))
                    .ipAddress(request != null ? request.getRemoteAddr() : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String extractResourceId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof String str && str.length() == 36 && str.contains("-")) {
                return str;
            }
        }
        return null;
    }
}
