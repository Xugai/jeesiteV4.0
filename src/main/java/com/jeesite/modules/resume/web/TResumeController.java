/**
 * Copyright (c) 2013-Now http://jeesite.com All rights reserved.
 */
package com.jeesite.modules.resume.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesite.modules.file.utils.FileUploadUtils;
import com.jeesite.modules.sys.entity.User;
import com.jeesite.modules.sys.utils.EmpUtils;
import com.jeesite.modules.sys.utils.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jeesite.common.config.Global;
import com.jeesite.common.entity.Page;
import com.jeesite.common.web.BaseController;
import com.jeesite.modules.resume.entity.TResume;
import com.jeesite.modules.resume.service.TResumeService;

import java.util.ArrayList;
import java.util.List;

/**
 * t_resumeController
 * @author kyrie
 * @version 2018-05-12
 */
@Controller
@RequestMapping(value = "${adminPath}/resume/tResume")
public class TResumeController extends BaseController {

	@Autowired
	private TResumeService tResumeService;
	/**
	 * 获取数据
	 */
	@ModelAttribute
	public TResume get(String reId, boolean isNewRecord) {
		return tResumeService.get(reId, isNewRecord);
	}

	/**
	 * 查询列表
	 */
	@RequiresPermissions("resume:tResume:view")
	@RequestMapping(value = {"list", ""})
	public String list(TResume tResume, Model model, HttpServletRequest request) {
		User user = UserUtils.getUser();
		//查看用户是否是管理员
//		if (user.isAdmin() || user.isSuperAdmin()){
//            request.getSession().setAttribute("role","manager");
//			model.addAttribute("tResume", tResume);
//			return "modules/resume/tResumeList";
//		}
		//查看该用户是否是部长
		List<String> role = tResumeService.findRole(user.getUserCode());
		for(String str:role){
			if (str.equals("dept")){
				//标识用户角色
                request.getSession().setAttribute("role","dept");
				model.addAttribute("tResume", tResume);
				return "modules/resume/tResumeList";
			}
		}
		//普通用户
		String userCode = user.getUserCode();
		System.out.println("userCode----->"+userCode);
		tResume = tResumeService.findByUserCode(userCode);
		//标识用户角色
        request.getSession().setAttribute("role","user");
//		request.getSession().setAttribute("isAdmin","false");
		model.addAttribute("tResume", tResume);
		if (tResume != null && tResume.getEmpCode() == null){
			tResume.setEmpCode(userCode);
		}
		return "modules/resume/MytResume";
	}

	/**
	 * 查询列表数据
	 */
	@RequiresPermissions("resume:tResume:view")
	@RequestMapping(value = "listData")
	@ResponseBody
	public Page<TResume> listData(TResume tResume, HttpServletRequest request, HttpServletResponse response) {
		System.out.println("tResume-------》"+tResume.toString());
//		Page<TResume> page = tResumeService.findPage(new Page<TResume>(request, response), tResume);
//		request.getSession().setAttribute("isAdmin","true");
		System.out.println("listData");
		Page<TResume> page = new Page<TResume>(request, response);
		//根据角色返回数据
		page = tResumeService.findListByOffice(page,tResume);
		request.getSession().setAttribute("isAdmin","true");
		return page;
	}

	/**
	 * 查看编辑表单
	 */
	@RequiresPermissions("resume:tResume:view")
	@RequestMapping(value = "form")
	public String form(TResume tResume, Model model, HttpServletRequest request) {
		try {
		    String userType  = request.getSession().getAttribute("role").toString();
		    //根据用户角色对同一个页面的数据进行不同的操作
            if (userType.equals("user")){
                tResume.setEmpCode(EmpUtils.getEmployee().getEmpCode());
                model.addAttribute("role","user");
            }else{
                model.addAttribute("role","dept");
            }
        }catch (NullPointerException ex){
		    ex.getStackTrace();
        }
        model.addAttribute("tResume", tResume);
		return "modules/resume/tResumeForm";
	}

	/**
	 * 保存t_resume
	 */
	@RequiresPermissions("resume:tResume:edit")
	@PostMapping(value = "save")
	@ResponseBody
	public String save(@Validated TResume tResume, HttpServletRequest request) {
//		String isAdmin = request.getSession().getAttribute("isAdmin").toString();
//		if (isAdmin.equals("false")){
//			tResume.setEmpCode(EmpUtils.getEmployee().getEmpCode());
//		}
		tResumeService.save(tResume);
		return renderResult(Global.TRUE, "保存简历成功！");
	}

	/**
	 * 更新t_resume
	 */
	@RequiresPermissions("resume:tResume:edit")
	@PostMapping(value = "update")
	@ResponseBody
	public String update(@Validated TResume tResume) {
		//更新简历信息
		tResumeService.update(tResume);
		// 保存上传附件
		FileUploadUtils.saveFileUpload(tResume.getReId(), "tResume_file");
		return renderResult(Global.TRUE, "修改简历成功！");
	}
	/**
	 * 停用t_resume
	 */
	@RequiresPermissions("resume:tResume:edit")
	@RequestMapping(value = "disable")
	@ResponseBody
	public String disable(TResume tResume) {
		tResume.setStatus(TResume.STATUS_DISABLE);
		tResumeService.updateStatus(tResume);
		return renderResult(Global.TRUE, "停用简历管理功能成功");
	}

	/**
	 * 启用t_resume
	 */
	@RequiresPermissions("resume:tResume:edit")
	@RequestMapping(value = "enable")
	@ResponseBody
	public String enable(TResume tResume) {
		tResume.setStatus(TResume.STATUS_NORMAL);
		tResumeService.updateStatus(tResume);
		return renderResult(Global.TRUE, "启用简历管理成功");
	}

	/**
	 * 删除t_resume
	 */
	@RequiresPermissions("resume:tResume:edit")
	@RequestMapping(value = "delete")
	@ResponseBody
	public String delete(TResume tResume) {
		tResumeService.delete(tResume);
		return renderResult(Global.TRUE, "删除简历管理成功！");
	}

}