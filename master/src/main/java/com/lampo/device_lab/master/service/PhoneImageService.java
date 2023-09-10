package com.lampo.device_lab.master.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.lampo.device_lab.master.model.Photo;
import com.lampo.device_lab.master.repos.IPhoneImageRepository;

import lombok.NonNull;
import lombok.SneakyThrows;

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
@Service
public class PhoneImageService {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private IPhoneImageRepository imageRepo;

	private List<String> phoneImages = new ArrayList<>();

	@PostConstruct
	@SneakyThrows
	private void uploadDefaultImages() {
		Arrays.stream(context.getResources("classpath:static/images/phones/*.png"))
                .forEach(e -> {
                    File file = new File(e.getFilename());
                    try (InputStream stream = e.getInputStream(); OutputStream out = new FileOutputStream(file)) {
                        FileCopyUtils.copy(stream, out);
                        addPhoto(file);
                        file.delete();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    phoneImages.add(e.getFilename().replace(".png", ""));
                });

	}

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

	public Collection<String> findByName(@NonNull String name) {
		return phoneImages.stream().filter(e -> e.toLowerCase().contains(name.trim().toLowerCase()))
				.collect(Collectors.toList());
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