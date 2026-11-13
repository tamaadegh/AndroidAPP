package com.tamaade.ecommerce.data

/**
 * Widths the app asks Cloudinary for. Product photos are uploaded as ~2 MB originals;
 * a resized WebP is 20-50x smaller, which is what makes images show up quickly.
 */
enum class ImageSize(val width: Int) {
    /** Small round chips and cart rows (64-72 dp). */
    Thumb(200),
    /** Product cards in grids and rows. */
    Card(480),
    /** Full-width banners and the product detail gallery. */
    Full(1080)
}

private const val UPLOAD_MARKER = "/image/upload/"

/**
 * Cloudinary URL resized to [size] (WebP, automatic quality, never upscaled).
 * Non-Cloudinary URLs, and URLs that already carry a transformation, are returned unchanged.
 */
fun optimizedImageUrl(url: String?, size: ImageSize): String? {
    if (url.isNullOrBlank()) return null
    if (!url.contains("res.cloudinary.com")) return url
    val at = url.indexOf(UPLOAD_MARKER)
    if (at < 0) return url
    val head = url.substring(0, at + UPLOAD_MARKER.length)
    val rest = url.substring(at + UPLOAD_MARKER.length)
    val firstSegment = rest.substringBefore('/')
    // e.g. "w_300,c_fill" or "q_auto": someone already chose a transformation.
    if (firstSegment.contains(',') || Regex("^[a-z]{1,3}_").containsMatchIn(firstSegment)) return url
    return head + "f_webp,q_auto,c_limit,w_${size.width}/" + rest
}
