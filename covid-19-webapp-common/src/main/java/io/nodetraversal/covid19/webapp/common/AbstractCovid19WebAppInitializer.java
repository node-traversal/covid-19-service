package io.nodetraversal.covid19.webapp.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.Path;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;

@Slf4j
@Configuration
public abstract class AbstractCovid19WebAppInitializer implements WebApplicationInitializer {

    protected abstract List<CacheSettings> createCaches();

    @Bean
    public SpringBus cxf() {
        return new SpringBus();
    }

    @Bean
    public CacheManager cacheManager() {
        log.info("Creating cache manager");
        EhcacheCachingProvider provider = (EhcacheCachingProvider) javax.cache.Caching.getCachingProvider();

        provider.close();

        Map<String, CacheConfiguration<?, ?>> cacheMap = new HashMap<>();
        for (CacheSettings settings : createCaches()) {
            cacheMap.put(settings.name, createCache(settings.name, settings.ttl, settings.sizeInMB));
        }

        javax.cache.CacheManager jcsManager = provider.getCacheManager(
            provider.getDefaultURI(),
            new DefaultConfiguration(cacheMap, provider.getDefaultClassLoader())
        );

        log.info("Created cache manager");

        return new JCacheCacheManager(jcsManager);
    }

    private CacheConfiguration<?, ?> createCache(String cacheName, Duration ttl, long sizeInMB) {
        log.info("Cache: {}, ttl: {}, size: {} MB",
                cacheName, ttl, sizeInMB);

        ResourcePools resourcePools = newResourcePoolsBuilder().heap(sizeInMB, MemoryUnit.MB).build();

        return newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                resourcePools
            )
            .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(ttl))
            .build();
    }

    @Bean
    public Server jaxRsServer(ApplicationContext context) {
        List<ResourceProvider> resourceProviders = new LinkedList<>();
        for (String beanName : context.getBeanDefinitionNames()) {
            if (context.findAnnotationOnBean(beanName, Path.class) != null) {
                log.info("Adding service: {}", beanName);
                SpringResourceFactory factory = new SpringResourceFactory(beanName);
                factory.setApplicationContext(context);
                resourceProviders.add(factory);
            }
        }

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(cxf());
        factory.setProviders(singletonList(new JacksonJsonProvider(context.getBean(ObjectMapper.class))));
        factory.setResourceProviders(resourceProviders);
        return factory.create();
    }

    @Override
    public void onStartup(ServletContext container) {
        log.info("Starting: " + getClass().getCanonicalName());

        container.setInitParameter("contextConfigLocation", "classpath:webapp-context.xml");
        container.addListener(ContextLoaderListener.class);

        ServletRegistration.Dynamic apiServlet = container.addServlet("CXFServlet", new CXFServlet());
        apiServlet.setLoadOnStartup(1);
        apiServlet.addMapping("/*");
    }

    @RequiredArgsConstructor
    protected static final class CacheSettings {
        private final String name;
        private final Duration ttl;
        private final int sizeInMB;
    }
}