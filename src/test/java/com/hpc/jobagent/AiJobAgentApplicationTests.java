package com.hpc.jobagent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class AiJobAgentApplicationTests {

	@Test
	void contextLoads() {
	}

}
