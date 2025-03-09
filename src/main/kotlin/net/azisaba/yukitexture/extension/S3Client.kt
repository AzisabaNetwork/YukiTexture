package net.azisaba.yukitexture.extension

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import net.azisaba.yukitexture.config.S3Config

fun S3Client.Companion.of(credentials: S3Config) =
    S3Client {
        region = credentials.region
        endpointUrl = Url.parse(credentials.endpoint)
        credentialsProvider =
            StaticCredentialsProvider {
                accessKeyId = credentials.accessKeyId
                secretAccessKey = credentials.secretAccessKey
            }
    }
