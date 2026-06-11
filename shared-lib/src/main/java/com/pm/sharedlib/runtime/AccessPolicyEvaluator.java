package com.pm.sharedlib.runtime;

import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;

public class AccessPolicyEvaluator {

    private static final String WHITE = "WHITE";
    private static final String BLACK = "BLACK";
    private static final String IP = "IP";
    private static final String CIDR = "CIDR";
    private static final String CLIENT_ID = "CLIENT_ID";

    public AccessPolicyDecision evaluate(List<AccessPolicyRuntimeConfig> policies, String sourceIp, String clientIdHeader) {
        var activePolicies = policies == null ? List.<AccessPolicyRuntimeConfig>of() : policies.stream()
                .filter(this::isActive)
                .toList();

        if (matchesType(activePolicies, BLACK, sourceIp, clientIdHeader)) {
            return AccessPolicyDecision.DENY;
        }
        if (matchesType(activePolicies, WHITE, sourceIp, clientIdHeader)) {
            return AccessPolicyDecision.ALLOW_TRUSTED;
        }
        return AccessPolicyDecision.REQUIRE_AUTH;
    }

    private boolean matchesType(List<AccessPolicyRuntimeConfig> policies, String type, String sourceIp, String clientIdHeader) {
        return policies.stream()
                .filter(policy -> type.equalsIgnoreCase(policy.getType()))
                .anyMatch(policy -> matches(policy, sourceIp, clientIdHeader));
    }

    private boolean matches(AccessPolicyRuntimeConfig policy, String sourceIp, String clientIdHeader) {
        if (IP.equalsIgnoreCase(policy.getMatchType())) {
            return StringUtils.hasText(sourceIp) && sourceIp.equals(policy.getMatchValue());
        }
        if (CIDR.equalsIgnoreCase(policy.getMatchType())) {
            return StringUtils.hasText(sourceIp) && matchesCidr(sourceIp, policy.getMatchValue());
        }
        if (CLIENT_ID.equalsIgnoreCase(policy.getMatchType())) {
            return StringUtils.hasText(clientIdHeader) && clientIdHeader.equalsIgnoreCase(policy.getMatchValue());
        }
        return false;
    }

    private boolean isActive(AccessPolicyRuntimeConfig policy) {
        if (policy == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(policy.getTemporary())) {
            return true;
        }
        if (policy.getExpiresAt() == null) {
            return false;
        }
        return policy.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private boolean matchesCidr(String sourceIp, String cidr) {
        if (!StringUtils.hasText(cidr)) {
            return false;
        }
        var parts = cidr.split("/", -1);
        if (parts.length != 2) {
            return false;
        }

        try {
            var sourceAddress = InetAddress.getByName(sourceIp);
            var networkAddress = InetAddress.getByName(parts[0]);
            var sourceBytes = sourceAddress.getAddress();
            var networkBytes = networkAddress.getAddress();
            if (sourceBytes.length != networkBytes.length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            int maxPrefixLength = sourceBytes.length * 8;
            if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                return false;
            }

            return matchesPrefix(sourceBytes, networkBytes, prefixLength);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesPrefix(byte[] sourceBytes, byte[] networkBytes, int prefixLength) {
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (sourceBytes[i] != networkBytes[i]) {
                return false;
            }
        }

        if (remainingBits == 0) {
            return true;
        }

        int mask = 0xFF << (8 - remainingBits);
        return (sourceBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }
}
