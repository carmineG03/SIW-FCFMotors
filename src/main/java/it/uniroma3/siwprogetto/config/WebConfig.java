package it.uniroma3.siwprogetto.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String tempDir = System.getProperty("java.io.tmpdir");
		String uploadPath = "file:" + tempDir + "/uploads/";
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadPath);
	}
}