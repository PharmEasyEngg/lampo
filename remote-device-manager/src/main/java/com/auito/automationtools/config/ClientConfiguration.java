package com.auito.automationtools.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;

@Configuration
public class ClientConfiguration {

	public static final String DEVICE_UPDATE_QUEUE_NAME = "devices";

	@Value("${spring.data.mongodb.url}")
	private String mongoUrl;

	private AmqpAdmin amqAdmin;

	@Autowired
	public void setAmqAdmin(ConnectionFactory factory) {
		this.amqAdmin = new RabbitAdmin(factory);
	}

	@Bean
	public RestTemplate getRestTemplate() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
		messageConverters.add(converter);
		messageConverters.add(new FormHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());
		return new RestTemplate(messageConverters);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public CodecRegistry getCodecRegistry() {
		return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
	}

	@Bean("mongoTemplate")
	public MongoTemplate getMongoTemplate() {
		MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(mongoDbFactory()),
				new MongoMappingContext());
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		return new MongoTemplate(mongoDbFactory(), converter);
	}

	@Bean
	public MongoDbFactory mongoDbFactory() {
		return new org.springframework.data.mongodb.core.SimpleMongoClientDbFactory(mongoUrl);
	}

	@Bean
	public com.mongodb.MongoClient mongoClient() {
		MongoClientOptions options = MongoClientOptions.builder().codecRegistry(getCodecRegistry()).build();
		MongoClientURI uri = new MongoClientURI(mongoUrl);
		return new MongoClient(uri.getHosts().get(0), options);
	}

	@Bean(name = "threadPoolTaskExecutor")
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("remote-master-");
		executor.setAllowCoreThreadTimeOut(true);
		executor.setKeepAliveSeconds(10);
		executor.initialize();
		return executor;
	}

	@Bean
	public RabbitTemplate rabbitTemplate() {
		RabbitTemplate template = new RabbitTemplate();
		template.setMessageConverter(new Jackson2JsonMessageConverter());
		return template;
	}

	@PostConstruct
	public void init() {
		QueueInformation queueInfo = amqAdmin.getQueueInfo(DEVICE_UPDATE_QUEUE_NAME);
		if (queueInfo == null) {
			amqAdmin.declareQueue(new Queue(DEVICE_UPDATE_QUEUE_NAME));
		}
	}

}
