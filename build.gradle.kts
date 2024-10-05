plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
