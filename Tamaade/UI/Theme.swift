import SwiftUI

/// TamaadeWeb globals.css tokens (same values as the Android theme).
extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }

    static let brandGreen = Color(hex: 0x365944)
    static let brandGreenDark = Color(hex: 0x2D4A38)
    static let brandBlue = Color(hex: 0x2D54A4)
    static let brandYellow = Color(hex: 0xFFD100)
    static let pageBackground = Color(hex: 0xF5F5F5)
    static let foreground = Color(hex: 0x1A1A1A)
    static let muted = Color(hex: 0x666666)
    static let border = Color(hex: 0xE5E5E5)
    static let newBadge = Color(hex: 0x28A745)
    static let promoStripStart = Color(hex: 0xFFF8E1)
    static let promoStripMid = Color(hex: 0xFFE082)
    static let cream = Color(hex: 0xF2F0E9)
    static let cardImageBackground = Color(hex: 0xF7F7F7)
    static let strikeGray = Color(hex: 0x9B9B9B)
    static let errorRed = Color(hex: 0xF01F0E)
}

extension LinearGradient {
    /// Yellow promo strip (product promo labels, "top_strip" promo).
    static let promoStrip = LinearGradient(
        colors: [.promoStripStart, .promoStripMid, .promoStripStart],
        startPoint: .leading, endPoint: .trailing
    )
}

extension Font {
    static let tHeadlineLarge = Font.system(size: 28, weight: .bold)
    static let tHeadline = Font.system(size: 22, weight: .bold)
    static let tTitleLarge = Font.system(size: 18, weight: .semibold)
    static let tTitle = Font.system(size: 16, weight: .semibold)
    static let tBodyLarge = Font.system(size: 16)
    static let tBody = Font.system(size: 14)
    static let tBodySmall = Font.system(size: 12)
    static let tLabel = Font.system(size: 14, weight: .semibold)
    static let tLabelSmall = Font.system(size: 11, weight: .medium)
}

extension View {
    /// Green navigation bar with white title and buttons, used on every screen.
    func brandNavigationBar() -> some View {
        self
            .toolbarBackground(Color.brandGreen, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
    }
}
