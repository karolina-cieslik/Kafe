package com.karolina.cieslik.kafe

import platform.UIKit.UIDevice

/** iOS z nazwą i wersją systemu. */
class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/** Zwraca [IOSPlatform]. */
actual fun getPlatform(): Platform = IOSPlatform()