package uk.gov.hmcts.reform.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapperConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .addModule(new JavaTimeModule())
            .build();
    }
}
