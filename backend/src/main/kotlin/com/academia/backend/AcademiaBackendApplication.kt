package com.academia.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * AFORA Backend Application.
 * Spring Boot modular monolith for Smart Classroom Attendance Management.
 */
@SpringBootApplication
class AcademiaBackendApplication

fun main(args: Array<String>) {
    runApplication<AcademiaBackendApplication>(*args)
}
