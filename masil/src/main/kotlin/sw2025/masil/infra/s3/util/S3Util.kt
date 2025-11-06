package sw2025.masil.infra.s3.util

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sw2025.masil.infra.s3.properties.S3Properties
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import sw2025.masil.infra.s3.exception.S3UploadFailedException
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@Service
class S3Util(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties
) {
    fun upload(file: MultipartFile): String {
        val key = "${UUID.randomUUID()}/${file.originalFilename}"

        return try {
            val putReq = PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .contentType(file.contentType)
                .build()

            s3Client.putObject(putReq, RequestBody.fromBytes(file.bytes))
            s3Client.utilities().getUrl { it.bucket(s3Properties.bucket).key(key) }.toExternalForm()
        } catch (e: IOException) {
            throw S3UploadFailedException
        }
    }

    fun uploadStream(fileName: String, inputStream: InputStream, contentType: String): String {
        val key = "${UUID.randomUUID()}/$fileName"

        return try {
            val putReq = PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .contentType(contentType)
                .build()

            s3Client.putObject(putReq, RequestBody.fromInputStream(inputStream, inputStream.available().toLong()))
            s3Client.utilities().getUrl { it.bucket(s3Properties.bucket).key(key) }.toExternalForm()
        } catch (e: IOException) {
            throw S3UploadFailedException
        }
    }

    fun delete(uploadFileName: String) {
        val delReq = DeleteObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(uploadFileName)
            .build()
        s3Client.deleteObject(delReq)
    }
}