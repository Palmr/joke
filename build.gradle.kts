plugins {
    id("java-library")
    id("maven-publish")
    id("com.diffplug.spotless") version "7.0.4"
    id("me.champeau.jmh") version "0.7.2"
}

group = "uk.co.palmr"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    create("examples") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("joke")
                description.set("Java KDB client library")
                url.set("https://github.com/Palmr/joke")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("Nick Palmer")
                        email.set("nick@palmr.co.uk")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Palmr/joke.git")
                    developerConnection.set("scm:git:ssh://github.com/Palmr/joke.git")
                    url.set("https://github.com/Palmr/joke")
                }
            }
        }
    }
}

spotless {
    java {
        googleJavaFormat("1.27.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
