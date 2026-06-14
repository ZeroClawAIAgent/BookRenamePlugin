plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "com.example"
version = "1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
}

tasks {
    compileJava {
        options.release = 25
    }
}
