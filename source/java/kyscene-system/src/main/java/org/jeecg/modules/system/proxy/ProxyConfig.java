package org.jeecg.modules.system.proxy;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProxyConfig {
    MyHttpProxy myHttpProxy = new MyHttpProxy();

    @Bean
    public ServletRegistrationBean<MyHttpProxy> servletRegistrationBeanAlarm(){
        ServletRegistrationBean<MyHttpProxy> servletRegistrationBeanAlarm = new ServletRegistrationBean<>(myHttpProxy,
                new String[]{"/alarmImg/*",
                        "/cameraImg/*",
                        "/refreshcover/*",
                        "/heartbeat/*",
                        "/firstrtsp/*",
                        "/startrtsp/*",
                        "/stoprtsp/*",
                        "/addjob/*",
                        "/removejob/*",
                        "/startscreen/*",
                        "/stopscreen/*",
                        "/uploadmodel/*"

        });
//  "/deviceManagement/deviceManagement/getConfigInfo/*"
        Map<String, String> params = new HashMap<>();
        params.put("targetUri", "alarmImg");
        params.put("log", "true");
        System.out.println("params = " + params);
        servletRegistrationBeanAlarm.setInitParameters(params);
        return servletRegistrationBeanAlarm;
    }

//    @Bean
//    public ServletRegistrationBean<MyHttpProxy> servletRegistrationBeanCamera(){
//        ServletRegistrationBean<MyHttpProxy> servletRegistrationBeanCamera = new ServletRegistrationBean<>(myHttpProxy, "/cameraImg/*");
//        Map<String, String> params = new HashMap<>();
//        params.put("targetUri", "cameraImg");
//        params.put("log", "true");
//        servletRegistrationBeanCamera.setInitParameters(params);
//        return servletRegistrationBeanCamera;
//    }

}
