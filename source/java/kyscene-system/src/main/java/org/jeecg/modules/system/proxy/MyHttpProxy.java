package org.jeecg.modules.system.proxy;

import org.apache.http.client.utils.URIUtils;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MyHttpProxy extends ProxyServlet {
    private String targetAddr = null;
    private String restfulAddr = null;
    private String targetUri = null;
    private String localAddr = null;

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
//        String targetUri = "http://192.168.1.95:8038/kyscene/sys/randomImage/1629428467008?_t=1685438461605";
//        http://192.168.1.156:8038/alarmImg/home/super/AI_Service/alarm_picture/download/image/192.168.60.250/2023-05-10_11_28_51_9.jpg
        servletRequest.setAttribute(ATTR_TARGET_URI, null);
        String path = servletRequest.getRequestURI();
        String queryParams = servletRequest.getQueryString();
        String query = null;
        if (queryParams != null) {
            String[] queryArr = queryParams.split("&");
            query = queryArr[0];
        }
        if (query != null && !query.isEmpty() && !query.equals("null")) {
            targetAddr = "http://" + query + ":8038";
            restfulAddr = "http://" + query + ":8899";
            localAddr = "http://" + query + ":8080";
        }
        System.out.println("query = " + query);
        if (path.contains("/alarmImg")) {
            targetUri = targetAddr + "/alarmImg";
        }
        if (path.contains("/cameraImg")) {
            targetUri = targetAddr + "/cameraImg";
        }
        /**
         * 实时预览相关restful接口
         */
        if (path.contains("/heartbeat")) {
            targetUri = restfulAddr + "/heartbeat";
        }
        if (path.contains("/firstrtsp")) {
            targetUri = restfulAddr + "/firstrtsp";
        }
        if (path.contains("/startrtsp")) {
            targetUri = restfulAddr + "/startrtsp";
        }
        if (path.contains("/stoprtsp")) {
            targetUri = restfulAddr + "/stoprtsp";
        }
        if (path.contains("/refreshcover")) {
            targetUri = restfulAddr + "/refreshcover";
        }
        /**
         * 十六分屏幕restful接口
         */
//        if (path.contains("/firstscreen")){
//            targetUri = restfulAddr + "/firstscreen";
//        }
        if (path.contains("/startscreen")) {
            targetUri = restfulAddr + "/startscreen";
        }
        if (path.contains("/stopscreen")) {
            targetUri = restfulAddr + "/stopscreen";
        }

        /**
         * 轮询组restful接口
         */
        if (path.contains("/addjob")) {
            targetUri = restfulAddr + "/addjob";
        }
        if (path.contains("/removejob")) {
            targetUri = restfulAddr + "/removejob";
        }
        /**
         * 算法包升级
         */
        if (path.contains("/uploadmodel")) {
            targetUri = restfulAddr + "/uploadmodel";
        }
        /**
         * 端云同步
         */
//        if (path.contains("/getConfigInfo")){
//            targetUri = localAddr + "/deviceManagement/deviceManagement/getConfigInfo";
//        }
//        System.out.println("转完的url："+targetUri);
        super.targetUri = targetUri;
        URI uri = null;
        try {
            uri = new URI(targetUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        servletRequest.setAttribute(ATTR_TARGET_HOST, null);
        super.targetHost = URIUtils.extractHost(uri);
        super.service(servletRequest, servletResponse);
    }

}
