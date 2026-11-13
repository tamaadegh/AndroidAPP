import SwiftUI

/// Product card aligned with TamaadeWeb ProductCard.tsx.
struct ProductCard: View {
    let product: Product
    let onTap: () -> Void
    let onAddToCart: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onTap) {
                VStack(alignment: .leading, spacing: 0) {
                    ZStack(alignment: .topLeading) {
                        Color.cardImageBackground
                        RemoteImage(url: product.image)
                            .padding(8)
                        if product.isNew {
                            Badge(text: "New", background: .newBadge)
                                .padding(8)
                        }
                    }
                    .aspectRatio(0.85, contentMode: .fit)
                    .clipped()

                    if !product.promoLabel.isBlank {
                        Text(product.promoLabel)
                            .font(.tLabelSmall.weight(.bold))
                            .foregroundColor(.brandGreen)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 4)
                            .background(LinearGradient.promoStrip)
                    }

                    VStack(alignment: .leading, spacing: 0) {
                        Text(product.brand.isBlank ? product.name : product.brand.uppercased() + " " + product.name)
                            .font(.tBodySmall)
                            .foregroundColor(.foreground)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                            .frame(minHeight: 30, alignment: .topLeading)
                        HStack(spacing: 6) {
                            Text(PriceFormat.format(product.price))
                                .font(.tTitle.weight(.bold))
                                .foregroundColor(.foreground)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                            if let pct = product.effectiveDiscount {
                                Badge(text: "-\(pct)%", foreground: .brandGreen, background: Color.brandGreen.opacity(0.1))
                            }
                        }
                        .padding(.top, 6)
                        Text(product.originalPrice.map { PriceFormat.format($0) } ?? " ")
                            .font(.tBodySmall)
                            .foregroundColor(.strikeGray)
                            .strikethrough(product.originalPrice != nil)
                    }
                    .padding(.horizontal, 10)
                    .padding(.top, 8)
                }
            }
            .buttonStyle(.plain)

            HStack {
                if product.isExpress {
                    Badge(text: "express", capsule: true)
                }
                Spacer()
                Button(action: onAddToCart) {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(product.inStock ? Color.brandGreen : Color.brandGreen.opacity(0.4))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(!product.inStock)
                .accessibilityLabel("Add to cart")
            }
            .padding(.horizontal, 10)
            .padding(.top, 6)
            .padding(.bottom, 8)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

/// Round merch tile / category chip with an optional badge.
struct CategoryChip: View {
    let title: String
    let imageURL: String?
    var badge = ""
    var highlight = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 6) {
                ZStack(alignment: .bottom) {
                    Circle()
                        .fill(highlight ? Color.brandGreen : Color.cardImageBackground)
                        .frame(width: 64, height: 64)
                    RemoteImage(url: imageURL, contentMode: .fill, size: .thumb)
                        .frame(width: highlight ? 58 : 64, height: highlight ? 58 : 64)
                        .clipShape(Circle())
                        .frame(width: 64, height: 64)
                    if !badge.isBlank {
                        Text(badge)
                            .font(.tLabelSmall)
                            .foregroundColor(.white)
                            .lineLimit(1)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(Color.brandGreen)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                }
                Text(title)
                    .font(.tLabelSmall)
                    .foregroundColor(.foreground)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
            }
            .frame(width: 76)
        }
        .buttonStyle(.plain)
    }
}

/// Top strip from the "top_strip" promo: title + bold highlight + CTA.
struct PromoBanner: View {
    let title: String
    let highlight: String
    let ctaLabel: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                (Text(title) + Text(highlight.isBlank ? "" : " " + highlight).bold())
                    .font(.tLabel.weight(.regular))
                    .foregroundColor(.brandGreenDark)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 8)
                Text((ctaLabel.isBlank ? "Shop now" : ctaLabel) + " ›")
                    .font(.tLabel)
                    .foregroundColor(.brandGreenDark)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(LinearGradient.promoStrip)
        }
        .buttonStyle(.plain)
    }
}

struct SectionHeader: View {
    let title: String
    let subtitle: String
    var actionLabel = "View all"
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.tTitleLarge.weight(.bold))
                    .foregroundColor(.foreground)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.tBodySmall)
                        .foregroundColor(.muted)
                }
            }
            Spacer()
            if let onAction {
                Button(actionLabel, action: onAction)
                    .font(.tLabel)
                    .foregroundColor(.brandGreen)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

/// White rounded row used on Profile ("My basket", "Edit my details", ...).
struct SettingsRow<Trailing: View>: View {
    let systemImage: String
    let title: String
    var subtitle: String? = nil
    var tint: Color = .brandGreen
    var titleColor: Color = .foreground
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 18))
                .foregroundColor(tint)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.tTitle.weight(.regular))
                    .foregroundColor(titleColor)
                if let subtitle {
                    Text(subtitle)
                        .font(.tBodySmall)
                        .foregroundColor(.muted)
                }
            }
            Spacer()
            trailing()
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .contentShape(Rectangle())
    }
}

struct Chevron: View {
    var body: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(.muted)
    }
}
