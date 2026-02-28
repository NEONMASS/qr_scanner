pluginManagement {
    repositories {
        google() // This is what was missing!
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// You can name this whatever your project is actually called
rootProject.name = "permissionauditor" 
include(":app")