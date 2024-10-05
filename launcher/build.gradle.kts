plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

dependencies {
    implementation(project(":jdosbox"))
    implementation(project(":jdosbox-pcap"))
    implementation(project(":jdosbox-win"))
}

tasks {
    shadowJar {
        archiveClassifier.set("") // Ensures the generated JAR is not appended with '-all'
        manifest {
            attributes(
                "Main-Class" to "jdos.gui.MainFrame"
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
