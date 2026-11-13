import Foundation

/// Widths the app asks Cloudinary for. Product photos are uploaded as ~2 MB originals;
/// a resized WebP is 20-50x smaller, which is what makes images show up quickly.
/// Same values as Android's `ImageSize`.
enum ImageSize: Int {
    /// Small round chips and cart rows (64-72 pt).
    case thumb = 200
    /// Product cards in grids and rows.
    case card = 480
    /// Full-width banners and the product detail gallery.
    case full = 1080

    var width: Int { rawValue }
}

private let uploadMarker = "/image/upload/"

/// Cloudinary URL resized to `size` (WebP, automatic quality, never upscaled).
/// Non-Cloudinary URLs, and URLs that already carry a transformation, are returned unchanged.
func optimizedImageURL(_ url: String?, _ size: ImageSize) -> String? {
    guard let url, !url.isBlank else { return nil }
    guard url.contains("res.cloudinary.com"), let marker = url.range(of: uploadMarker) else { return url }
    let head = url[..<marker.upperBound]
    let rest = url[marker.upperBound...]
    let firstSegment = rest.split(separator: "/", maxSplits: 1, omittingEmptySubsequences: false).first ?? ""
    // e.g. "w_300,c_fill" or "q_auto": someone already chose a transformation.
    if firstSegment.contains(",") ||
        firstSegment.range(of: "^[a-z]{1,3}_", options: .regularExpression) != nil {
        return url
    }
    return head + "f_webp,q_auto,c_limit,w_\(size.width)/" + rest
}
