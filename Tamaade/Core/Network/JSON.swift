import Foundation

/// A decoded JSON object. The API is read leniently (like org.json on Android):
/// numbers sent as strings and strings sent as numbers are both accepted.
typealias JSONObject = [String: Any]

enum JSON {
    /// Parses a response body; anything that isn't a JSON object or array gives nil.
    static func parse(_ data: Data) -> Any? {
        guard let text = String(data: data, encoding: .utf8)?
            .trimmingCharacters(in: .whitespacesAndNewlines),
              text.hasPrefix("{") || text.hasPrefix("[") else { return nil }
        return try? JSONSerialization.jsonObject(with: Data(text.utf8), options: [.fragmentsAllowed])
    }

    static func data(_ object: JSONObject) -> Data? {
        try? JSONSerialization.data(withJSONObject: object)
    }

    static func objects(_ value: Any?) -> [JSONObject] {
        (value as? [Any])?.compactMap { $0 as? JSONObject } ?? []
    }

    static func isBool(_ number: NSNumber) -> Bool {
        CFGetTypeID(number) == CFBooleanGetTypeID()
    }
}

extension Dictionary where Key == String, Value == Any {
    /// Non-empty string value; JSON null, missing keys and "" give nil.
    func string(_ key: String) -> String? {
        guard let value = self[key], !(value is NSNull) else { return nil }
        let text: String
        if let s = value as? String {
            text = s
        } else if let n = value as? NSNumber {
            text = JSON.isBool(n) ? (n.boolValue ? "true" : "false") : n.stringValue
        } else {
            return nil
        }
        return text.isEmpty ? nil : text
    }

    func int(_ key: String) -> Int? {
        guard let value = self[key], !(value is NSNull) else { return nil }
        if let n = value as? NSNumber, !JSON.isBool(n) { return n.intValue }
        if let s = value as? String { return Int(s) }
        return nil
    }

    /// Like org.json optInt: 0 (or [fallback]) when missing or not a number.
    func int(_ key: String, default fallback: Int) -> Int {
        if let n = self[key] as? NSNumber, !JSON.isBool(n) { return n.intValue }
        if let s = self[key] as? String, let d = Double(s) { return Int(d) }
        return fallback
    }

    func double(_ key: String) -> Double? {
        guard let value = self[key], !(value is NSNull) else { return nil }
        if let n = value as? NSNumber, !JSON.isBool(n) { return n.doubleValue }
        if let s = value as? String { return Double(s) }
        return nil
    }

    /// Like org.json optBoolean: a JSON boolean or the strings "true"/"false".
    func bool(_ key: String, default fallback: Bool) -> Bool {
        if let n = self[key] as? NSNumber, JSON.isBool(n) { return n.boolValue }
        if let s = self[key] as? String {
            if s.caseInsensitiveCompare("true") == .orderedSame { return true }
            if s.caseInsensitiveCompare("false") == .orderedSame { return false }
        }
        return fallback
    }

    func object(_ key: String) -> JSONObject? {
        self[key] as? JSONObject
    }

    func array(_ key: String) -> [Any]? {
        self[key] as? [Any]
    }
}
