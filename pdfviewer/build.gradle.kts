plugins {
    id("com.android.library")  // Updated Android Gradle Plugin version
    id("kotlin-android")       // Updated Kotlin plugin version
    id("maven-publish")

}

android {
    namespace = "com.japps.pdfviewer"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate{
    android.libraryVariants.forEach {
        variant-> publishing.publications.create(variant.name,MavenPublication::class.java){
            groupId = "com.github.jayeshshah417"
            artifactId = "pdfviewer"
            version = "0.0.1"
    }
    }
}