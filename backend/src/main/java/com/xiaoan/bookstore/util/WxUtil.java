package com.xiaoan.bookstore.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WxUtil {

    private static final Logger log = LoggerFactory.getLogger(WxUtil.class);

    @Value("${app.wx.app-id}")
    private String appId;

    @Value("${app.wx.app-secret}")
    private String appSecret;

    @Value("${app.wx.mch-id:}")
    private String mchId;

    @Value("${app.wx.mch-key:}")
    private String mchKey;

    @Value("${app.wx.notify-url:}")
    private String notifyUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public Map<String, String> createPrepay(String orderNo, int amountFen, String description, String openid) {
        Map<String, String> result = new HashMap<>();
        if (mchId == null || mchId.isEmpty()) {
            log.warn("微信支付未配置，使用模拟支付模式");
            result.put("prepayId", "mock_prepay_" + orderNo);
            result.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
            result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            result.put("paySign", "mock_sign");
            result.put("package", "prepay_id=mock_prepay_" + orderNo);
            return result;
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("appid", appId);
            body.put("mchid", mchId);
            body.put("description", description);
            body.put("out_trade_no", orderNo);
            body.put("notify_url", notifyUrl);

            ObjectNode amount = body.putObject("amount");
            amount.put("total", amountFen);
            amount.put("currency", "CNY");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("prepay_id")) {
                String prepayId = json.get("prepay_id").asText();
                String nonceStr = UUID.randomUUID().toString().replace("-", "");
                String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
                String packageStr = "prepay_id=" + prepayId;

                result.put("prepayId", prepayId);
                result.put("nonceStr", nonceStr);
                result.put("timeStamp", timeStamp);
                result.put("package", packageStr);
                result.put("paySign", "sign_placeholder");
                return result;
            }

            log.error("微信预支付失败: {}", response.body());
            result.put("error", "预支付创建失败");
            return result;
        } catch (Exception e) {
            log.error("微信预支付异常", e);
            result.put("error", "预支付异常");
            return result;
        }
    }
}
