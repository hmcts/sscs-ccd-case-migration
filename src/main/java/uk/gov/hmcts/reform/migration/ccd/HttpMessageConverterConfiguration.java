package uk.gov.hmcts.reform.migration.ccd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class HttpMessageConverterConfiguration {
    @Bean
    public MappingJackson2HttpMessageConverter converter(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    }
}
