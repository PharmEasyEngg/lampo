package com.auito.automationtools.repos;

import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.auito.automationtools.model.Photo;

public interface IPhoneImageRepository extends MongoRepository<Photo, String> {

	@Query("{'name': {$regex: ?0, $options: 'i'}})")
	public Collection<Photo> findByName(String name);
}