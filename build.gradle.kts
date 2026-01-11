plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.14.1"
}

group = "com.jacoco"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.1") // 你可以根据需要修改为你的 IDEA 版本
    type.set("IC")         // IC for Community Edition, IU for Ultimate
    plugins.set(listOf("com.intellij.java"))
}

dependencies {
    implementation("org.jacoco:org.jacoco.core:0.8.12") // 新增：JaCoCo CLI工具
    implementation("org.jacoco:org.jacoco.report:0.8.12") // 新增：报告生成核心
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
// 打包 JAR 时添加 MANIFEST.MF 信息（可选）
tasks.jar {
    manifest {
        attributes(
                mapOf(
                        "Implementation-Title" to project.name,
                        "Implementation-Version" to project.version
                )
        )
    }
}
tasks.withType<JavaExec> {
    val javaHome = System.getenv("JAVA_HOME") // 或者直接写死路径
    executable = "$javaHome/bin/java"
}
