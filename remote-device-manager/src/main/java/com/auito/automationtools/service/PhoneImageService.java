package com.auito.automationtools.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auito.automationtools.model.Photo;
import com.auito.automationtools.repos.IPhoneImageRepository;

import lombok.NonNull;

@Service
public class PhoneImageService {

	@Autowired
	private IPhoneImageRepository imageRepo;

	public Collection<Photo> getPhotoByName(@NonNull String name) {
		Collection<Photo> photos = getPhotoByName(name, false);
		return !photos.isEmpty() ? photos : getPhotoByName(name.split("\\s+")[0], true);
	}

	private Collection<Photo> getPhotoByName(@NonNull String name, boolean checkDefault) {
		Collection<Photo> photos = imageRepo.findByName(name);
		if (photos.isEmpty() && checkDefault) {
			return getPhotoByName("default");
		}
		return photos;
	}

	public Photo addPhoto(@NonNull File file) throws IOException {
		String name = file.getName().substring(0, file.getName().lastIndexOf('.')).replaceAll("[\\(\\)]", " ");
		Collection<Photo> photo = getPhotoByName(name, false);
		Photo _photo = null;
		if (photo.isEmpty()) {
			_photo = new Photo();
			_photo.setName(name);
			_photo.setImage(new Binary(BsonBinarySubType.BINARY, Files.readAllBytes(file.toPath())));
		} else {
			_photo = photo.stream().findFirst().get();
		}
		return addPhoto(_photo);
	}

	public Photo addPhoto(@NonNull Photo photo) {
		return imageRepo.save(photo);
	}
}