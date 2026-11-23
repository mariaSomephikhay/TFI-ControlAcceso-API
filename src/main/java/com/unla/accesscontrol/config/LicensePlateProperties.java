package com.unla.accesscontrol.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "license-plates")
public record LicensePlateProperties(List<String> patterns) {
}
