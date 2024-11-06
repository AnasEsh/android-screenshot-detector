import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

val publishProperties = Properties()
val file = File("publish.properties")
if (file.exists())
    FileInputStream(file).use { stream ->
        publishProperties.load(stream)
    }

val libName = "compose-form-state-utils"
val newVersion = publishProperties["VERSION"]
android {
    namespace = "com.aesh.screenshotdetector"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 31
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

//    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}


fun groovy.util.Node.appendDependencyNode(dependency: Dependency, scope: String) {
    appendNode("dependency").apply {
        appendNode("groupId", dependency.group)
        appendNode("artifactId", dependency.name)
        appendNode("version", dependency.version)
        appendNode("scope", scope)
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.mobily"
                artifactId = "composeformstateutils"
                version = newVersion.toString()
                artifact("${buildDir}/outputs/aar/compose-form-state-utils-release.aar")
                pom.withXml {
                    // including api dependencies
                    val dependenciesNode = asNode().appendNode("dependencies")
                    // Including implementation dependencies
                    configurations["implementation"].allDependencies.forEach { dep ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dep.group)
                        dependencyNode.appendNode("artifactId", dep.name)
                        dependencyNode.appendNode("version", dep.version)
                        dependencyNode.appendNode("scope", "compile")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GithubPackages"
                url = uri("https://maven.pkg.github.com/AnasEsh/ComposeFormStateUtils")
                //add credentials for gh
                credentials {
                    username = System.getenv("gh_username")
                    password = System.getenv("gh_package_cred")
                }
                isAllowInsecureProtocol = true
            }
        }
    }

}