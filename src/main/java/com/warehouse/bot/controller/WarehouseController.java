package com.warehouse.bot.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WarehouseController
{

    @Bean
    public RestTemplate restTemplate()
    {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        
        // Correct timeout configuration for httpclient5
        requestFactory.setConnectTimeout(5000); // 5 seconds connection timeout
        requestFactory.setConnectionRequestTimeout(5000); // 5 seconds request timeout
        
        return new RestTemplate(requestFactory);
    }
}