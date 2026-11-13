import SwiftUI

/// One card in an endless feed; `id` stays unique when a product comes back in a later round.
struct FeedEntry: Identifiable {
    let id: String
    let product: Product
}

/// Doomscroll-style product feed: reveals `pageSize` products at a time as the user scrolls.
/// When `loop` is on and the catalog runs out, it starts a new reshuffled round so the feed
/// never ends. Without `loop` it stops after the last product (searches and filters).
/// Same rules as Android's `EndlessFeed`.
@MainActor
final class EndlessFeed: ObservableObject {
    static let minProductsToLoop = 8
    /// Start loading the next page when the user is this many items from the end.
    static let prefetchDistance = 8

    @Published private(set) var entries: [FeedEntry] = []

    private let pageSize: Int
    private let keyPrefix: String
    private var source: [Product] = []
    private var loop = true
    /// Round 0 keeps the caller's order (sorting, sections); later rounds are shuffled.
    private var round = 0
    private var order: [Product] = []
    private var position = 0

    init(pageSize: Int = 20, keyPrefix: String = "feed") {
        self.pageSize = pageSize
        self.keyPrefix = keyPrefix
    }

    /// True while scrolling can still bring more cards (always, for a looping feed).
    var hasMore: Bool { !source.isEmpty && (position < order.count || canLoop) }

    // Looping a handful of products would just show obvious repeats.
    private var canLoop: Bool { loop && source.count >= Self.minProductsToLoop }

    /// Restarts the feed when the products (or the loop setting) change; no-op otherwise.
    func update(source: [Product], loop: Bool) {
        guard source.map(\.id) != self.source.map(\.id) || loop != self.loop else { return }
        self.source = source
        self.loop = loop
        round = 0
        order = source
        position = 0
        entries = []
        loadMore()
    }

    func loadMore() {
        guard hasMore else { return }
        var added: [FeedEntry] = []
        added.reserveCapacity(pageSize)
        while added.count < pageSize {
            if position >= order.count {
                guard canLoop else { break }
                round += 1
                var rng = SeededRandom(seed: UInt64(round * 7919 + source.count))
                order = source.shuffled(using: &rng)
                position = 0
            }
            let product = order[position]
            position += 1
            added.append(FeedEntry(id: "\(keyPrefix)_\(round)_\(product.id)", product: product))
        }
        if !added.isEmpty { entries += added }
    }

    /// Call from each card's `onAppear`: loads the next page near the end of the list.
    func onAppear(_ entry: FeedEntry) {
        guard let index = entries.lastIndex(where: { $0.id == entry.id }) else { return }
        if index >= entries.count - Self.prefetchDistance { loadMore() }
    }
}

/// Small deterministic generator (SplitMix64) so a round's order is stable while scrolling.
private struct SeededRandom: RandomNumberGenerator {
    private var state: UInt64
    init(seed: UInt64) { state = seed }

    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}

/// Small spinner at the bottom of a feed that still has more to show; appearing loads more.
struct FeedLoadingFooter: View {
    let onAppear: () -> Void

    var body: some View {
        ProgressView()
            .tint(.brandGreen)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .onAppear(perform: onAppear)
    }
}
