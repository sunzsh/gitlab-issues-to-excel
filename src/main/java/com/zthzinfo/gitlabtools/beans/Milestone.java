package com.zthzinfo.gitlabtools.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Milestone {
	private Long id;
	private Long iid;
	private Long project_id;
	private String title;
	private String description;
	private String state;
	private String created_at;
	private String updated_at;
	private String due_date;
	private String start_date;
	private String web_url;

}
