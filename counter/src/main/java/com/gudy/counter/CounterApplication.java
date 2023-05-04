package com.gudy.counter;

import com.gudy.counter.config.CounterConfig;
import com.gudy.counter.thirdpart.uuid.GudyUuid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CounterApplication {
    @Autowired
    private CounterConfig counterConfig;

    @PostConstruct
    private void init(){
        GudyUuid.getInstance().init(counterConfig.getDataCenterId(),counterConfig.getWorkerId());
    }

    public static void main(String[] args) {
        SpringApplication.run(CounterApplication.class, args);
    }

}
