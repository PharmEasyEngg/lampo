package com.lampo.device_lab.slave.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getProperty;
import static com.lampo.device_lab.slave.utils.CommonUtilities.isBlank;
import static com.rabbitmq.client.ConnectionFactory.DEFAULT_AMQP_PORT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

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

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

import lombok.NonNull;

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
@Configuration
@PropertySource("classpath:application.properties")
public class ClientConfiguration {

	@Value("${master.host}")
	private String masterHost;

	@Value("${queue.username}")
	private String queueUsername;

	@Value("${queue.password}")
	private String queuePassword;

	private Connection rethinkDBConnection;

	private ConnectionFactory rabbitMQConnectionFactory;

	@Bean
	public RabbitTemplate setRabbitTemplate() {

		rabbitMQConnectionFactory = getConnectionFactory();
		RabbitTemplate template = new RabbitTemplate(rabbitMQConnectionFactory);
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

		connectionFactory.setUsername(queueUsername);
		connectionFactory.setPassword(queuePassword);

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

	@Bean
	public Connection getRethinkDBConnection() {
		rethinkDBConnection = RethinkDB.r.connection().hostname("localhost").port(28015).connect();
		return rethinkDBConnection;
	}

	@PreDestroy
	public void closeConnections() {
		if (rethinkDBConnection != null && rethinkDBConnection.isOpen()) {
			rethinkDBConnection.close();
		}
		if (rabbitMQConnectionFactory != null) {
			rabbitMQConnectionFactory.clearConnectionListeners();
		}
	}

}
