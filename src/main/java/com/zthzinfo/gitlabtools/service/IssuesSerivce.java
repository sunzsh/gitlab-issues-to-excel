package com.zthzinfo.gitlabtools.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.cell.FormulaCellValue;
import com.alibaba.fastjson.JSON;
import com.zthzinfo.gitlabtools.HttpUtil;
import com.zthzinfo.gitlabtools.beans.Entry;
import com.zthzinfo.gitlabtools.beans.Milestone;
import com.zthzinfo.gitlabtools.beans.TimeStats;
import com.zthzinfo.gitlabtools.beans.User;
import com.zthzinfo.gitlabtools.utils.WorkDayUtil;
import lombok.*;
import org.apache.poi.ss.usermodel.Cell;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IssuesSerivce {

	public IssuesSerivce(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	private String baseUrl;

	private String token;


	public List<Entry> getIssues(String projectId) {
		int pageSize = 100;
		List<Entry> entries = new ArrayList<>();

		List<Entry> everyEntries = null;
		int page = 1;
		do {
			String url = String.format(baseUrl + "/projects/%s/issues?state=opened&scope=all&per_page=" + pageSize  + "&page=" + page, projectId);
			System.out.println("拉取最新数据...");
			HttpResponse response = HttpUtil.get(url, token);
			String jsonBody = response.body();
			everyEntries = JSON.parseArray(jsonBody, Entry.class);
			if (everyEntries != null && everyEntries.size() > 0) {
				entries.addAll(everyEntries);
			}
			page++;
		} while (everyEntries.size() >= pageSize);



		entries.stream().filter(entry -> entry.getDue_date() == null).forEach(entry -> entry.setDue_date(""));
		entries.sort(Comparator.comparing(Entry::getDue_date));

		entries.forEach(entry -> entry.initPriority());

		// 工时默认1d
		entries.stream().filter(entry -> entry.getTime_stats() == null).forEach(entry -> entry.setTime_stats(new TimeStats(null, null, "1d", null)));
		entries.stream().filter(entry -> entry.getTime_stats().getHuman_time_estimate() == null).forEach(entry -> entry.getTime_stats().setHuman_time_estimate("1d"));
		return entries;
	}

	public void modifyIssues(String projectId, Integer iid, Map<String, Object> param) {
		if (param == null || param.size() == 0) {
			return;
		}
		String url = String.format(baseUrl + "/projects/%s/issues/%s", projectId, iid);

		StringBuffer paramStr = new StringBuffer();
		for (String key : param.keySet()) {
			if (Objects.equals(key, "estimate")) {
				continue;
			}

			if (Objects.equals(key, "assignee_ids[]")) {
				List<Long> list = (List<Long>) param.get("assignee_ids[]");
				if (list != null) {
					if (list.size() == 0) {
						paramStr.append("&assignee_ids[]=0");
					} else {
						for (Long assignee_id : list) {
							paramStr.append(String.format("&assignee_ids[]=%s", assignee_id));
						}
					}


				}
				continue;
			}


			try {
				paramStr.append(String.format("&%s=%s", key, URLEncoder.encode((String)param.get(key), "utf-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		if (paramStr.length() > 0) {

			HttpResponse response = HttpUtil.put(url, paramStr.substring(1), token);

			String jsonBody = response.body();

			Entry newEntry = JSON.parseObject(jsonBody, Entry.class);
			if (newEntry.getIid() != null) {
				// 成功
			}
		}

		String estimate = (String)param.get("estimate");
		if (estimate != null) {
			String estimateUrl = String.format(baseUrl + "/projects/%s/issues/%s/time_estimate", projectId, iid);

			HttpResponse estimateResponse = HttpUtil.post(estimateUrl, String.format("duration=%s", estimate), token);
			String estimateJsonBody = estimateResponse.body();
			TimeStats timeStats = JSON.parseObject(estimateJsonBody, TimeStats.class);
			if (timeStats.getHuman_time_estimate() != null) {
				// 成功
			}
		}
		System.out.println();
	}

	private static String getDate(Map<String, Object> row, String key) {
		Object o = row.get(key);
		if (o instanceof DateTime) {
			return ((DateTime)o).toDateStr();
		}
		return (String)o;
	}

	public List<Entry> fromExcel(String path, List<User> users) {
		ExcelReader reader = ExcelUtil.getReader(path);
		List<Map<String, Object>> readAll = reader.readAll();


		List<Entry> result = new ArrayList<>();
		int rowIndex = 1;
		for (Map<String, Object> row : readAll) {
			Cell issues = reader.getCell(0, rowIndex);
			String cellFormula = issues.getCellFormula();
			if (issues == null || cellFormula == null) {
				continue;
			}

			String iid = cellFormula.replaceAll(".*,\"\\#", "").replaceAll("\\\"\\)$", "");
			if (iid == null) {
				continue;
			}

			String web_url = cellFormula.replaceAll("^[^\\\"]*\\\"", "").replaceAll("\\\".*$", "");

			String title = ((String) row.get("功能描述"));
			String start_date = getDate(row, "计划开始时间");
			String end_date = getDate(row, "计划完成时间");
			String priority = (String) row.get("优先级");
			priority = Objects.equals("", priority.trim()) ? null : priority;
			String assignees = (String) row.get("负责人");
			List<User> assigneeUsers = new ArrayList<>();
			if (assignees != null) {
				String[] split = assignees.split("\\s*\\,\\s*");
				for (String userName : split) {
					User user = new User();
					user.setName(userName);
					if (users != null) {
						User fullUserInfo = users.stream().filter(u -> Objects.equals(u.getName(), userName)).findFirst().orElse(null);
						if (fullUserInfo != null) {
							user.setId(fullUserInfo.getId());
						}
					}
					assigneeUsers.add(user);
				}
			}

			List<String> labels = new ArrayList<>();
			Iterator<String> keyIterator = row.keySet().iterator();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				if (key.equals("issues") || key.equals("功能描述")) {
					continue;
				}

				String value = Optional.ofNullable(row.get(key)).map(Object::toString).orElse(null);
				if (Objects.equals(value, "是")) {
					labels.add(key);
					continue;
				}
				if (value != null && value.trim().length() > 0) {
					labels.add(key + "：" + value);
					continue;
				}

				if (key.equals("计划开始时间")) {
					break;
				}
			}

			Entry entry = new Entry();
			entry.setDue_date(end_date);
			entry.setAssignees(assigneeUsers);
			entry.setIid(Integer.parseInt(iid));
			entry.setLabels(labels);
			entry.setPriority(priority);
			entry.setTitle(title);
			entry.setWeb_url(web_url);

			TimeStats timeStats = new TimeStats();
			if (StrUtil.isNotBlank(end_date)) {
				if (StrUtil.isBlank(start_date)) {
					start_date = end_date;
				}
				Integer days = WorkDayUtil.getWorkDays(start_date, end_date);
				timeStats.setHuman_time_estimate(days + "d");
			} else {
				timeStats.setHuman_time_estimate("1d");
			}
			entry.setTime_stats(timeStats);

			result.add(entry);
			rowIndex++;
		}

		return result;

	}

	public void exportToExcel(List<Entry> entries, String outputDir) {

		System.out.println("开始解析生成excel...");
		Set<String> sumLabels = new LinkedHashSet<>();
//		entries.forEach(entry -> sumLabels.addAll(entry.getLabels()));
		entries.forEach(entry -> sumLabels.addAll(entry.getLabels().stream().map(label -> label.replaceAll("：.*", "")).distinct().collect(Collectors.toList())));
		List<Map<String, Object>> data = new ArrayList<>();
		int dueDateNullCount = 0;
		List<Entry> finalEportList = new ArrayList<>();
		for (Entry entry : entries) {
			String fuzeren = entry.getAssignees().stream().map(User::getName).collect(Collectors.joining(","));
			fuzeren = fuzeren == null || fuzeren.trim().length() == 0 ? null : fuzeren;
			if (entry.getDue_date() == null || entry.getDue_date().trim().length() == 0) {
				String msg = String.format("[截止时间为空] %s：%s\t%s",
						fuzeren,
						entry.getWeb_url(),
						entry.getTitle()
				);
				System.out.println(msg);
				dueDateNullCount++;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("issues", entry.getIid() + "");
			row.put("功能描述", entry.getTitle());
			sumLabels.forEach(label -> {
				if (entry.getLabels().contains(label)) {
					row.put(label, "是");
					return;
				}
				String value = entry.getLabels().stream().filter(l -> l.startsWith(label + "：")).map(l -> l.replaceAll("^.*：", "")).collect(Collectors.joining("、"));

				row.put(label, value);
			});
			row.put("计划开始时间", WorkDayUtil.getStartDate(entry.getDue_date(), entry.getTime_stats().getTime_estimate()));
			row.put("计划完成时间", entry.getDue_date());
			row.put("里程碑", Optional.ofNullable(entry.getMilestone()).map(Milestone::getTitle).orElse(""));
			row.put("优先级", entry.getPriority());
			row.put("发现者", entry.getAuthor().getName());
			row.put("负责人", fuzeren);

			data.add(row);
			finalEportList.add(entry);
		}
		if (data.size() == 0) {
			System.err.println("没有要生成的任务");
			return;
		}

		String fileName = outputDir + "/issues_" + DateUtil.format(new Date(), "yyyyMMdd") + ".xlsx";
		ExcelWriter writer = ExcelUtil.getWriter(fileName);

		int rowHeight = 30;
		/* 设置行高、第一列超链接 */
		boolean showHead = true;
		writer.write(data, showHead);
		if (showHead) {
			writer.setRowHeight(0, rowHeight);
		}
		for (int i = 0; i < data.size(); i++) {
			Entry entry = finalEportList.get(i);
			int rowIndex = showHead ? i + 1 : i;

			FormulaCellValue cellValue = new FormulaCellValue("HYPERLINK(\"" + entry.getWeb_url() + "\",\"#" + entry.getIid() + "\")");

			writer.writeCellValue(0, rowIndex, cellValue);
			writer.setRowHeight(i + 1, rowHeight);
		}
		/* End of 设置行高、第一列超链接 */


		/* 设置自适应宽度 */
		writer.autoSizeColumn(1);
		writer.autoSizeColumn(2);
		for (int i = 0; i < sumLabels.size(); i++) {
			writer.autoSizeColumn(3 + i);
		}
		writer.setColumnWidth(2 + sumLabels.size(), 15);
		writer.setColumnWidth(2 + sumLabels.size() + 1, 15);
		/* End of 设置自适应宽度 */

		writer.close();

		System.out.println(String.format("成功生成 %s个， %s个截止时间为空的任务 ：%s", data.size() + "", dueDateNullCount + "", fileName));

	}

}
