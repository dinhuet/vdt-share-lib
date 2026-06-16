package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.config.VdtShareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@RequiredArgsConstructor
public class SecurityAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final ObjectMapper objectMapper;
    private final VdtShareProperties properties;
    private final SecurityAuditLogPublisher publisher;

    public void log(SecurityLogEvent event) {
        if (event == null || !properties.getAudit().isEnabled()) {
            return;
        }

        writeLocal(event);
        publish(event);
    }

    private void writeLocal(SecurityLogEvent event) {
        if (!properties.getAudit().isLogLocal()) {
            return;
        }
        try {
            AUDIT_LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.warn("security_audit_log_local_serialize_failed endpointId={}", event.getEndpointId(), e);
        } catch (RuntimeException e) {
            log.warn("security_audit_log_local_write_failed endpointId={}", event.getEndpointId(), e);
        }
    }

    private void publish(SecurityLogEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException e) {
            log.warn("security_audit_log_publisher_failed endpointId={}", event.getEndpointId(), e);
        }
    }
}
