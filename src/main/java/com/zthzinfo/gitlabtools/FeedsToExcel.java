package com.zthzinfo.gitlabtools;

import com.zthzinfo.gitlabtools.beans.Entry;
import com.zthzinfo.gitlabtools.beans.TimeStats;
import com.zthzinfo.gitlabtools.beans.User;
import com.zthzinfo.gitlabtools.service.IssuesSerivce;
import com.zthzinfo.gitlabtools.service.UserService;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class FeedsToExcel {

	static IssuesSerivce issuesSerivce;
	static UserService userService;


	public static void main(String[] args) {

		// 默认情况下只做导出excel，不做导入
		boolean need_export_excel = true;
		boolean need_update_issues = false;

		if (args != null && args.length >= 1) {
			String baseUrl = args[0];
			issuesSerivce = new IssuesSerivce(baseUrl);
			userService = new UserService(baseUrl);
		}

		String token = null;
		if (args != null && args.length >= 2) {
			token = args[1];
		}

		// 各种查询条件参考：https://docs.gitlab.com/ee/api/issues.html
		String projectId = null;	// 项目id可以再gitlab的项目详情页找到
		if (args != null && args.length >= 3) {
			projectId = args[2];
		}
		if (projectId == null) {
			System.err.println("缺少项目id参数");
			return;
		}

		String xlsxFile = null;
		if (args != null && args.length >= 4) {
			xlsxFile = args[3];
			need_update_issues = true;		// 如果传了第4个参数，说明需要更新issues
			need_export_excel = false;		// 更新issues的情况，就不做导出了
		}

		// 依次为service注入token
		issuesSerivce.setToken(token);
		userService.setToken(token);
		// End of 依次为service注入token


		List<Entry> entries = issuesSerivce.getIssues(projectId);
		if (need_update_issues) {
			toUpdateIssues(projectId, xlsxFile, entries);
		}

		if (entries == null) {
			System.exit(0);
			return;
		}

		if (need_export_excel) {
			issuesSerivce.exportToExcel(entries, getDesktopDir());
			System.exit(0);
		}

	}

	private static void toUpdateIssues(String projectId, String xlsxFile, List<Entry> entries) {
		List<User> users = userService.getUsers();
		List<Entry> inputList = issuesSerivce.fromExcel(xlsxFile, users);
		System.out.println("test");
		int count = 0;
		for (Entry input : inputList) {
			Entry issues = entries.stream().filter(is -> Objects.equals(is.getIid(), input.getIid())).findFirst().orElse(null);
			if (issues == null) {
				continue;
			}

			Map<String, Object> param = new HashMap<>();
			if (!Objects.equals(issues.getDue_date(), input.getDue_date())) {
				param.put("due_date", input.getDue_date());
				System.out.println(String.format("#%s\t[截止日期]\t%s -> %s", input.getIid(), issues.getDue_date(), input.getDue_date()));
			}

			String oldAssignees = issues.getAssignees().stream().map(User::getName).sorted().collect(Collectors.joining(","));
			String inputAssignees = input.getAssignees().stream().map(User::getName).sorted().collect(Collectors.joining(","));
			if (!Objects.equals(oldAssignees, inputAssignees)) {
				if (input.getAssignees().size() == 0) {
					param.put("assignee_ids[]", new ArrayList<>());
					System.out.println(String.format("#%s\t[ 负责人 ]\t%s -> %s",
							input.getIid(),
							issues.getAssignees().stream().map(User::getName).collect(Collectors.joining(",")),
							"空"
							));
				} else {
					List<Long> assignee_ids = input.getAssignees().stream().filter(u -> u.getId() != null).map(User::getId).distinct().collect(Collectors.toList());
					param.put("assignee_ids[]", assignee_ids);
					System.out.println(String.format("#%s\t[ 负责人 ]\t%s -> %s",
							input.getIid(),
							issues.getAssignees().stream().map(User::getName).collect(Collectors.joining(",")),
							input.getAssignees().stream().map(User::getName).collect(Collectors.joining(","))
					));

				}


			}

			if (!Objects.equals(issues.getTime_stats().getHuman_time_estimate(), input.getTime_stats().getHuman_time_estimate())) {
				param.put("estimate", input.getTime_stats().getHuman_time_estimate());
				System.out.println(String.format("#%s\t[预估工时]\t%s -> %s",
						input.getIid(),
						Optional.ofNullable(issues.getTime_stats()).map(TimeStats::getHuman_time_estimate).orElse("空"),
						Optional.ofNullable(input.getTime_stats()).map(TimeStats::getHuman_time_estimate).orElse("空")));
			}


			if (!Objects.equals(issues.getPriority(), input.getPriority())) {
				String labels = input.getFullLables().stream().collect(Collectors.joining(","));
				param.put("labels", labels);
				System.out.println(String.format("#%s\t[ 优先级 ]\t%s -> %s", input.getIid(),
						Optional.ofNullable(issues).map(Entry::getPriority).orElse("空"),
						Optional.ofNullable(input).map(Entry::getPriority).orElse("空")));
			}

			if (param.size() == 0) {
				continue;
			}


			// 更新需要更新的任务
			issuesSerivce.modifyIssues(projectId, input.getIid(), param);
			count++;
		}
		if (count == 0) {
			System.out.println("没有要同步的任务！");
		} else {
			System.out.println("成功同步了" + count + "个任务！");
		}
	}

	private static String getDesktopDir() {
		File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
		String output_dir = desktopDir.getAbsolutePath();

		if (output_dir.length() > 7 && !output_dir.substring(output_dir.length() - 7).equalsIgnoreCase("desktop")) {
			output_dir = output_dir + "/Desktop";
		}

		return output_dir;
	}


}
