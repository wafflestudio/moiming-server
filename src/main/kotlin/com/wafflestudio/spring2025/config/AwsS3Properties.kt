package com.wafflestudio.spring2025.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class AwsS3Properties(
    val bucket: String,
    val region: String,
    val presignExpireSeconds: Long = 1800,
)
