package io.nodetraversal.covid19.jhucsse.service.config;

import com.google.common.collect.ImmutableList;
import io.nodetraversal.covid19.webapp.common.AbstractCovid19WebAppInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static java.time.Duration.ofHours;

@Slf4j
@Configuration
@ComponentScan("io.nodetraversal")
@EnableCaching
public class Covid19WebAppInitializer extends AbstractCovid19WebAppInitializer {

    protected List<CacheSettings> createCaches() {
        return ImmutableList.of(
                new CacheSettings(Caches.DEFAULT, ofHours(6), 20),
                new CacheSettings(Caches.SHORT, ofHours(1), 100)
        );
    }
}
