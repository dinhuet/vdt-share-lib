package com.pm.sharedlib.config;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "vdt.share")
public class VdtShareProperties {
    boolean enabled = true;
    String serviceName;
}
