package com.xiaoan.bookstore.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class WxUtil {

    private static final Logger log = LoggerFactory.getLogger(WxUtil.class);

    @Value("${app.wx.app-id}")
    private String appId;

    @Value("${app.wx.app-secret}")
    private String appSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过 code 获取微信 openid
     */
    public String getOpenid(String code) {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("openid")) {
                return json.get("openid").asText();
            }
            log.error("微信登录失败: {}", response.body());
            return null;
        } catch (Exception e) {
            log.error("微信登录异常", e);
            return null;
        }
    }
}
