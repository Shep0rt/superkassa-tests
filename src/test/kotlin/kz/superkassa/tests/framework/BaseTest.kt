package kz.superkassa.tests.framework

import kz.superkassa.tests.framework.config.TestConfig
import kz.superkassa.tests.framework.http.SuperkassaApiClient
import kz.superkassa.tests.framework.support.Polling

@Suppress("unused")
abstract class BaseTest {
    protected val superkassa = SuperkassaApiClient(CONFIG)
    protected val polling = Polling(CONFIG)

    companion object {
        protected val CONFIG: TestConfig = TestConfig.load()
    }
}
