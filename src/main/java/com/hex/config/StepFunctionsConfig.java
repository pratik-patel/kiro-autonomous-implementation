package com.hex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;

/**
 * Configuration for AWS Step Functions client.
 * State machine ARN is externalized via application.yml.
 */
@Configuration
public class StepFunctionsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.stepfunctions.state-machine-arn}")
    private String stateMachineArn;

    @Bean
    public SfnClient sfnClient() {
        return SfnClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    public String getStateMachineArn() {
        return stateMachineArn;
    }
}
