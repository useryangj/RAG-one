package com.example.ragone.controller;

import com.example.ragone.service.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 音频处理控制器
 */
@RestController
@RequestMapping("/audio")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AudioController {
    
    @Autowired
    private AudioService audioService;
    
    /**
     * 语音转文本接口
     */
    @PostMapping(value = "/speech-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> speechToText(@RequestParam("audio") MultipartFile audioFile,
                                        @RequestParam(value = "language", defaultValue = "zh") String language) {
        try {
            if (audioFile.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "音频文件不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String transcription = audioService.speechToText(audioFile, language);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transcription", transcription);
            response.put("language", language);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "语音转文本失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 文本转语音接口
     */
    @PostMapping(value = "/text-to-speech")
    public ResponseEntity<?> textToSpeech(@RequestParam("text") String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "文本内容不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            byte[] audioData = audioService.textToSpeech(text);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("audio/mpeg"))
                    .header("Content-Disposition", "attachment; filename=\"speech.mp3\"")
                    .body(audioData);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "文本转语音失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
