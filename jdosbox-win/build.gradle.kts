plugins {
    id("java")
}

dependencies {
    // jdosbox-win-specific dependencies
    implementation(project(":jdosbox"))
    implementation("org.javassist:javassist:3.29.2-GA")
    testImplementation("junit:junit:4.12")
}

// Run tests only on Windows
tasks.withType<Test> {
    if (!System.getProperty("os.name").startsWith("Windows")) {
        enabled = false
    }
}
