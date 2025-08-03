# Polocloud v3

[![Download](https://img.shields.io/github/downloads/HttpMarco/Polocloud/total?style=for-the-badge&logo=github&color=2ea043)](https://github.com/HttpMarco/polocloud/releases)
[![Discord](https://img.shields.io/discord/1278460874679386244?label=Community&style=for-the-badge&logo=discord&color=7289da)](https://discord.gg/WGzUcuJax7)


### 🧩 PoloCloud Kotlin SDK

Bring cloud-native capabilities to your Kotlin applications with the **PoloCloud SDK** — designed for performance, simplicity, and seamless integration.

#### 📦 Maven Snapshot Repository

To get started, add the PoloCloud snapshot repository to your `pom.xml` or `build.gradle.kts`:

##### Maven
```xml
<repositories>
    <repository>
        <id>polocloud-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.httpmarco.polocloud</groupId>
        <artifactId>sdk-kotlin</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

##### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven {
        name = "polocloud-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    implementation("dev.httpmarco.polocloud:sdk-kotlin:3.0.0-SNAPSHOT")
}
```

> 🧪 This version is a snapshot release (`3.0.0-SNAPSHOT`) — ideal for testing the latest features. Expect frequent updates!


### 🌐 Translations

This project is being translated into multiple languages with the help of [Crowdin](https://crowdin.com/project/polocloud).  
You can contribute by helping us localize it!

[![Crowdin](https://badges.crowdin.net/polocloud/localized.svg)](https://crowdin.com/project/polocloud)

