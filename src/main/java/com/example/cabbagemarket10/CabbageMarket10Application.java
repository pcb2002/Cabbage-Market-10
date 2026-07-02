package com.example.cabbagemarket10;

import com.example.cabbagemarket10.global.common.config.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class})
public class CabbageMarket10Application {

    public static void main(String[] args) {
        SpringApplication.run(CabbageMarket10Application.class, args);
    }

}
