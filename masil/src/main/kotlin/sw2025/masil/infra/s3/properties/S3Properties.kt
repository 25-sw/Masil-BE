package sw2025.masil.infra.s3.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloud.aws.s3")
data class S3Properties(
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucket: String

)