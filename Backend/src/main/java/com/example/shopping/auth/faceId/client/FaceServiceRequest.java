package com.example.shopping.auth.faceId.client;

import lombok.Data;
import java.util.List;

@Data
public class FaceServiceRequest {
    private String image;
    private List<String> images;
    private String user_id;
}
