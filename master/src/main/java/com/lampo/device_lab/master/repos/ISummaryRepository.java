package com.lampo.device_lab.master.repos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lampo.device_lab.master.model.Summary;
import com.lampo.device_lab.master.model.SummaryInfo;

import lombok.NonNull;

/**
 * MIT License <br/>
 * <br/>
 * 
 * Copyright (c) [2022] [PharmEasyEngg] <br/>
 * <br/>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, prepare derivatives of the work, and to permit
 * persons to whom the Software is furnished to do so, subject to the following
 * conditions: <br/>
 * <br/>
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <br/>
 * <br/>
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. <br/>
 * <br/>
 * 
 * 
 * This software uses open-source dependencies that are listed under the
 * licenses - {@link <a href="https://www.eclipse.org/legal/epl-2.0/">Eclipse
 * Public License v2.0</a>},
 * {@link <a href="https://www.apache.org/licenses/LICENSE-2.0.html">Apache
 * License 2.0</a>}, {@link <a href=
 * "https://www.mongodb.com/licensing/server-side-public-license">Server Side
 * Public License</a>},
 * {@link <a href="https://www.mozilla.org/en-US/MPL/2.0/">Mozilla Public
 * License 2.0</a>} and {@link <a href="https://opensource.org/licenses/MIT">MIT
 * License</a>}. Please go through the description of the licenses to understand
 * the usage agreement. <br/>
 * <br/>
 * 
 * By using the license, you agree that you have read, understood and agree to
 * be bound by, including without any limitation by these terms and that the
 * entire risk as to the quality and performance of the software is with you.
 *
 */
public interface ISummaryRepository extends MongoRepository<Summary, String> {

	Summary findByDate(String date);

	default List<Summary> findBetweenDates(String team, @NonNull String start, @NonNull String end) {
		return findBetweenDates(LocalDate.parse(start, DateTimeFormatter.ofPattern("dd-MM-yyyy")),
				LocalDate.parse(end, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
	}

	default List<Summary> findBetweenDates(@NonNull LocalDate start, @NonNull LocalDate end) {
		return findAll().stream().filter(e -> {
			LocalDate localDate = LocalDate.parse(e.getDate(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
			return localDate.compareTo(start) >= 0 && localDate.compareTo(end) <= 0;
		}).collect(Collectors.toList());
	}

	default void updateSessionCount(String team) {

		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

		Summary summary = findByDate(date);
		if (summary == null) {
			summary = new Summary();
			summary.setDate(date);
		}
		Map<String, SummaryInfo> map = summary.getInfo();
		SummaryInfo info = map.getOrDefault(team, new SummaryInfo());
		info.setNumberOfSessions(info.getNumberOfSessions() + 1);
		map.put(team, info);
		summary.setInfo(map);
		save(summary);
	}

	default void updateSessionDuration(String team, long duration) {

		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

		Summary summary = findByDate(date);
		if (summary == null) {
			summary = new Summary();
			summary.setDate(date);
		}
		Map<String, SummaryInfo> map = summary.getInfo();
		SummaryInfo info = map.getOrDefault(team, new SummaryInfo());
		info.setTotalDuration(info.getTotalDuration() + duration);
		map.put(team, info);
		summary.setInfo(map);
		save(summary);
	}
}