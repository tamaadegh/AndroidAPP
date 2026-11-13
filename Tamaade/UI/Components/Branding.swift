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
///
/// `size` picks the resized Cloudinary variant (see `optimizedImageURL`). When `previewSize` is
/// set, that smaller variant is shown straight from cache while the `size` one downloads,
/// e.g. the card photo on the product page while the full-size photo arrives.
struct RemoteImage: View {
    let url: String?
    var contentMode: ContentMode = .fit
    var size: ImageSize = .card
    var previewSize: ImageSize? = nil

    @State private var image: UIImage?
    @State private var preview: UIImage?

    private var mainURL: String? { optimizedImageURL(url, size) }
    private var previewURL: String? { previewSize.flatMap { optimizedImageURL(url, $0) } }

    var body: some View {
        // Color.clear takes exactly the offered size; the image (even in .fill) is clipped to it.
        Color.clear
            .overlay {
                if let shown = image ?? memoryImage {
                    Image(uiImage: shown).resizable().aspectRatio(contentMode: contentMode)
                } else if let preview {
                    Image(uiImage: preview).resizable().aspectRatio(contentMode: contentMode)
                } else {
                    LogoPlaceholder()
                }
            }
            .clipped()
            .task(id: mainURL) { await load() }
    }

    /// Images already decoded in memory draw on the first frame, without a placeholder flash.
    private var memoryImage: UIImage? {
        mainURL.flatMap { ImageLoader.shared.cachedImage($0) }
    }

    private func load() async {
        image = nil
        preview = nil
        guard let mainURL else { return }
        if let cached = ImageLoader.shared.cachedImage(mainURL) {
            image = cached
            return
        }
        // Preview only if already cached: never download the same photo twice.
        if let previewURL, let cached = await ImageLoader.shared.cachedOrDiskImage(previewURL) {
            preview = cached
        }
        let loaded = await ImageLoader.shared.image(mainURL)
        guard !Task.isCancelled else { return }
        withAnimation(.easeOut(duration: 0.15)) { image = loaded }
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
