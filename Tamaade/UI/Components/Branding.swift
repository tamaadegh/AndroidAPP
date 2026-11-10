import SwiftUI

/// The real TAMAADE logo (trimmed to the wordmark). Its background is brand green (#365944),
/// so it blends into green surfaces.
struct TamaadeWordmark: View {
    var height: CGFloat = 40

    var body: some View {
        Image("TamaadeLogo")
            .resizable()
            .scaledToFit()
            .frame(height: height)
            .accessibilityLabel(SiteConfig.name)
    }
}

/// Green tile with the logo, shown while an image loads or when it fails / is missing.
struct LogoPlaceholder: View {
    var body: some View {
        GeometryReader { geo in
            ZStack {
                Color.brandGreen
                Image("TamaadeLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: geo.size.width * 0.8)
            }
        }
        .accessibilityHidden(true)
    }
}

/// Remote product/category image that falls back to the Tamaade logo instead of a blank box.
struct RemoteImage: View {
    let url: String?
    var contentMode: ContentMode = .fit

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:)), transaction: Transaction(animation: .easeOut(duration: 0.2))) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: contentMode)
            } else {
                LogoPlaceholder()
            }
        }
    }
}

/// Dots under a pager (hero banners, product gallery).
struct PageDots: View {
    let count: Int
    let current: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { i in
                Circle()
                    .fill(i == current ? Color.brandGreen : Color.brandGreen.opacity(0.3))
                    .frame(width: i == current ? 8 : 6, height: i == current ? 8 : 6)
            }
        }
    }
}

/// Small rounded label ("New", "express", "-20%").
struct Badge: View {
    let text: String
    var foreground: Color = .white
    var background: Color = .brandGreen
    var capsule = false

    var body: some View {
        Text(text)
            .font(.tLabelSmall.weight(.semibold))
            .foregroundColor(foreground)
            .padding(.horizontal, capsule ? 8 : 5)
            .padding(.vertical, capsule ? 3 : 2)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: capsule ? 50 : 4))
    }
}
