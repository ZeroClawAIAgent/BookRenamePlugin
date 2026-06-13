plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
}

group = "com.example"
version = "1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    paperweight.paperDevBundle("26.1.2-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
