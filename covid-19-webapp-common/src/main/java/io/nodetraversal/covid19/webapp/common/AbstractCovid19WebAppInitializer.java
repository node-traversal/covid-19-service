package io.nodetraversal.covid19.webapp.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Configuration
public abstract class AbstractCovid19WebAppInitializer implements WebApplicationInitializer {
    @Bean
    public SpringBus cxf() {
        return new SpringBus();
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
        factory.setProviders(Arrays.asList(new JacksonJsonProvider(context.getBean(ObjectMapper.class))));
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
}