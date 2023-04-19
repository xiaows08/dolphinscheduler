package org.apache.dolphinscheduler.plugin.alert.wechat.robot;

import org.apache.dolphinscheduler.alert.api.AlertResult;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WeChatRobotSender {

    private final String url;
    private final String msgType;

    public WeChatRobotSender(Map<String, String> params) {
        url = params.get(WeChatRobotAlertParamsConstants.NAME_WECHAT_ROBOT_WEB_HOOK);
        msgType = params.get(WeChatRobotAlertParamsConstants.NAME_MSG_TYPE);
    }

    public AlertResult sendGroupChatMsg(String title, String content) {
        AlertResult result = new AlertResult();
        String msg = generateMsg(title, content);
        HttpPost httpPost = constructHttpPost(url, msg);

        try (CloseableHttpClient httpClient = getDefaultClient()) {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consume(entity);
            result.setStatus("true");
            result.setMessage(res);
            response.close();
        } catch (IOException e) {
            log.warn("send WeChat Group alert msg exception: {}", e.getMessage());
            result.setStatus("false");
            result.setMessage("send WeChat Group alert fail.");
        }
        return result;
    }

    private static CloseableHttpClient getDefaultClient() {
        return HttpClients.createDefault();
    }

    private static HttpPost constructHttpPost(String url, String msg) {
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(msg, StandardCharsets.UTF_8);
        post.setEntity(entity);
        post.addHeader("Content-Type", "application/json; charset=utf-8");
        return post;
    }

    private String generateMsg(String title, String content) {
        Map<String, Object> map = new HashMap<>();
        map.put("msgtype", msgType);
        Map<String, String> text = new HashMap<>();
        // 拼接markdown文本 String.join("\n", title, content)
        text.put("content", markdown(title, content));
        map.put(msgType, text);
        return JSONUtils.toJsonString(map);
    }

    private String markdown(String title, String content) {
        if (StringUtils.isNotEmpty(content)) {
            List<LinkedHashMap> maps = JSONUtils.toList(content, LinkedHashMap.class);
            if (null == maps || maps.isEmpty()) {
                log.error("content is null");
                throw new RuntimeException("content map is null");
            }
            StringBuilder contents = new StringBuilder(100);
            contents.append(String.format("** %s **%n", title));
            for (LinkedHashMap<String, Object> map : maps) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    contents.append("> ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                }
                contents.append("\n");
            }
            return contents.toString();
        }
        return null;
    }
}
