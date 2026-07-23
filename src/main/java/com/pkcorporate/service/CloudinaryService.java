package com.pkcorporate.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pkcorporate.exception.CloudinaryException;

import java.io.IOException;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, String> uploadFile(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "pk-corporate/" + folder,
                        "resource_type", "auto",
                        "transformation", "q_auto,f_auto"
                ));

        return Map.of(
                "url", (String) result.get("secure_url"),
                "publicId", (String) result.get("public_id")
        );
    }

    public Map<String, String> uploadImage(MultipartFile file, String folder) throws IOException {
        try {
            log.info("Uploading image to Cloudinary...");

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "pk-corporate/" + folder,
                            "resource_type", "image",
                            "transformation", "q_auto,f_webp,w_1200"
                    ));

            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            log.info("Upload successful");
            log.info("Cloudinary URL: {}", url);

            return Map.of(
                    "url", url,
                    "publicId", publicId
            );
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            if (e instanceof IOException io) throw io;
            throw new CloudinaryException("Cloudinary upload failed", e);
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new CloudinaryException("Cloudinary delete failed", e);
        }
    }
}
