package com.kaarelkaasla.klaustestassignment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;

/**
 * Configuration class for setting up custom serialization for Protobuf messages with Jackson.
 */
@Configuration
public class ProtobufConfig {

    /**
     * Configures the Jackson {@link ObjectMapper} to use a custom serializer for Protobuf messages.
     *
     * @param builder
     *            the Jackson2ObjectMapperBuilder used to build the ObjectMapper
     *
     * @return the configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Message.class, new ProtobufJsonSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Custom serializer for Protobuf messages to convert them to JSON format.
     */
    public static class ProtobufJsonSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Message> {

        /**
         * Serializes a Protobuf message to its JSON representation.
         *
         * @param value
         *            the Protobuf message to serialize
         * @param gen
         *            the JsonGenerator used to write the JSON
         * @param serializers
         *            the SerializerProvider
         *
         * @throws IOException
         *             if an I/O error occurs during serialization
         */
        @Override
        public void serialize(Message value, com.fasterxml.jackson.core.JsonGenerator gen,
                com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
            String json = JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames().print(value);
            gen.writeRawValue(json);
        }
    }
}
