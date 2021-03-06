package com.civism.shiro;


import com.civism.constants.SsoConstants;
import com.civism.dao.RedisClient;
import com.civism.error.CivismException;
import com.civism.error.CustomAccountException;
import com.civism.service.UserService;
import com.civism.utils.SerializeUtil;
import com.civism.vo.LoginEntity;
import com.civism.vo.UserInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


/**
 * @author star
 * @date 2018/3/19 下午4:46
 */
public class UpmsRealm extends AuthorizingRealm {

    private static final Logger logger = LoggerFactory.getLogger(UpmsRealm.class);

    @Resource
    private RedisClient redisClient;

    @Resource
    private UserService userService;

    /**
     * 授权
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        UserInfo userInfo = (UserInfo) SecurityUtils.getSubject().getPrincipal();

        //根据用户查询有数据接口权限
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        Set<String> sets = new HashSet<>();
        sets.add("/civism/index.html");
        sets.add("/civism/hello.html");
        info.setStringPermissions(sets);
        return info;
    }

    /**
     * 认证信息，主要针对用户登录，
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        SsoUserNameToken ssoUserNameToken = (SsoUserNameToken) authenticationToken;
        LoginEntity loginEntity = ssoUserNameToken.getLoginEntity();
        UserInfo userInfo = null;
        try {
            userInfo = userService.login(loginEntity);
            Serializable id = SecurityUtils.getSubject().getSession().getId();
            userInfo.setToken((String) id);
            redisClient.set((String) id, SerializeUtil.serialize(userInfo), SsoConstants.DEFAULT_LOGIN_EXPIRE);
        } catch (CivismException e) {
            throw new CustomAccountException(e.getErrorCode());
        }
        return new SimpleAuthenticationInfo(userInfo, userInfo.getToken(), getName());
    }

}
