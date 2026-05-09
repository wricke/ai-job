package com.hpc.jobagent;

import com.hpc.jobagent.config.AgentProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.hpc.jobagent.mapper")
@EnableConfigurationProperties(AgentProperties.class)
public class AiJobAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiJobAgentApplication.class, args);
	}

}
