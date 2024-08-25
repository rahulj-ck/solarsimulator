
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    id("org.springframework.boot") version "3.2.8"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.openapi.generator") version "7.0.0"
    id("nu.studer.jooq") version "9.0"
    id("org.flywaydb.flyway") version "9.21.0"
    id("org.jmailen.kotlinter") version "4.4.1"
}

group = "com.fastned"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlinter {
    failBuildWhenCannotAutoFormat = false
    ignoreFailures = false
    reporters = arrayOf("checkstyle", "plain")
}

tasks.withType<LintTask> {
    this.source = this.source.minus(fileTree("build/generated")).asFileTree
}

tasks.withType<FormatTask> {
    this.source = this.source.minus(fileTree("build/generated")).asFileTree
}

repositories {
    mavenCentral()
}

flyway {
    url = "jdbc:postgresql://localhost:5432/admin"
    user = "admin"
    password = "admin"
    schemas = arrayOf("public")
    locations = arrayOf("classpath:db/migration")
}

tasks.register<Exec>("checkPostgresContainer") {
    val result = ByteArrayOutputStream()

    commandLine("docker", "ps", "-q", "-f", "name=postgres-db")
    isIgnoreExitValue = true
    standardOutput = result

    doLast {
        println("Checking if PostgreSQL container is running.")
        project.ext.set("postgresRunning", result.toString().trim().isNotEmpty())
    }
}

tasks.register("startPostgresContainer") {
    dependsOn("checkPostgresContainer")

    doFirst {
        val postgresRunning = project.ext.get("postgresRunning") as Boolean
        if (postgresRunning) {
            println("PostgreSQL container already running.")
        } else {
            println("Starting PostgreSQL container.")
            exec {
                commandLine("docker-compose", "up", "-d")
            }
        }
    }

    doLast {
        println("PostgreSQL container started.")
    }
}

jooq {
    configurations {
        create("main") {

            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/admin"
                    user = "admin"
                    password = "admin"
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        generate.apply {
                            isDeprecated = false
                            isRecords = true
                            isImmutablePojos = true
                            isFluentSetters = true
                        }
                        strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.11")

    implementation("io.swagger.core.v3:swagger-core:2.2.11")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.14")

    jooqGenerator("org.jooq:jooq:3.19.1")
    jooqGenerator("org.jooq:jooq-meta:3.19.1")
    jooqGenerator("org.jooq:jooq-codegen:3.19.1")

    jooqGenerator("org.postgresql:postgresql:42.5.4")
    implementation("org.postgresql:postgresql:42.5.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/src/main/resources/specs/specs.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("com.fastned.solar.simulator.api")
    modelPackage.set("com.fastned.solar.simulator.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "delegatePattern" to "true",
            "useSpringBoot3" to "true",
            "withJavaxValidation" to "false",
            "delegatePattern" to "false",
            "exceptionHandler" to "false",
            "skipDefaultInterface" to "true",
            "generateSupportingFiles" to "false"
        )
    )
}

tasks.openApiGenerate {
    doLast {
        delete(
            "$buildDir/generated/.openapi-generator",
            "$buildDir/generated/src/main/kotlin/com/fastned/solar/simulator/api/ApiUtil.kt"
        )
    }
}

tasks.named("generateJooq") {
    dependsOn("openApiGenerate", "flywayMigrate")
}

tasks.named("flywayMigrate") {
    dependsOn("startPostgresContainer")
}

tasks.named("build") {
    dependsOn("openApiGenerate", "generateJooq", "startPostgresContainer")
}

sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated/src/main/kotlin")
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
