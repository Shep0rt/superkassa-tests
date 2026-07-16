package kz.superkassa.tests.framework.reporting

import io.qameta.allure.Allure

fun <T> reportStep(name: String, action: () -> T): T {
    val step: Allure.ThrowableRunnable<T> = Allure.ThrowableRunnable {
        action()
    }

    return Allure.step(name, step)
}
