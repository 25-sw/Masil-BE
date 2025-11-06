package sw2025.masil.infra.s3.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import sw2025.masil.infra.s3.properties.S3Properties

@Configuration
@EnableConfigurationProperties(S3Properties::class)
class S3Config(
    private val s3Properties: S3Properties
) {
    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            s3Properties.accessKey,
            s3Properties.secretKey
        )
        return S3Client.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}