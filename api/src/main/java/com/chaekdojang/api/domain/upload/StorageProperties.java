package com.chaekdojang.api.domain.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String type = "local";
    private final Local local = new Local();
    private final S3 s3 = new S3();

    public boolean isS3() {
        return "s3".equalsIgnoreCase(type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    public S3 getS3() {
        return s3;
    }

    public static class Local {
        private String uploadDir = "uploads/profile-images";
        private String publicPath = "/uploads/profile-images";

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public String getPublicPath() {
            return publicPath;
        }

        public void setPublicPath(String publicPath) {
            this.publicPath = publicPath;
        }
    }

    public static class S3 {
        private String bucket = "";
        private String region = "ap-northeast-2";
        private String publicBaseUrl = "";
        private String profileImagePrefix = "profile-images";
        private long presignedUrlExpirationMinutes = 10;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public String getProfileImagePrefix() {
            return profileImagePrefix;
        }

        public void setProfileImagePrefix(String profileImagePrefix) {
            this.profileImagePrefix = profileImagePrefix;
        }

        public long getPresignedUrlExpirationMinutes() {
            return presignedUrlExpirationMinutes;
        }

        public void setPresignedUrlExpirationMinutes(long presignedUrlExpirationMinutes) {
            this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
        }
    }
}
