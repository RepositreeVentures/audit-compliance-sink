plugins {
    id("repositree.spring-service")
}

val awsSdkVersion = "2.26.27"
val parquetVersion = "1.14.1"
val hadoopVersion = "3.4.0"
val bouncyCastleVersion = "1.78.1"
val platformLibsVersion = "0.1.3"

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:$awsSdkVersion")
    }
}

dependencies {
    // platform-libs
    implementation("io.repositree.libs:repositree-common:$platformLibsVersion")
    implementation("io.repositree.libs:repositree-observability:$platformLibsVersion")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // AWS SDK v2 — S3, Athena, Glue, S3Control (Object Lock / Legal Hold)
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:s3control")
    implementation("software.amazon.awssdk:athena")
    implementation("software.amazon.awssdk:glue")
    implementation("software.amazon.awssdk:sts")

    // Parquet + Hadoop minimal (no YARN/HDFS) for Parquet file writing
    implementation("org.apache.parquet:parquet-avro:$parquetVersion")
    implementation("org.apache.parquet:parquet-hadoop:$parquetVersion") {
        exclude(group = "org.apache.hadoop", module = "hadoop-common")
    }
    implementation("org.apache.hadoop:hadoop-common:$hadoopVersion") {
        exclude(group = "org.apache.hadoop.thirdparty", module = "hadoop-shaded-protobuf_3_25")
        exclude(group = "com.sun.jersey")
        exclude(group = "javax.servlet")
        exclude(group = "log4j")
    }
    implementation("org.apache.hadoop:hadoop-mapreduce-client-core:$hadoopVersion") {
        exclude(group = "com.sun.jersey")
        exclude(group = "javax.servlet")
        exclude(group = "log4j")
    }
    implementation("org.apache.avro:avro:1.11.3")

    // Ed25519 via Bouncy Castle (JDK 15+ has native support but BC gives more control)
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")

    testImplementation("io.repositree.libs:repositree-test:$platformLibsVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.mockito:mockito-core")
}
