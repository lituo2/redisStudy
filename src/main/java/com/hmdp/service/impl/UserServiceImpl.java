package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1检验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
           //2不符合，返回
            return Result.fail("手机号格式错误！");
        }
        //3生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4验证码存入session->redis
//        session.setAttribute("code",code);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5发送验证码（麻烦还花钱，先不实现
        log.debug("验证码发送成功，验证码：{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2不符合，返回
            return Result.fail("手机号格式错误！");
        }

        //2校验验证码
//        if (RegexUtils.isCodeInvalid(loginForm.getCode())) {
//            return Result.fail("验证码格式错误！");
//        }
        //3核对校验码 ->redis
        String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
//        String code = (String) session.getAttribute("code");
        if (code == null || !cachecode.equals(code)) {
            return Result.fail("验证码错误！");
        }
        //4查询手机号是否存在，若存在，登录成功
        User user = query().eq("phone", phone).one();
        //5若不存在，创建新用户。
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        //保持用户信息到session->redis
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        String token = UUID.randomUUID().toString(true);
        //换成userdto类型
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //userdto变成hasmap
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        //出入redis  k为login：token+uuid随机字符串 value为map类型的user对象
        String tokenkey = LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenkey,userMap);
        //设置rdis数据的有效期
        stringRedisTemplate.expire(tokenkey,LOGIN_USER_TTL,TimeUnit.SECONDS);
        //返回token ，这块前端需要返回token  没搞明白
        return Result.ok(token);
    }
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
