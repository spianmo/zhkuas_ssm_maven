package com.zhku.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONException;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.zhku.bean.Admin;
import com.zhku.bean.User;
import com.zhku.module.weibo.WeiBoUserMgm;
import com.zhku.module.weibo.bean.AccessToken;
import com.zhku.module.weibo.bean.WeiboException;
import com.zhku.service.db.IAdminService;
import com.zhku.service.db.IUserService;
import com.zhku.utils.WebConfigUtils;
import com.zhku.web.Constants.SessionKey;

@Controller
public class LoginController {
	@Autowired
	private IUserService userService;
	@Autowired
	private IAdminService adminService;
	@RequestMapping("/account/logout")
	public String logout(HttpSession session){
		session.removeAttribute(SessionKey.LOGIN_USER.toString());
		return "redirect:/index";
	}
	@RequestMapping("/weibo/login")
	public ModelAndView  weiboLogin(HttpServletRequest request ,HttpSession session){
		String referer = request.getHeader("Referer");
		session.setAttribute(SessionKey.LOGIN_REFERER.toString(), referer);
//		String weiboLoginUrl ="https://api.weibo.com/oauth2/authorize?client_id="+WebConfigUtils.getValue("client_ID")+"&response_type=code&redirect_uri="+WebConfigUtils.getValue("domain")+"/"+WebConfigUtils.getValue("redirect_URI");
		String centerRediretService =new StringBuffer().append( WebConfigUtils.getValue("rz_host")).append("/weibo/login?service=").append(WebConfigUtils.getValue("domain")).append("/").toString();
		return new ModelAndView("redirect:"+centerRediretService);
	}
	@RequestMapping("/account/callback")
	public ModelAndView loginCallBack(String code,HttpSession session,HttpServletRequest request){
		if(code==null) new ModelAndView("redirect:"+session.getServletContext().getContextPath());
		String referer = (String) session.getAttribute(SessionKey.LOGIN_REFERER.toString());
		WeiBoUserMgm weiboUserMgm = new WeiBoUserMgm();
		try {
			AccessToken accessToken = weiboUserMgm.getAccessTokenByCode(code);
			com.zhku.module.weibo.bean.User weiBoUser = weiboUserMgm.getWeiboUser(accessToken,
					accessToken.getUid());
			if(weiBoUser==null){ 
				session.setAttribute("message", "微博授权失败，请清除浏览器缓存后再进行授权!");
				return new ModelAndView("redirect:"+session.getServletContext().getContextPath()+"/message/error");
			}
			User user= null;
			if(referer!=null&&referer.contains("admin")){
				user = new User();
				user.setSinaUid(weiBoUser.getId());
				Admin admin = new Admin();
				admin.setUser(user);
				Admin findAdmin = adminService.findAdmin(admin);
				if(findAdmin==null){
					session.setAttribute("result", "用户名或者密码错误!");
				}else{
					session.setAttribute(SessionKey.ADMIN_USER.toString(), findAdmin);
				}
			}else{
				user =userService.findUserBySinaUid(weiBoUser.getId());
				if(user==null){ //第一次登录 ,进行 信息的保存
					user=new User();
					user.setAvatorUrl(weiBoUser.getAvatarLarge());
					user.setDescription(weiBoUser.getDescription());
					user.setNickName(weiBoUser.getScreenName());
					user.setSinaUid(weiBoUser.getId());
					userService.register(user);
				}
				session.setAttribute(SessionKey.LOGIN_USER.toString(), user);
			}
		} catch (JSONException e) {
			session.setAttribute("message", "微博登录失败,请重新登录!");
			return new ModelAndView("redirect:"+session.getServletContext().getContextPath()+"/message/error");
		} catch (WeiboException e) {
			session.setAttribute("message", "微博登录失败,可能是你的账户问题,请重新登录!");
			return new ModelAndView("redirect:"+session.getServletContext().getContextPath()+"/message/error");
		}catch (Exception e) {
			session.setAttribute("message", "微博登录失败,可能是你的账户问题,请重新登录!");
			return new ModelAndView("redirect:"+session.getServletContext().getContextPath()+"/message/error");
		}
		if(referer==null||referer.trim().equals("")){
			referer = session.getServletContext().getContextPath()+"/";
		}
		return new ModelAndView("redirect:"+referer);
	}
	public IUserService getUserService() {
		return userService;
	}
	public void setUserService(IUserService userService) {
		this.userService = userService;
	}
	public IAdminService getAdminService() {
		return adminService;
	}
	public void setAdminService(IAdminService adminService) {
		this.adminService = adminService;
	}
}
