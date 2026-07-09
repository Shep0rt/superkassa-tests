package kz.superkassa.tests.framework.tags

import org.junit.jupiter.api.Tag

@Tag(TestLayer.PROTOCOL)
@Tag(TestSuite.SMOKE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Suppress("unused")
annotation class ProtocolSmoke
