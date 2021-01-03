package com.auito.automationtools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AllocatedTo {

	private String ip;
	private String user;
	private String jenkinsJobLink;
}
