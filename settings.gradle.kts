pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
        maven("https://raw.githubusercontent.com/HighCapable/maven-repository/main/repository/releases") {
            content {
                includeGroupByRegex("com\\.highcapable.*")
            }
        }
    }
}

rootProject.name = "HyperZoomRing"
include(":app")
