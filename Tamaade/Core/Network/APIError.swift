import Foundation

/// A failed API call. `status` is the HTTP code, or 0 when the server could not be reached.
/// `code` is the machine-readable "code" field of the error body (e.g. "not_registered"),
/// `retryAfter` the "retry_after" seconds of a 429 and `fieldErrors` the per-field messages of
/// an {"errors": {"password": [...]}} body, when the server sent them.
struct APIError: Error, LocalizedError, Equatable {
    let message: String
    let status: Int
    var code: String? = nil
    var retryAfter: Int? = nil
    var fieldErrors: [String: String] = [:]

    var isUnauthorized: Bool { status == 401 }

    var errorDescription: String? { message }

    static let offlineMessage = "Can't reach Tamaade. Check your internet connection and try again."
    static let timeoutMessage = "Tamaade is taking too long to respond. Please try again."
    static let sessionExpiredMessage = "Your session has expired. Please sign in again."

    static let offline = APIError(message: offlineMessage, status: 0)

    /// Any error from a call, as an APIError (unexpected errors read as "offline").
    static func from(_ error: Error) -> APIError {
        if let api = error as? APIError { return api }
        if let url = error as? URLError, url.code == .timedOut {
            return APIError(message: timeoutMessage, status: 0)
        }
        return .offline
    }
}

/// Turns DRF error bodies into user-facing text (same rules as the Android app).
enum APIErrorParser {
    /// Machine-readable error fields that are not messages for the user.
    private static let metaKeys: Set<String> = ["code", "retry_after", "errors"]

    /// Flattens DRF errors: {"detail"}, {"errors": {...}}, {"non_field_errors": [...]} or {"field": ["msg"]}.
    static func message(body: Any?, status: Int) -> String {
        if status == 401 { return APIError.sessionExpiredMessage }
        if let array = body as? [Any] {
            let text = joined(array)
            if !text.isBlank { return text }
        }
        if let json = body as? JSONObject {
            if let detail = json.string("detail"), !detail.isBlank { return detail }
            let fields = fieldErrors(json.object("errors"))
            let fieldText = fields.keys.sorted().compactMap { fields[$0] }.joined(separator: "\n")
            if !fieldText.isBlank { return fieldText }
            if let nonField = json.array("non_field_errors") {
                let text = joined(nonField)
                if !text.isBlank { return text }
            }
            let messages: [String] = json.keys.sorted().filter { !metaKeys.contains($0) }.compactMap { key in
                let text: String?
                switch json[key] {
                case let array as [Any]: text = joined(array)
                case let string as String: text = string
                default: text = nil
                }
                guard let text, !text.isBlank else { return nil }
                return "\(fieldLabel(key)): \(text)"
            }
            if !messages.isEmpty { return messages.joined(separator: "\n") }
        }
        if status == 404 { return "This feature isn't available on the server yet." }
        if status >= 500 { return "Tamaade server error (\(status)). Please try again shortly." }
        return "Request failed (\(status)). Please try again."
    }

    /// {"password": ["Too short.", "Too common."]} -> ["password": "Too short. Too common."]
    static func fieldErrors(_ errors: JSONObject?) -> [String: String] {
        guard let errors else { return [:] }
        var result: [String: String] = [:]
        for (key, value) in errors {
            let text: String?
            switch value {
            case let array as [Any]: text = joined(array)
            case let string as String: text = string
            default: text = nil
            }
            if let text, !text.isBlank { result[key] = text }
        }
        return result
    }

    static func fieldLabel(_ key: String) -> String {
        let spaced = key.replacingOccurrences(of: "_", with: " ")
        guard let first = spaced.first else { return spaced }
        return first.uppercased() + spaced.dropFirst()
    }

    private static func joined(_ array: [Any]) -> String {
        array.compactMap { item -> String? in
            if item is NSNull { return nil }
            let text = (item as? String) ?? "\(item)"
            return text.isBlank ? nil : text
        }.joined(separator: " ")
    }
}
