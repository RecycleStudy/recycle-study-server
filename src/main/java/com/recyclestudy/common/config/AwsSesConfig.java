package com.recyclestudy.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class AwsSesConfig {

    @Bean
    public SesV2Client sesV2Client() {
        return SesV2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
