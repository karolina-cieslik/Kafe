import SwiftUI
import Shared

@main
struct iOSApp: App {
    // KAFE-2: Start DI – adres API do podmiany po uruchomieniu backendu.
    init() {
        #if DEBUG
        KoinIosKt.startKoinIos(baseUrl: "https://api.kafe.invalid/", enableLogging: true)
        #else
        KoinIosKt.startKoinIos(baseUrl: "https://api.kafe.invalid/", enableLogging: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}