package org.jeecg.modules.system.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(redisTemplate))
                .excludePathPatterns("/login")
                .excludePathPatterns("/sys/randomImage/**")
                .excludePathPatterns("/sys/checkCaptcha")
                .excludePathPatterns("/sys/login")
                .excludePathPatterns("/sys/mLogin")
                .excludePathPatterns("/sys/logout")
                .excludePathPatterns("/sys/thirdLogin/**")
                .excludePathPatterns("/sys/phoneLogin")
                .excludePathPatterns("/thirdApi/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/swagger**/**")
                .excludePathPatterns("/error")
                .excludePathPatterns("/sys/getEncryptedString")
                .excludePathPatterns("/sys/sms")
                .excludePathPatterns("/sys/user/checkOnlyUser")
                .excludePathPatterns("/sys/user/register")
                .excludePathPatterns("/sys/permission/getPermCode")
                .excludePathPatterns("/sys/permission/getUserPermissionByToken")
                .excludePathPatterns("/sys/annountCement/listByUser")
                .excludePathPatterns("/sys/user/getUserInfo")
                .excludePathPatterns("/sys/user/phoneVerification")
                .excludePathPatterns("/sys/user/passwordChange")
                .excludePathPatterns("/auth/2step-code")
                .excludePathPatterns("/sys/common/static/**")
                .excludePathPatterns("/sys/common/pdf/**")
                .excludePathPatterns("/generic/**")
                .excludePathPatterns("/sys/getLoginQrcode/**")
                .excludePathPatterns("/sys/getQrcodeToken/**")
                .excludePathPatterns("/sys/checkAuth")
                .excludePathPatterns("/deviceManagement/deviceManagement/getConfigInfo")
                .excludePathPatterns("/deviceManagement/deviceManagement/devGetRequest")
                .excludePathPatterns("/deviceManagement/deviceManagement/devPostRequest")
                .excludePathPatterns("/deviceManagement/deviceManagement/devSync")
                .excludePathPatterns("/")
                .excludePathPatterns("/doc.html")
                .excludePathPatterns("/**/*.js")
                .excludePathPatterns("/**/*.css")
                .excludePathPatterns("/**/*.html")
                .excludePathPatterns("/**/*.svg")
                .excludePathPatterns("/**/*.pdf")
                .excludePathPatterns("/**/*.jpg")
                .excludePathPatterns("/**/*.png")
                .excludePathPatterns("/**/*.gif")
                .excludePathPatterns("/**/*.ico")
                .excludePathPatterns("/**/*.ttf")
                .excludePathPatterns("/**/*.woff")
                .excludePathPatterns("/**/*.woff2")
                .excludePathPatterns("/druid/**")
                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/v2/**")
                .excludePathPatterns("/sys/annountCement/show/**")
                .excludePathPatterns("/**/*.js.map")
                .excludePathPatterns("/**/*.css.map")
                .excludePathPatterns("/test/bigScreen/**")
                .excludePathPatterns("/bigscreen/template1/**")
                .excludePathPatterns("/websocket/**")
                .excludePathPatterns("/newsWebsocket/**")
                .excludePathPatterns("/vxeSocket/**")
                .excludePathPatterns("/ViWebSocket/**")
                .excludePathPatterns("/alarmToWebSocket/**")
                .excludePathPatterns("/deviceinfo/**")
                .excludePathPatterns("/platforminfo/**")
                .excludePathPatterns("/test/seata/**");
//                .excludePathPatterns("/**");


    }
}
