package io.nodetraversal.json;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingEnumDeserializer extends BeanDeserializerModifier {
    private final Logger logger;

    LoggingEnumDeserializer() {
        this(LoggerFactory.getLogger("UNKNOWN_ENUM_CONSTANT"));
    }

    LoggingEnumDeserializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    @SuppressWarnings("unchecked")
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
                    logger.warn("{}.{}", rawClass.getName(), valueAsString);
                }

                return anEnum;
            }
        };
    }
}
