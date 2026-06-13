plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
}

group = "com.example"
version = "1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
