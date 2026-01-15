import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.9.25"
    id("com.google.protobuf") version "0.9.6"
    id("java-library")
}

val grpcVersion = "1.78.0"
val grpcKotlinVersion = "1.5.0"
val protobufVersion = "3.25.8"

repositories {
    mavenCentral()
}

dependencies {
    // Protobuf (Kotlin)
    api("com.google.protobuf:protobuf-kotlin:$protobufVersion")

    // gRPC
    api("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-netty-shaded:$grpcVersion")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Annotation API (für Generated Code)
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        proto {
            srcDir("src/proto")
        }

        kotlin {
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/kotlin"))
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/grpckt"))
        }

        java {
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/java"))
            srcDir(layout.buildDirectory.dir("generated/source/proto/main/grpc"))
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact =
                "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Kotlin Message-Klassen
                create("kotlin")
            }
            task.plugins {
                // gRPC Java (Pflicht)
                id("grpc")
                // gRPC Kotlin (Coroutines)
                id("grpckt")
            }
        }
    }
}
