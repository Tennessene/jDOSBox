plugins {
    id("java")
}

// Apply a toolchain to allow any JDK version 8 or newer
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8)) // Default to Java 8
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Allow any JDK version 8 or newer by specifying compatibility
    options.release.set(8) // Set the minimum compatibility level
}

allprojects {
    group = "com.acclash.jdosbox"
    version = "0.74.31"

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
