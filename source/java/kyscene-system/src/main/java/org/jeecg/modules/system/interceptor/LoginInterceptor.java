package org.jeecg.modules.system.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.HttpMethod;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.system.entity.TokenUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    public LoginInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(HttpMethod.OPTIONS.toString().equals(request.getMethod())){
            System.out.println("OPTIONS请求，放行");
            return true;
        }
        String username = JwtUtil.getUserNameByToken(request);
        String token = request.getHeader(CommonConstant.X_ACCESS_TOKEN);
//        System.out.println("8888:"+token);
//        String requestURI = request.getRequestURI();
        if(token==null){
            //没有登录
            response.setStatus(401);
//            System.out.println("拦截 401");
            return false;
        }
        JSONObject obj = new JSONObject();
        obj.put("username",username);
        obj.put("token",token);
        TokenUtil tokenUtil = BeanUtil.toBean(obj, TokenUtil.class);
        String tokenValue = redisTemplate.opsForValue().get(tokenUtil.getUsername());
//        System.out.println("tokenValue = " + tokenValue);
        if(tokenValue!=null&&tokenValue.equals(tokenUtil.getToken())){
//            System.out.println("token正确,放行");
            return true;
        }
        response.setStatus(402);
        return false;
    }
}