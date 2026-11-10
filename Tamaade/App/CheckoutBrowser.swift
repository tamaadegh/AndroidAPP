import AuthenticationServices
import UIKit

/// Opens the Hubtel checkout in an in-app Safari sheet and catches the backend's redirect to
/// tamaade://checkout/result (the iOS counterpart of Android opening the browser and getting
/// the deep link back).
final class CheckoutBrowser: NSObject, ASWebAuthenticationPresentationContextProviding {
    private var session: ASWebAuthenticationSession?

    /// Starts the sheet. `onFinish` gets the tamaade:// URL, or nil when the user closed the sheet.
    /// Returns false when the sheet could not be shown.
    func open(_ url: URL, onFinish: @escaping (URL?) -> Void) -> Bool {
        let session = ASWebAuthenticationSession(url: url, callbackURLScheme: "tamaade") { [weak self] callback, _ in
            DispatchQueue.main.async {
                self?.session = nil
                onFinish(callback)
            }
        }
        session.presentationContextProvider = self
        // Keep Safari cookies so a Hubtel login (if any) is remembered.
        session.prefersEphemeralWebBrowserSession = false
        self.session = session
        if session.start() { return true }
        self.session = nil
        return false
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first(where: \.isKeyWindow)
            ?? scenes.first?.windows.first
            ?? ASPresentationAnchor()
    }
}
