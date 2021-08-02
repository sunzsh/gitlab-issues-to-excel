package com.zthzinfo.gitlabtools.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {
	public User(String name) {
		this.name = name;
	}

	private Long id;
	private String username;
	private String name;
	private String state;
	private String avatar_url;
	private String web_url;
}
