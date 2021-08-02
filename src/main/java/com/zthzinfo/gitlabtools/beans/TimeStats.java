package com.zthzinfo.gitlabtools.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TimeStats {
	private Long time_estimate;
	private Long total_time_spent;
	private String human_time_estimate;
	private String human_total_time_spent;
}
