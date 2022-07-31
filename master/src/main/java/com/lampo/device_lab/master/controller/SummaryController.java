package com.lampo.device_lab.master.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lampo.device_lab.master.model.Summary;
import com.lampo.device_lab.master.model.SummaryInfo;
import com.lampo.device_lab.master.repos.ISummaryRepository;

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
@RestController
@RequestMapping("/summary")
public class SummaryController {

	private static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	@Autowired
	private ISummaryRepository repo;

	@GetMapping("/find")
	public Summary findAll(@RequestParam String date) {
		return repo.findByDate(date);
	}

	@GetMapping("/find_between")
	public ResponseEntity<?> findAllBetween(@RequestParam String start, @RequestParam String end) {
		LocalDate startDate = LocalDate.parse(start, PATTERN);
		LocalDate endDate = LocalDate.parse(end, PATTERN);
		if (startDate.isAfter(endDate)) {
			return ResponseEntity.badRequest()
					.body(String.format("start date '%s' is after end date '%s'", start, end));
		}
		return ResponseEntity.ok(repo.findBetweenDates(startDate, endDate));
	}

	@GetMapping("/total_between")
	public ResponseEntity<?> totalBetween(@RequestParam String start, @RequestParam String end) {

		LocalDate startDate = LocalDate.parse(start, PATTERN);
		LocalDate endDate = LocalDate.parse(end, PATTERN);

		if (startDate.isAfter(endDate)) {
			return ResponseEntity.badRequest()
					.body(String.format("start date '%s' is after end date '%s'", start, end));
		}
		List<Summary> list = repo.findBetweenDates(startDate, endDate);

		Map<String, SummaryInfo> map = new HashMap<>();
		for (Summary summary : list) {
			Map<String, SummaryInfo> info = summary.getInfo();
			info.forEach((k, v) -> {
				SummaryInfo val = map.getOrDefault(k, new SummaryInfo());
				val.setNumberOfSessions(val.getNumberOfSessions() + v.getNumberOfSessions());
				val.setTotalDuration(val.getTotalDuration() + v.getTotalDuration());
				map.put(k, val);
			});
		}

		return ResponseEntity.ok(map);
	}
}
