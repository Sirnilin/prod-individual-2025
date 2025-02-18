package prod.individual.sirnilin.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class S3Service {
    private final AmazonS3 s3Client;

    @Value("${S3_BUCKET}")
    private String bucket;

    public S3Service(
            @Value("${S3_ENDPOINT}") String endpoint,
            @Value("${S3_ACCESS_KEY}") String accessKey,
            @Value("${S3_SECRET_KEY}") String secretKey) {

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    public String uploadFile(MultipartFile file) {
        String key = "images/" + file.getOriginalFilename();

        if (!s3Client.doesBucketExistV2(bucket)) {
            s3Client.createBucket(bucket);
            // Публичная политика
            String publicReadPolicy =
                    "{" +
                            "  \"Version\": \"2012-10-17\"," +
                            "  \"Statement\": [" +
                            "    {" +
                            "      \"Action\": [" +
                            "        \"s3:GetBucketLocation\"," +
                            "        \"s3:ListBucket\"" +
                            "      ]," +
                            "      \"Effect\": \"Allow\"," +
                            "      \"Principal\": \"*\"," +
                            "      \"Resource\": \"arn:aws:s3:::" + bucket + "\"" +
                            "    }," +
                            "    {" +
                            "      \"Action\": \"s3:GetObject\"," +
                            "      \"Effect\": \"Allow\"," +
                            "      \"Principal\": \"*\"," +
                            "      \"Resource\": \"arn:aws:s3:::" + bucket + "/*\"" +
                            "    }" +
                            "  ]" +
                            "}";
            s3Client.setBucketPolicy(bucket, publicReadPolicy);
        }

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(putObjectRequest);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки файла", e);
        }
        return getLocalUrl(s3Client.getUrl(bucket, key).toString());
    }

    private String getLocalUrl(String minioUrl) {
        return minioUrl.replace("http://minio:9000", "http://localhost:9000");
    }
}
