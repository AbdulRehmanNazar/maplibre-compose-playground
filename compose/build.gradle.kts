import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.compose.compiler)
  id("maven-publish")
}

android {
  namespace = "com.maplibre.compose"
  compileSdk = 35

  defaultConfig {
    minSdk = 25

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions { jvmTarget = "1.8" }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)

  api(libs.maplibre)
  api(libs.maplibre.annotation)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test)
  androidTestImplementation(libs.androidx.ui.test.junit4)

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  coordinates("io.github.abdulrehmannazar", "maplibre-compose-fork", "1.0.0")

  configure(AndroidSingleVariantLibrary(sourcesJar = true, publishJavadocJar = true))

  pom {
    name.set("Maplibre Compose")
    url.set("https://github.com/AbdulRehmanNazar/maplibre-compose-playground")
    description.set("Composable UI wrapper for Maplibre-Native Android with Custom marker")
    inceptionYear.set("2025")
    licenses {
      license {
        name.set("MPL-2.0")
        url.set("https://www.mozilla.org/en-US/MPL/2.0/")
      }
    }
    developers {
      developer {
        name.set("Abdul Rehman Nazar")
        organization.set("Dev Techlogix")
        email.set("a.rehman.nazar@gmail.com")
      }
    }
    contributors {
      contributor {
        name.set("Ramani Maps")
        organizationUrl.set("https://github.com/ramani-maps/ramani-maps")
      }
    }
    scm {
      connection.set("scm:git:https://github.com/AbdulRehmanNazar/maplibre-compose-playground.git")
      developerConnection.set("scm:git:ssh://github.com/AbdulRehmanNazar/maplibre-compose-playground.git")
      url.set("https://github.com/AbdulRehmanNazar/maplibre-compose-playground")
    }

    withXml {
      val rootNode = asNode()
      val repositoriesNode = rootNode.appendNode("repositories")
      val repositoryNode = repositoriesNode.appendNode("repository")
      repositoryNode.appendNode("name", "Google")
      repositoryNode.appendNode("id", "google")
      repositoryNode.appendNode("url", "https://maven.google.com/")
    }
  }
}
