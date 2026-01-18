package br.com.falbot.seplag.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.BeforeAll;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("app");
    protected static final String MINIO_ACCESS_KEY = "minio";
    protected static final String MINIO_SECRET_KEY = "minio12345";
    protected static final String MINIO_BUCKET = "app-bucket";
    protected static final String MINIO_REGION = "us-east-1";

    @Container
    protected static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio"))
            .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
            .withCommand("server", "/data", "--console-address", ":9001")
            .withExposedPorts(9000, 9001);

    protected static String minioEndpoint() {
        return "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // garante Flyway no mesmo banco do container
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        
        //S3/MinIO
        registry.add("app.s3.endpoint", IntegrationTestBase::minioEndpoint);
        registry.add("app.s3.region", () -> MINIO_REGION);
        registry.add("app.s3.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("app.s3.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("app.s3.bucket", () -> MINIO_BUCKET);
    }

    @BeforeAll
    static void criarBucketMinio() {
        var s3 = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(minioEndpoint()))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("minio", "minio12345")
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        try {
            s3.createBucket(CreateBucketRequest.builder().bucket("app-bucket").build());
        } catch (Exception ignored) {
            //Se já existir, ok
        } finally {
            s3.close();
        }
    }

}
