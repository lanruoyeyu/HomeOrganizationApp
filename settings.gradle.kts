pluginManagement {
    repositories {
        // 阿里云镜像（国内加速）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（国内加速）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}

rootProject.name = "FridgeKeeper"
include(":app")

// 编译中间产物与 APK 输出到 F 盘，避免占用工程目录
gradle.projectsLoaded {
    val buildRoot = java.io.File("F:/software/app-build/HomeOrganizationApp")
    rootProject.allprojects {
        layout.buildDirectory.set(java.io.File(buildRoot, name))
    }
}
