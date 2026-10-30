package com.pcpart.assembler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pcpart.assembler.mapper")
public class PcpartAssemblerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PcpartAssemblerApplication.class, args);
    }
}
