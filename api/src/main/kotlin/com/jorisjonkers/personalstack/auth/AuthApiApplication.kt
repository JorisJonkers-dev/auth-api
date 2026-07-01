package com.jorisjonkers.personalstack.auth

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication(scanBasePackages = ["com.jorisjonkers.personalstack"])
class AuthApiApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AuthApiApplication::class.java), args)
}
