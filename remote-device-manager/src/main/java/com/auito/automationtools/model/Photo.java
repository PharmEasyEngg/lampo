package com.auito.automationtools.model;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "phone-images")
public class Photo {

	@Id
	private String id;

	private String name;

	private Binary image;
}