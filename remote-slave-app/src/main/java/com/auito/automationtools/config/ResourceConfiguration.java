package com.auito.automationtools.config;

import static com.auito.automationtools.utils.AppiumLocalService.Builder.LOG_DIRECTORY;

import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class ResourceConfiguration implements WebMvcConfigurer {

	public static final File RESOURCE_CREATION_DIR = new File(LOG_DIRECTORY);

	static {
		if (!RESOURCE_CREATION_DIR.exists()) {
			RESOURCE_CREATION_DIR.mkdirs();
		}
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		if (!registry.hasMappingForPattern("/appium/logs/**")) {
			registry.addResourceHandler("/appium/logs/**")
					.addResourceLocations(RESOURCE_CREATION_DIR.toURI().toString());
		}
	}

}