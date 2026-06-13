package com.seal.seal_backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DotEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx <= 0) continue;

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                // Strip inline comments (e.g. value  # comment)
                int commentIdx = value.indexOf(" #");
                if (commentIdx >= 0) value = value.substring(0, commentIdx).trim();

                props.put(key, value);
            }
        } catch (IOException e) {
            // .env not found — skip (production uses real env vars)
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", props));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
