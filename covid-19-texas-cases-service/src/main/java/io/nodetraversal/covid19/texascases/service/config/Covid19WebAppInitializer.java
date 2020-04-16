package io.nodetraversal.covid19.texascases.service.config;

import io.nodetraversal.covid19.webapp.common.AbstractCovid19WebAppInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("io.nodetraversal")
public class Covid19WebAppInitializer extends AbstractCovid19WebAppInitializer {
}
