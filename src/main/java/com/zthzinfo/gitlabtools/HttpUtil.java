package com.zthzinfo.gitlabtools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public class HttpUtil {

	private static void printError(HttpResponse response) {
		System.err.println("地址不正确或gitlab服务器异常：");
		System.err.println(response.getStatus());
		System.err.println(response.body());
	}

	public static HttpResponse get(String url, String token) {
		HttpResponse response = HttpRequest.get(url)
				.header("PRIVATE-TOKEN", token)
				.execute();
		if (response.getStatus() != 200) {
			printError(response);
			return null;
		}

		return response;
	}

	public static HttpResponse put(String url, String body, String token) {
		HttpResponse response = HttpRequest.put(url)
				.body(body)
				.header("PRIVATE-TOKEN", token)
				.execute();
		if (response.getStatus() != 200) {
			printError(response);
		}

		return response;
	}
	public static HttpResponse post(String url, String body, String token) {
		HttpResponse response = HttpRequest.post(url)
				.body(body)
				.header("PRIVATE-TOKEN", token)
				.execute();
		if (response.getStatus() != 200) {
			printError(response);
		}

		return response;
	}
}
