package com.befapress.service;

import com.befapress.entity.CloudinaryConfig;
import com.befapress.repository.CloudinaryConfigRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final CloudinaryConfigRepository configRepository;

    /**
     * Build a Cloudinary instance from DB-stored config.
     * This is created per-request so config changes take effect immediately.
     */
    private Cloudinary buildCloudinary(CloudinaryConfig config) {
        Map<String, String> cloudConfig = new HashMap<>();
        cloudConfig.put("cloud_name", config.getCloudName().trim());
        cloudConfig.put("api_key", config.getApiKey().trim());
        cloudConfig.put("api_secret", config.getApiSecret().trim());
        cloudConfig.put("secure", "true");
        return new Cloudinary(cloudConfig);
    }

    /**
     * Check if Cloudinary is enabled and properly configured.
     */
    public boolean isEnabled() {
        Optional<CloudinaryConfig> configOpt = configRepository.findFirstByOrderByIdAsc();
        return configOpt.isPresent() && Boolean.TRUE.equals(configOpt.get().getEnabled())
                && configOpt.get().getCloudName() != null
                && configOpt.get().getApiKey() != null
                && configOpt.get().getApiSecret() != null;
    }

    /**
     * Upload a file to Cloudinary.
     *
     * @param file      The multipart file to upload
     * @param subfolder Subfolder within the default folder (e.g., "ads", "news",
     *                  "profiles")
     * @return The secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        CloudinaryConfig config = configRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Cloudinary is not configured"));

        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new RuntimeException("Cloudinary is disabled");
        }

        Cloudinary cloudinary = buildCloudinary(config);

        String folder = config.getDefaultFolder();
        if (subfolder != null && !subfolder.isBlank()) {
            folder = folder + "/" + subfolder;
        }

        // Determine resource type based on content type
        String contentType = file.getContentType();
        String resourceType = "image"; // default
        if (contentType != null && contentType.startsWith("video/")) {
            resourceType = "video";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", resourceType,
                        "use_filename", true,
                        "unique_filename", true));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Uploaded file to Cloudinary: {} -> {}", file.getOriginalFilename(), secureUrl);
        return secureUrl;
    }

    /**
     * Delete a file from Cloudinary by its public ID.
     */
    public boolean deleteFile(String publicId) {
        try {
            CloudinaryConfig config = configRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("Cloudinary is not configured"));

            Cloudinary cloudinary = buildCloudinary(config);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            return "ok".equals(status);
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    /**
     * Test the Cloudinary connection by pinging the API.
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            CloudinaryConfig config = configRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("Cloudinary is not configured"));

            Cloudinary cloudinary = buildCloudinary(config);

            // Ping by fetching usage info
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = cloudinary.api().usage(ObjectUtils.emptyMap());

            result.put("success", true);
            result.put("cloudName", config.getCloudName());
            result.put("plan", usage.get("plan"));

            // Storage usage
            if (usage.get("storage") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> storage = (Map<String, Object>) usage.get("storage");
                result.put("storageUsed", storage.get("usage"));
                result.put("storageLimit", storage.get("limit"));
            }

            // Bandwidth usage
            if (usage.get("bandwidth") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bandwidth = (Map<String, Object>) usage.get("bandwidth");
                result.put("bandwidthUsed", bandwidth.get("usage"));
                result.put("bandwidthLimit", bandwidth.get("limit"));
            }

            // Transformations
            if (usage.get("transformations") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> transformations = (Map<String, Object>) usage.get("transformations");
                result.put("transformationsUsed", transformations.get("usage"));
                result.put("transformationsLimit", transformations.get("limit"));
            }

            result.put("message", "Connection successful!");
        } catch (Exception e) {
            log.error("Cloudinary connection test failed", e);
            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Get the current Cloudinary config (masks the API secret for security).
     */
    public Map<String, Object> getConfigSafe() {
        Optional<CloudinaryConfig> configOpt = configRepository.findFirstByOrderByIdAsc();
        Map<String, Object> result = new HashMap<>();

        if (configOpt.isPresent()) {
            CloudinaryConfig config = configOpt.get();
            result.put("id", config.getId());
            result.put("cloudName", config.getCloudName());
            result.put("apiKey", config.getApiKey());
            // Mask secret: show first 4 and last 4 chars
            String secret = config.getApiSecret();
            if (secret != null && secret.length() > 8) {
                result.put("apiSecret", secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4));
            } else {
                result.put("apiSecret", "****");
            }
            result.put("apiSecretFull", config.getApiSecret()); // For form re-population
            result.put("defaultFolder", config.getDefaultFolder());
            result.put("enabled", config.getEnabled());
            result.put("maxImageSizeMb", config.getMaxImageSizeMb());
            result.put("maxVideoSizeMb", config.getMaxVideoSizeMb());
            result.put("uploadPreset", config.getUploadPreset());
            result.put("configured", true);
        } else {
            result.put("configured", false);
            result.put("enabled", false);
        }
        return result;
    }

    /**
     * Save or update Cloudinary config.
     */
    public CloudinaryConfig saveConfig(Map<String, Object> configData) {
        CloudinaryConfig config = configRepository.findFirstByOrderByIdAsc()
                .orElse(new CloudinaryConfig());

        if (configData.containsKey("cloudName")) {
            config.setCloudName(((String) configData.get("cloudName")).trim());
        }
        if (configData.containsKey("apiKey")) {
            config.setApiKey(((String) configData.get("apiKey")).trim());
        }
        if (configData.containsKey("apiSecret")) {
            String newSecret = (String) configData.get("apiSecret");
            // Only update if the secret is not the masked version
            if (newSecret != null && !newSecret.contains("****")) {
                config.setApiSecret(newSecret.trim());
            }
        }
        if (configData.containsKey("defaultFolder")) {
            config.setDefaultFolder((String) configData.get("defaultFolder"));
        }
        if (configData.containsKey("enabled")) {
            config.setEnabled((Boolean) configData.get("enabled"));
        }
        if (configData.containsKey("maxImageSizeMb")) {
            config.setMaxImageSizeMb(((Number) configData.get("maxImageSizeMb")).intValue());
        }
        if (configData.containsKey("maxVideoSizeMb")) {
            config.setMaxVideoSizeMb(((Number) configData.get("maxVideoSizeMb")).intValue());
        }
        if (configData.containsKey("uploadPreset")) {
            config.setUploadPreset((String) configData.get("uploadPreset"));
        }

        return configRepository.save(config);
    }
}
