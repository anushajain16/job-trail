package com.example.anusha.job_trail.jobposting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * One {@link RestClient} for the ml-service, wired with a hard connect/read
 * timeout so a stalled scrape or LLM call on the other end can never hang a
 * Spring request thread indefinitely — {@link MlServiceParseClient} relies
 * on that to fail predictably and fall back to manual entry.
 */
@Configuration
public class MlServiceClientConfig {

    @Bean
    RestClient mlServiceRestClient(MlServiceProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory);

        // Only sent when configured: local dev typically runs ml-service
        // with MLSVC_INTERNAL_API_KEY unset, so both sides agree on "no
        // check" rather than every dev needing to mint a shared value.
        if (StringUtils.hasText(properties.sharedSecret())) {
            builder.defaultHeader("X-Internal-Api-Key", properties.sharedSecret());
        }

        return builder.build();
    }
}
