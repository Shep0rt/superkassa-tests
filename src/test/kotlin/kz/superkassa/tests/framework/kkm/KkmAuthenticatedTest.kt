package kz.superkassa.tests.framework.kkm

import kz.superkassa.tests.framework.BaseApiTest
import org.junit.jupiter.api.BeforeEach

@Suppress("NonAsciiCharacters")
abstract class KkmAuthenticatedTest : BaseApiTest() {
    private val kkmAuth = KkmAuthSupport(superkassa, config)
    protected lateinit var preparedKkm: PreparedKkmAuth

    @BeforeEach
    fun `Получаем контрольную ККМ и подготовленный ADMIN PIN`() {
        preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
    }
}
