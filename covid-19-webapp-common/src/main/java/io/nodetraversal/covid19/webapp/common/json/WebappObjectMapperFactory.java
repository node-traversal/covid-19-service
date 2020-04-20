package io.nodetraversal.covid19.webapp.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class WebappObjectMapperFactory {
    @Bean
    public ObjectMapper objectMapper() {
        log.info("Creating custom jackson json provider");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new LoggingEnumDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    private static class LoggingEnumDeserializer extends BeanDeserializerModifier {
        private final Logger log;

        LoggingEnumDeserializer() {
            this(LoggerFactory.getLogger("UNKNOWN_ENUM_CONSTANT"));
        }

        LoggingEnumDeserializer(Logger log) {
            this.log = log;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"}) // because this is a "plugin", it should not be directly invoked ...
        public JsonDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config,
                                                             JavaType type,
                                                             BeanDescription description,
                                                             JsonDeserializer<?> deserializer) {
            return new JsonDeserializer<>() {
                @Override
                public Enum deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                    Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();

                    Enum anEnum = null;
                    String valueAsString = parser.getValueAsString();

                    try {
                        anEnum = Enum.valueOf(rawClass, StringUtils.upperCase(valueAsString));
                    } catch (Exception e) {
                        log.warn("{}.{}", rawClass.getName(), valueAsString);
                    }

                    return anEnum;
                }
            };
        }
    }
}
