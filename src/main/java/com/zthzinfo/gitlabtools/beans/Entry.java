package com.zthzinfo.gitlabtools.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Entry {

	private Integer iid;
	private String web_url;
	private String title;
	private String start_date;
	private String due_date;
	private List<String> labels;
	private List<User> assignees;
	private User author;
	private TimeStats time_stats;
	private Milestone milestone;

	private String priority;

	public List<String> getFullLables() {
		List<String> result = new ArrayList<>();
		if (this.labels != null) {
			result.addAll(this.labels);
		}
		if (this.priority != null) {
			result.add("优先级：" + this.priority);
		}
		return result;
	}


	public void initPriority() {
		if (this.labels == null || this.labels.size() == 0) {
			return;
		}
		Predicate<String> priorityComparetor = label -> label.startsWith("优先级：");


		String priorityTag = labels.stream().filter(priorityComparetor).sorted().findFirst().orElse(null);
		if (priorityTag == null) {
			return;
		}
		String priority = priorityTag.substring(4);
		this.priority = priority;


		labels.removeIf(priorityComparetor);
	}

}
