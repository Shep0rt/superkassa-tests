package kz.superkassa.tests.framework.kkm

import kz.superkassa.tests.framework.BaseTest
import org.junit.jupiter.api.BeforeEach

@Suppress("NonAsciiCharacters")
abstract class KkmAuthenticatedTest : BaseTest() {
    protected lateinit var preparedKkm: PreparedKkmAuth

    @BeforeEach
    fun `Получаем контрольную ККМ и подготовленный ADMIN PIN`() {
        preparedKkm = kkmAuth.prepareFirstKkmAdminPin()
    }
}
