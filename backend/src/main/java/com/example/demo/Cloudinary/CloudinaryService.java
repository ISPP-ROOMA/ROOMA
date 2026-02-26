package com.example.demo.Cloudinary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${CLOUDINARY_CLOUD_NAME:${cloudinary.cloud_name}}") String name,
                             @Value("${CLOUDINARY_API_KEY:${cloudinary.api_key}}") String key,
                             @Value("${CLOUDINARY_API_SECRET:${cloudinary.api_secret}}") String secret) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", name);
        config.put("api_key", key);
        config.put("api_secret", secret);
        this.cloudinary = new Cloudinary(config);
    }

    public Map<String, Object> upload(MultipartFile multipartFile, String folder) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", "rooma/" + folder,
            "resource_type", "image"
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(multipartFile.getBytes(), params);
        return result;
    }

    public void deleteByPublicId(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

}