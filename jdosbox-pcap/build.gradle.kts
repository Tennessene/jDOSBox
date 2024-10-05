plugins {
    id("java")
}

dependencies {
    // jdosbox-pcap-specific dependencies
    implementation(project(":jdosbox"))
    implementation("jnetpcap:jnetpcap:1.4.r1425-1g")
}
