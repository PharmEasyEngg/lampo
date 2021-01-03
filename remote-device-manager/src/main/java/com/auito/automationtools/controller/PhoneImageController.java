package com.auito.automationtools.controller;

import java.io.IOException;
import java.util.Collection;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auito.automationtools.model.Photo;
import com.auito.automationtools.service.PhoneImageService;

@RestController
@RequestMapping("/photos")
public class PhoneImageController {

	@Autowired
	private PhoneImageService service;

	@PostMapping("/add")
	public String addPhoto(@RequestParam("name") String name, @RequestParam("file") MultipartFile file)
			throws IOException {

		Collection<Photo> _photo = getPhotoByName(name);
		Photo photo = null;
		if (_photo.isEmpty()) {
			photo = new Photo();
			photo.setName(name);
			photo.setImage(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
			photo = service.addPhoto(photo);
		} else {
			photo = _photo.stream().findAny().get();
		}
		return photo.getId();
	}

	@GetMapping
	public Collection<Photo> getPhotoByName(@RequestParam String name) {
		return service.getPhotoByName(name);
	}
}
