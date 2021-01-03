package com.auito.automationtools.config;

import static com.auito.automationtools.utils.StringUtils.getProperty;
import static com.auito.automationtools.utils.StringUtils.isBlank;
import static com.google.common.base.Preconditions.checkArgument;
import static com.rabbitmq.client.ConnectionFactory.DEFAULT_AMQP_PORT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import lombok.NonNull;

@Configuration
@PropertySource("classpath:application.properties")
public class ClientConfiguration {

	@Value("${master.host}")
	private String masterHost;

	@Value("${queue.username}")
	private String queueUsername;

	@Value("${queue.password}")
	private String queuePassword;

	@Bean
	public RabbitTemplate setRabbitTemplate() {

		RabbitTemplate template = new RabbitTemplate(getConnectionFactory());
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}

	private ConnectionFactory getConnectionFactory() {

		if (isBlank(masterHost)) {
			masterHost = getProperty("master.host");
		}
		checkArgument(!isBlank(masterHost),
				"master host is not found. Please pass `--master.host=<ip[:port]>` or `-Dmaster.host=<ip[:port]>` or set as environment variable `master.host=<ip[:port]>`");

		String queueHost = getQueueHost(masterHost);

		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(queueHost, DEFAULT_AMQP_PORT);

		boolean isLocalNetwork = masterHost.equals("127.0.0.1") || masterHost.equals("0.0.0.0")
				|| masterHost.startsWith("192.") || masterHost.startsWith("local");
		if (!isLocalNetwork) {
			if (isBlank(queueUsername)) {
				queueUsername = getProperty("queue.username");
			}
			checkArgument(!isBlank(queueUsername),
					"queue username is not found. Please pass `--queue.username=<username>` or `-Dqueue.username=<username>` or set as environment variable `queue.username=<username>`");
			queueUsername = queueUsername.trim();

			if (isBlank(queuePassword)) {
				queuePassword = getProperty("queue.username");
			}
			checkArgument(!isBlank(queuePassword),
					"queue password is not found. Please pass `--queue.password=<password>` or `-Dqueue.password=<password>` or set as environment variable `queue.password=<password>`");
			queuePassword = queuePassword.trim();

			connectionFactory.setUsername(queueUsername);
			connectionFactory.setPassword(queuePassword);
		}
		return connectionFactory;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	private String getQueueHost(@NonNull String host) {

		if (host.startsWith("http")) {
			String _host = host.replaceAll("http.*://", "");
			int index = _host.indexOf(':');
			return _host.substring(0, index == -1 ? _host.length() : index).replace("/", "");
		}
		int index = host.indexOf(':');
		return host.substring(0, index == -1 ? host.length() : index).replace("/", "");
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
}
