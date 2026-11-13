import Foundation

enum PhoneNumber {
    static let otpLength = 6

    /// Light check of a Ghana mobile number (XXXXXXXXX, 0XXXXXXXXX, 233XXXXXXXXX or +233XXXXXXXXX, spaces and
    /// dashes allowed). Returns it as +233XXXXXXXXX, or nil when it doesn't look valid.
    /// The server does the real validation.
    static func normalizeGhana(_ raw: String) -> String? {
        let compact = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .filter { !" \t\n-()".contains($0) }
        guard compact.range(of: #"^(\+?233|0)?[235]\d{8}$"#, options: .regularExpression) != nil else {
            return nil
        }
        return "+233" + compact.suffix(9)
    }

    /// What the phone field accepts while typing.
    static func filterInput(_ text: String) -> String {
        text.filter { $0.isASCII && ($0.isNumber || "+ -()".contains($0)) }
    }

    /// The 9 local digits after +233: drops what the prefix already says, so typing or pasting
    /// "024 123 4567", "+233 24 123 4567" or "233241234567" all give "241234567".
    static func ghanaLocalDigits(_ raw: String) -> String {
        var digits = raw.filter { $0.isASCII && $0.isNumber }
        if digits.hasPrefix("233") && digits.count > 9 { digits = String(digits.dropFirst(3)) }
        digits = String(digits.drop { $0 == "0" })
        return String(digits.prefix(9))
    }

    /// Local digits grouped as typed in the field: 241234567 -> "24 123 4567".
    static func groupLocal(_ digits: String) -> String {
        var out = ""
        for (i, c) in digits.enumerated() {
            if i == 2 || i == 5 { out.append(" ") }
            out.append(c)
        }
        return out
    }

    /// +233241234567 -> "+233 24 123 4567" (anything else is returned as is).
    static func formatGhana(_ phone: String) -> String {
        guard phone.range(of: #"^\+233\d{9}$"#, options: .regularExpression) != nil else { return phone }
        return "+233 " + groupLocal(String(phone.suffix(9)))
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
