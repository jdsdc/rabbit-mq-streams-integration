package dev.coffe.springintegration.rabbitmqintegration.rabbitmqstreamsintegration;

import com.rabbitmq.stream.Environment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.RabbitStream;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;

import java.util.Map;

@SpringBootApplication
public class RabbitMqStreamsIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(RabbitMqStreamsIntegrationApplication.class, args);
	}

    @Bean
	InitializingBean initializingBean(RabbitProperties rabbitProperties, Environment environment) {
		return () -> environment.streamCreator().stream(rabbitProperties.getStream().getName()).create();
	}

	static Map<String, String > payload(String name) {
		return Map.of("message", name);
	}



	@Bean
	DirectChannel streamMessageChannel() {
		return MessageChannels.direct().getObject();
	}

	@Bean
	ApplicationRunner producer() {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				var message = MessageBuilder.withPayload(payload("Hello streams!")).build();
				streamMessageChannel().send(message);
			}
		};
	}

	@Bean
	IntegrationFlow outbound(RabbitStreamTemplate template) {
		return IntegrationFlow
				.from(this.streamMessageChannel())
				.handle(RabbitStream.outboundStreamAdapter(template))
				.get();
	}

	@Bean
	IntegrationFlow inbound(Environment environment) {
		return IntegrationFlow
				.from(RabbitStream.inboundAdapter(environment).streamName("stream-requests"))
				.handle((GenericHandler<Map<String, String>>) (payload, headers) -> {
					System.out.println(payload);
					for (var h : headers.keySet()) {
						System.out.println("Stream Incoming: " + h + " = " + headers.get(h));
					}
					return null;
				})
				.get();
	}

}
