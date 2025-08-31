package com.tvz.mediaapp.backend.controller;

import com.tvz.mediaapp.backend.service.StorageService;
import com.tvz.mediaapp.dto.DownloadRequestDto;
import com.tvz.mediaapp.dto.UploadRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final StorageService storageService;

    @PostMapping("/generate-upload-url")
    public ResponseEntity<Map<String, String>> generateUploadUrl(@RequestBody UploadRequestDto request) {
        String requestedFilename = request.getFilename();

        URL uploadUrl = storageService.generatePreSignedUploadUrl(requestedFilename);

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl.toString());
        response.put("finalFilename", requestedFilename);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-download-urls")
    public ResponseEntity<Map<String, String>> generateDownloadUrls(@RequestBody DownloadRequestDto request) {
        Map<String, String> urls = storageService.generatePreSignedDownloadUrls(request.getPostUuid(), request.getFilenames());
        return ResponseEntity.ok(urls);
    }
}