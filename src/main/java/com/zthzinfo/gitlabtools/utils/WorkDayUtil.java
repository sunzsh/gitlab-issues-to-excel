package com.zthzinfo.gitlabtools.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;

import java.util.Date;

public class WorkDayUtil {


	public static Integer getWorkDays(String startDay, String endDay) {
		if (startDay == null || endDay == null) {
			return 0;
		}
		DateTime start = new DateTime(startDay, "yyyy-MM-dd");
		DateTime end = new DateTime(endDay, "yyyy-MM-dd");

		int dayCount = 0;

		while (start.isBeforeOrEquals(end)) {
			if (start.dayOfWeekEnum() == Week.SATURDAY || start.dayOfWeekEnum() == Week.SUNDAY) {
				start = DateUtil.offsetDay(start, 1);
				continue;
			}
			dayCount++;
			start = DateUtil.offsetDay(start, 1);

		}

		return dayCount;

	}

	public static String getStartDate(String dueDate, Long time_estimate) {
		if (dueDate == null || dueDate.trim().length() == 0) {
			return "";
		}
		if (time_estimate == null || time_estimate == 0) {
			return dueDate;
		}
		Date date = DateUtil.parseDate(dueDate);

		int dayCount = (int) (time_estimate / 28800);


		DateTime dateTime = new DateTime(date);
		int i = 1;
		while (i < dayCount) {
			dateTime = DateUtil.offsetDay(dateTime, -1);
			// 跳过周六周日
			if (dateTime.dayOfWeekEnum() == Week.SATURDAY || dateTime.dayOfWeekEnum() == Week.SUNDAY) {
				continue;
			}

			i++;
		}


		return dateTime.toDateStr();
	}
}
