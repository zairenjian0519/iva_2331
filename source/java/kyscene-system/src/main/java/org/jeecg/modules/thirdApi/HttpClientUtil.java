package org.jeecg.modules.thirdApi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * HttpClient 工具类
 */
public class HttpClientUtil {
    public static final String APPLICATION_JSON_VALUE = "application/json";
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final Pattern RTSP_CREDENTIALS = Pattern.compile("(?i)(rtsp://)[^\\s@]+@");

    // 连接超时时间豪秒
    private static final Integer CONN_TIME_OUT = 3000;

    // socket超时时间
    private static final Integer SOCKET_TIME_OUT = 10000;

    /** 每个路由的最大请求数，默认2 */
    private static final Integer DEFAULT_MAX_PER_ROUTE = 40;

    /** 最大连接数，默认20 */
    private static final Integer MAX_TOTAL = 400;


    private static HttpClient httpClient;

    static {
        // 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONN_TIME_OUT)
                .setConnectionRequestTimeout(CONN_TIME_OUT)
                .setSocketTimeout(SOCKET_TIME_OUT)
                .build();

        // 管理 http连接池
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        cm.setMaxTotal(MAX_TOTAL);

        httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).build();
    }

    public static String requestGet(String url, Map<String, String> paramsMap, Map<String, String> headerMap) throws Exception {

        long start = System.currentTimeMillis();

        List<NameValuePair> params = initParams(paramsMap);
        // Get请求
        HttpGet httpGet = new HttpGet(url);
        // header  参数
        if (!headerMap.isEmpty()) {
            Set<String> keySet = headerMap.keySet();
            for (String key: keySet){
                httpGet.addHeader(key, headerMap.get(key));
            }
        }
        try {
            // 设置参数
            String str = EntityUtils.toString(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            String uriStr = StringUtils.isEmpty(str) ? httpGet.getURI().toString() : httpGet.getURI().toString() + "?" + str;
            httpGet.setURI(new URI(uriStr));
            // 发送请求
            HttpResponse response = httpClient.execute(httpGet);
            logger.info("GET request  url:{} time:{}", url, System.currentTimeMillis() - start);

            // 获取返回数据
            return getSuccessRetFromResp(response, url, JSON.toJSONString(paramsMap));
        } finally {
            // 必须释放连接，连接用完后会阻塞
            httpGet.releaseConnection();
        }
    }

    /**
     *
     * @param url
     * @param paramsMap
     * @return 响应结果
     */
    public static String requestPost(String url, Map<String, String> paramsMap) throws Exception {
        long start = System.currentTimeMillis();

        List<NameValuePair> params = initParams(paramsMap);

        HttpPost httpPost = new HttpPost(url);

        try {

            httpPost.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

            HttpResponse response = httpClient.execute(httpPost);

            logger.info("POST request  url:{} response:{}  time:{}", url, response, System.currentTimeMillis() - start);

            return getSuccessRetFromResp(response, url, JSON.toJSONString(paramsMap));
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * POST json 格式数据
     *
     */
    public static String requestPostJsonStr(String url, String json) throws Exception {

        long start = System.currentTimeMillis();

        HttpPost httpPost = new HttpPost(url);
        try {
            if (json != null) {
                StringEntity entity = new StringEntity(json, Consts.UTF_8);
                entity.setContentType(APPLICATION_JSON_VALUE);
                httpPost.setEntity(entity);
            } else {
                httpPost.setHeader("Content-Type", "application/json");
            }

            HttpResponse response = httpClient.execute(httpPost);

            logger.info("POST request  url:{} response:{}  time:{}", url, response, System.currentTimeMillis() - start);

            return getSuccessRetFromResp(response, url, json);
        } finally {
            // 资源释放
            httpPost.releaseConnection();
        }

    }

    /**
     * post json 格式数据
     *
     * @param url
     * @param obj
     * @return
     * @throws Exception
     */
    public static String requestPostJson(String url, Object obj) throws Exception {
        String params = obj != null? JSON.toJSONString(obj): null;
        return requestPostJsonStr(url, params);
    }

    /**
     * post json 并可配置header参数
     */
    public static String requestPostHeader(String url, String json, Map<String, String> headerMap) throws Exception{

        long start = System.currentTimeMillis();

        HttpPost httpPost = new HttpPost(url);
        if (!headerMap.isEmpty()) {
            Set<String> keySet = headerMap.keySet();
            for (String key: keySet){
                httpPost.addHeader(key, headerMap.get(key));
            }
        }

        try {
            StringEntity entity = new StringEntity(json, Consts.UTF_8);
            entity.setContentType(APPLICATION_JSON_VALUE);
            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost);

            logger.info("POST request  url:{} response:{}  time:{}", url, response, System.currentTimeMillis() - start);

            return getSuccessRetFromResp(response, url, json);
        } finally {
            // 资源释放
            httpPost.releaseConnection();
        }
    }

    /**
     * 可处理JSON参数的post请求
     * 并且可添加header参数
     */
    public static String requestPostJsonWithHeader(String url, Object obj, Map<String, String> headerMap) throws Exception {
        String params = JSON.toJSONString(obj);
        return requestPostHeader(url, params, headerMap);
    }

    private static String getSuccessRetFromResp(HttpResponse response, String url, String params) throws Exception {
        String retStr;
        // 检验状态码，如果成功接收数据
        int code = response.getStatusLine().getStatusCode();

        if (code == 200) {
            retStr = EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } else {
            throw new RuntimeException(String.format("Http request error:%s, url:%s", response, url));
        }

        logger.debug("Http request retStr:{}. url:{}", hideBase64(retStr), url);
        return retStr;
    }

    private static List<NameValuePair> initParams(Map<String, String> paramsMap) {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (paramsMap == null)
            return params;

        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        return params;
    }

    // 隐藏城建智控请求返回的base64串
    private static String hideBase64(String responseStr) {
        String sanitizedResponse = maskRtspCredentials(responseStr);
        try {
            JSONObject jsonObject = JSON.parseObject(sanitizedResponse);
            JSONObject data = jsonObject.getObject("data", JSONObject.class);
            if (data == null) {
                return sanitizedResponse;
            }

            JSONArray records = data.getObject("records", JSONArray.class);
            hideBase64Item(records);

            JSONArray addList = data.getObject("addList", JSONArray.class);
            hideBase64Item(addList);

            return jsonObject.toJSONString();
        } catch (Exception e) {
            logger.debug("Response is not the object schema handled by hideBase64: {}", e.getMessage());
        }
        return sanitizedResponse;
    }

    private static String maskRtspCredentials(String text) {
        if (text == null) return null;
        return RTSP_CREDENTIALS.matcher(text).replaceAll("$1***@");
    }

    private static void hideBase64Item(JSONArray records) {
        if (records != null) {
            for (int i = 0; i < records.size(); i++) {
                JSONObject item = records.getObject(i, JSONObject.class);
                if (item != null && item.containsKey("photoBase64")) {
                    item.put("photoBase64", "base64Data");
                }
            }
        }
    }
}

