package ru.connector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuditdbTestApplication

fun main(args: Array<String>) {
    runApplication<AuditdbTestApplication>(*args)
}
