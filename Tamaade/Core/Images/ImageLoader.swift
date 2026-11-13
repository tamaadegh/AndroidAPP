import UIKit

/// Shared image pipeline (the iOS counterpart of the app-wide Coil loader on Android):
/// decoded images in memory, downloads in a 250 MB disk cache that survives restarts.
/// Cloudinary URLs never change content, so cached copies are used without revalidation.
final class ImageLoader: @unchecked Sendable {
    static let shared = ImageLoader()

    private let memory = NSCache<NSString, UIImage>()
    private let session: URLSession
    private let lock = NSLock()
    private var inFlight: [String: Task<UIImage?, Never>] = [:]

    init() {
        memory.totalCostLimit = 64 * 1024 * 1024
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
        let cache = URLCache(
            memoryCapacity: 16 * 1024 * 1024,
            diskCapacity: 250 * 1024 * 1024,
            directory: caches?.appendingPathComponent("image_cache", isDirectory: true)
        )
        let config = URLSessionConfiguration.default
        config.urlCache = cache
        config.requestCachePolicy = .returnCacheDataElseLoad
        config.timeoutIntervalForRequest = 30
        config.httpMaximumConnectionsPerHost = 6
        session = URLSession(configuration: config)
    }

    /// Already decoded and in memory (no disk or network).
    func cachedImage(_ url: String) -> UIImage? {
        memory.object(forKey: url as NSString)
    }

    /// Decoded image from memory, else the disk cache only (never the network).
    func cachedOrDiskImage(_ url: String) async -> UIImage? {
        if let image = cachedImage(url) { return image }
        guard let request = request(url, cacheOnly: true) else { return nil }
        guard let response = session.configuration.urlCache?.cachedResponse(for: request) else { return nil }
        return decode(response.data, url: url, keepInMemory: true)
    }

    /// Memory, then disk, then network. Concurrent requests for one URL share a download.
    func image(_ url: String, keepInMemory: Bool = true) async -> UIImage? {
        if let image = cachedImage(url) { return image }
        let task: Task<UIImage?, Never>
        lock.lock()
        if let running = inFlight[url] {
            task = running
        } else {
            task = Task.detached(priority: .userInitiated) { [weak self] in
                guard let self, let request = self.request(url, cacheOnly: false),
                      let (data, response) = try? await self.session.data(for: request),
                      ((response as? HTTPURLResponse)?.statusCode ?? 200) < 400 else { return nil }
                return self.decode(data, url: url, keepInMemory: keepInMemory)
            }
            inFlight[url] = task
        }
        lock.unlock()
        let image = await task.value
        lock.lock()
        inFlight[url] = nil
        lock.unlock()
        if keepInMemory, let image { memory.setObject(image, forKey: url as NSString, cost: Self.cost(image)) }
        return image
    }

    /// Downloads into the disk cache only (keeps RAM low), without waiting.
    func prefetchToDisk(_ urls: [String]) {
        let cache = session.configuration.urlCache
        for url in urls {
            guard cachedImage(url) == nil, let request = request(url, cacheOnly: false),
                  cache?.cachedResponse(for: request) == nil else { continue }
            session.dataTask(with: request).resume()
        }
    }

    private func request(_ url: String, cacheOnly: Bool) -> URLRequest? {
        guard let parsed = URL(string: url) else { return nil }
        return URLRequest(url: parsed, cachePolicy: cacheOnly ? .returnCacheDataDontLoad : .returnCacheDataElseLoad)
    }

    private func decode(_ data: Data, url: String, keepInMemory: Bool) -> UIImage? {
        // preparingForDisplay decodes off the main thread so scrolling stays smooth.
        guard let raw = UIImage(data: data) else { return nil }
        let image = raw.preparingForDisplay() ?? raw
        if keepInMemory { memory.setObject(image, forKey: url as NSString, cost: Self.cost(image)) }
        return image
    }

    private static func cost(_ image: UIImage) -> Int {
        Int(image.size.width * image.scale * image.size.height * image.scale * 4)
    }
}

/// Downloads images before the screens that show them, using the same URLs as `RemoteImage`
/// so the requests hit the same caches. Mirrors Android's `ImagePrefetcher`.
enum ImagePrefetcher {
    /// How many products of each home section are on (or right below) the first screen.
    private static let firstScreenProducts = 6

    /// Decodes what Home shows first (banners, tiles, the first products) into memory and
    /// returns once they are ready, so Home opens with its images in place.
    static func warmUpHome(_ data: CatalogData) async {
        var urls: [String?] = data.banners.map { optimizedImageURL($0.image, .full) }
        urls += (data.mainTiles + data.featuredTiles).map { optimizedImageURL($0.image, .thumb) }
        for section in data.homeSections.prefix(2) {
            urls += productsForSection(section.productSource, data.products)
                .prefix(firstScreenProducts)
                .map { optimizedImageURL($0.image, .card) }
        }
        let unique = distinct(urls)
        await withTaskGroup(of: Void.self) { group in
            for url in unique {
                group.addTask { _ = await ImageLoader.shared.image(url) }
            }
        }
    }

    /// Puts the rest of the catalog's images in the disk cache, so scrolling and other tabs
    /// show images without waiting on the network.
    static func prefetchCatalog(_ data: CatalogData) {
        var urls: [String?] = data.products.map { optimizedImageURL($0.image, .card) }
        urls += data.banners.map { optimizedImageURL($0.image, .full) }
        urls += (data.mainTiles + data.featuredTiles + data.bottomTiles).map { optimizedImageURL($0.image, .thumb) }
        urls += data.categories.map { optimizedImageURL($0.icon, .thumb) }
        ImageLoader.shared.prefetchToDisk(distinct(urls))
    }

    /// Every gallery photo of a product at detail size, so swiping the gallery is instant.
    static func prefetchProductGallery(_ product: Product) {
        ImageLoader.shared.prefetchToDisk(distinct(galleryURLs(product).map { optimizedImageURL($0, .full) }))
    }

    static func galleryURLs(_ product: Product) -> [String?] {
        product.images.isEmpty ? [product.image] : product.images.map { Optional($0) }
    }

    private static func distinct(_ urls: [String?]) -> [String] {
        var seen = Set<String>()
        return urls.compactMap { $0 }.filter { seen.insert($0).inserted }
    }
}
