package com.unla.gestionUsuario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

import com.unla.gestionUsuario.config.LicensePlateProperties;

class LicensePlatePropertiesTests {
    @Test
    void readsPatternsFromApplicationProperties() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new ResourcePropertySource("classpath:application.properties"));

        LicensePlateProperties properties = Binder.get(environment)
                .bind("license-plates", Bindable.of(LicensePlateProperties.class))
                .orElseThrow(() -> new AssertionError("No se configuraron patrones de patente."));

        assertEquals(List.of("[A-Z0-9]{1,16}"), properties.patterns());
    }

    @Test
    void bindsMultipleIndexedPatterns() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("licensePlatePatterns", Map.of(
                "license-plates.patterns[0]", "[A-Z]{2}[0-9]{3}",
                "license-plates.patterns[1]", "[0-9]{3}[A-Z]{2}")));

        LicensePlateProperties properties = Binder.get(environment)
                .bind("license-plates", Bindable.of(LicensePlateProperties.class))
                .orElseThrow(() -> new AssertionError("No se configuraron patrones de patente."));

        assertEquals(List.of("[A-Z]{2}[0-9]{3}", "[0-9]{3}[A-Z]{2}"), properties.patterns());
    }
}
