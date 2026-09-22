package com.afora.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * AFORA Backend Application.
 * Spring Boot modular monolith for Smart Classroom Attendance Management.
 */
@SpringBootApplication
class AforaBackendApplication

fun main(args: Array<String>) {
    runApplication<AforaBackendApplication>(*args)
}
