package com.github.davidkuster.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Log config on startup, except for those that contain any of the string values specified in the excluded list.
 */
@Component
@Profile("!test")
@Slf4j
public class PropertyLogger {

    private static final Set<String> EXCLUDED_PROPERTIES = Stream
        .of(
            "apikey",
            "credentials",
            "passcode",
            "password",
            "private",
            "secret",
            "token")
        .map(String::toLowerCase) // guard against future non-lowercase fat fingers
        .collect(Collectors.toSet());


    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext().getEnvironment();
        log.info("====== Environment and configuration ======");
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
            .filter(ps -> ps instanceof EnumerablePropertySource)
            .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
            .flatMap(Arrays::stream)
            .distinct()
            .sorted()
            .forEach(prop -> {
                try {
                    String value = env.getProperty(prop);
                    boolean isSensitive = EXCLUDED_PROPERTIES.stream().anyMatch(prop.toLowerCase()::contains);

                    if (isSensitive && StringUtils.isNotBlank(value)) {
                        log.info("{}: <sensitive value not logged>", prop);
                    } else {
                        log.info("{}: {}", prop, value);
                    }
                } catch (Exception e) {
                    log.error("{}: error reading value: {}", prop, e.getMessage());
                }
            });
        log.info("===========================================");
    }
}