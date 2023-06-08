package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {

//    private StringRedisTemplate stringRedisTemplate;
//
//    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
//        this.stringRedisTemplate = stringRedisTemplate;
//    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //加了一个外部拦截器，大部分功能已经在RefreshTokenInterceptor里实现
//        //1获取session ->获取请求头token -> 基于toekn获取redis中的用户
////        HttpSession session = request.getSession();
//        String token = request.getHeader("authorization");
//        //2判断用户是否存在，不存在，拦截
//        if (StrUtil.isBlank(token)) {
//            response.setStatus(401);
//            return false;
//        }
//        String key = RedisConstants.LOGIN_USER_KEY + token;
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//
//
////        Object user = session.getAttribute("user");
//        if (userMap.isEmpty()) {
//            response.setStatus(401);
//            return false;
//        }
//        //把hasmap转换为userDto对象
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//
//        //3存在，存入threadlocal
////        UserHolder.saveUser((UserDTO) user);
//        UserHolder.saveUser(userDTO);
//
//        //刷新token有效期
//        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
//
//        //4放行

        //判断是否有用户
        if (UserHolder.getUser()==null) {
            response.setStatus(401);
            return false;
        }



        return true;
    }

}
