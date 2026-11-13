import SwiftUI

@main
struct TamaadeApp: App {
    @StateObject private var store = StoreModel(loadMusic: !TamaadeApp.isRunningTests)
    @StateObject private var catalog = CatalogModel(autoload: !TamaadeApp.isRunningTests)
    @StateObject private var router = Router()

    /// Unit tests host the app; it must not call the live API while they run.
    static let isRunningTests = ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(catalog)
                .environmentObject(router)
                .preferredColorScheme(.light)
                // Hubtel return when the page was opened outside the in-app browser.
                .onOpenURL { store.handleDeepLink($0) }
        }
    }
}
