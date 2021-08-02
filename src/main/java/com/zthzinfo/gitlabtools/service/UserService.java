package com.zthzinfo.gitlabtools.service;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.zthzinfo.gitlabtools.HttpUtil;
import com.zthzinfo.gitlabtools.beans.User;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserService {

	public UserService(String baseUrl) {
		this.baseUrl = baseUrl;
	}


	private String baseUrl;

	private String token;

	public List<User> getUsers() {
		String url = baseUrl + "/users?per_page=100000";
		HttpResponse response = HttpUtil.get(url, token);

		String body = response.body();

		List<User> list = JSON.parseArray(body, User.class);

		return list;
	}

}
