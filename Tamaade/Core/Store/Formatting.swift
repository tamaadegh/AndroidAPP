import Foundation

enum PhoneNumber {
    static let otpLength = 6

    /// Light check of a Ghana mobile number (0XXXXXXXXX, 233XXXXXXXXX or +233XXXXXXXXX, spaces and
    /// dashes allowed). Returns it as +233XXXXXXXXX, or nil when it doesn't look valid.
    /// The server does the real validation.
    static func normalizeGhana(_ raw: String) -> String? {
        let compact = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .filter { !" \t\n-()".contains($0) }
        guard compact.range(of: #"^(\+?233|0)[235]\d{8}$"#, options: .regularExpression) != nil else {
            return nil
        }
        return "+233" + compact.suffix(9)
    }

    /// What the phone field accepts while typing.
    static func filterInput(_ text: String) -> String {
        text.filter { $0.isASCII && ($0.isNumber || "+ -()".contains($0)) }
    }

    /// Digits only, at most `otpLength`.
    static func filterCode(_ text: String) -> String {
        String(text.filter { $0.isASCII && $0.isNumber }.prefix(otpLength))
    }
}

enum Validation {
    static let minPasswordLength = 8

    static func isEmail(_ text: String) -> Bool {
        text.range(of: #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#, options: .regularExpression) != nil
    }
}

enum PriceFormat {
    private static let formatter: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US")
        f.numberStyle = .decimal
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 2
        f.roundingMode = .halfEven
        return f
    }()

    static func format(_ price: String) -> String {
        guard let value = Double(price) else { return SiteConfig.currency + price }
        return format(value)
    }

    static func format(_ value: Double) -> String {
        SiteConfig.currency + (formatter.string(from: NSNumber(value: value)) ?? String(value))
    }
}
