package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class JsonDataSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime date, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // Format ISO-8601 : parsable nativement par `new Date(...)` cote frontend
        // (l'ancien format "dd-MM-yyyy" produisait un "Invalid Date" en JavaScript).
        gen.writeString(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

}
