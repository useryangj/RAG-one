package com.example.ragone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 音频处理服务
 */
@Service
public class AudioService {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    private static final String SPEECH_TO_TEXT_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";
    private static final String TEXT_TO_SPEECH_URL = "https://api.siliconflow.cn/v1/audio/speech";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public AudioService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 语音转文本
     */
    public String speechToText(MultipartFile audioFile, String language) throws IOException {
        logger.info("开始语音转文本，文件大小: {} bytes, 语言: {}", audioFile.getSize(), language);
        
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);
        
        // 准备请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        });
        body.add("model", "FunAudioLLM/SenseVoiceSmall");
        body.add("language", language);
        body.add("response_format", "json");
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    SPEECH_TO_TEXT_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String transcription = jsonResponse.get("text").asText();
                logger.info("语音转文本成功，转录结果: {}", transcription);
                return transcription;
            } else {
                logger.error("语音转文本API调用失败，状态码: {}", response.getStatusCode());
                throw new RuntimeException("语音转文本API调用失败");
            }
        } catch (Exception e) {
            logger.error("语音转文本过程中发生错误", e);
            throw new RuntimeException("语音转文本失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 文本转语音
     */
    public byte[] textToSpeech(String text) throws IOException {
        logger.info("开始文本转语音，文本长度: {}", text.length());
        
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        // 准备请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "fnlp/MOSS-TTSD-v0.5");
        requestBody.put("input", text);
        requestBody.put("voice", "fnlp/MOSS-TTSD-v0.5:alex");
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    TEXT_TO_SPEECH_URL,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] audioData = response.getBody();
                logger.info("文本转语音成功，音频数据大小: {} bytes", audioData != null ? audioData.length : 0);
                return audioData;
            } else {
                logger.error("文本转语音API调用失败，状态码: {}", response.getStatusCode());
                throw new RuntimeException("文本转语音API调用失败");
            }
        } catch (Exception e) {
            logger.error("文本转语音过程中发生错误", e);
            throw new RuntimeException("文本转语音失败: " + e.getMessage(), e);
        }
    }
}
