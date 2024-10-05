plugins {
    id("java")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allprojects {
    group = "com.acclash.jdosbox"
    version = "0.74.31-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://clojars.org/repo/")
        }
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        implementation("org.javassist:javassist:3.29.2-GA")
        testImplementation("junit:junit:4.12")
    }
}
