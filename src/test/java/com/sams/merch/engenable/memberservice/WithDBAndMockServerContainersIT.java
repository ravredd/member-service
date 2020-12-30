package com.sams.merch.engenable.memberservice;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class WithDBAndMockServerContainersIT {

    @Container
    public static JdbcDatabaseContainer MSSQLSERVER = new MSSQLServerContainer(
            DockerImageName.parse("azcr.docker.prod.walmart.com/mssql/server:2017-CU12")
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")).acceptLicense()
                            .withInitScript("init.sql");


    @DynamicPropertySource
    static void setConfigs(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MSSQLSERVER::getJdbcUrl);
        registry.add("spring.datasource.username", MSSQLSERVER::getUsername);
        registry.add("spring.datasource.password", MSSQLSERVER::getPassword);
    }
}