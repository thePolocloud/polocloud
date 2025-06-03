import com.google.protobuf.gradle.id

val protobufProtoc by configurations.creating

plugins {
    id("java")
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    implementation(libs.bundles.proto)

    protobufProtoc("com.google.protobuf:protoc:3.25.7")
    //implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:3.25.7"
        }
        plugins {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:1.73.0"
            }
        }
        generateProtoTasks {
            ofSourceSet("main").forEach {
                it.plugins {
                    id("grpc") {}
                }
            }
        }
    }
}