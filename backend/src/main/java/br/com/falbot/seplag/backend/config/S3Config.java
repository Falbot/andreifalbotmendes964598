package br.com.falbot.seplag.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client(S3Props props) {
    var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey, props.secretKey));
    var s3cfg = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    return S3Client.builder()
        .credentialsProvider(creds)
        .serviceConfiguration(s3cfg)
        .endpointOverride(URI.create(props.endpoint))
        .region(Region.of(props.region))
        .build();
  }

  @Bean
  public S3Presigner s3Presigner(S3Props props) {
    var creds = AwsBasicCredentials.create(props.accessKey, props.secretKey);
    return S3Presigner.builder()
        .credentialsProvider(StaticCredentialsProvider.create(creds))
        .endpointOverride(URI.create(props.publicEndpoint))
        .region(Region.of(props.region))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }
}
