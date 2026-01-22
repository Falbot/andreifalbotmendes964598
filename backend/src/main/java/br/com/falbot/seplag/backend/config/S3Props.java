package br.com.falbot.seplag.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3Props {
  @Value("${app.s3.endpoint}") public String endpoint;
  @Value("${app.s3.region}") public String region;
  @Value("${app.s3.access-key}") public String accessKey;
  @Value("${app.s3.secret-key}") public String secretKey;
  @Value("${app.s3.bucket}") public String bucket;
  @Value("${app.s3.public-endpoint:${app.s3.endpoint}}") public String publicEndpoint;
}
