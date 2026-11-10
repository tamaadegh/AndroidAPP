import SwiftUI

struct CartView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    var body: some View {
        Group {
            if case let .paid(_, orderId) = store.checkoutState, store.cartLines.isEmpty {
                PaymentSuccess(orderId: orderId) {
                    store.dismissCheckoutStatus()
                    router.open(.home)
                }
            } else if store.cartLines.isEmpty {
                VStack(spacing: 0) {
                    CheckoutStatusBanner(state: store.checkoutState,
                                         onCheckPayment: store.checkPaymentAgain,
                                         onDismiss: store.dismissCheckoutStatus)
                        .padding(16)
                    Spacer()
                    EmptyState(title: "Your basket is empty",
                               message: "Browse deals and add items to get started.",
                               systemImage: "cart",
                               actionLabel: "Continue shopping",
                               onAction: { router.open(.home) })
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        CheckoutStatusBanner(state: store.checkoutState,
                                             onCheckPayment: store.checkPaymentAgain,
                                             onDismiss: store.dismissCheckoutStatus)
                        ForEach(store.cartLines) { line in
                            CartLineRow(
                                line: line,
                                onIncrement: { store.increment(productId: line.product.id) },
                                onDecrement: { store.decrement(productId: line.product.id) },
                                onRemove: { store.removeFromCart(productId: line.product.id) },
                                onOpen: { router.push(.product(id: line.product.id)) }
                            )
                        }
                    }
                    .padding(16)
                }
                .safeAreaInset(edge: .bottom) { totalPanel }
            }
        }
        .background(Color.pageBackground)
        .navigationTitle("My basket")
        .navigationBarTitleDisplayMode(.inline)
        .brandNavigationBar()
    }

    private var totalPanel: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Total").font(.tTitle.weight(.regular)).foregroundColor(.foreground)
                Spacer()
                Text(PriceFormat.format(store.cartTotal))
                    .font(.tTitleLarge.weight(.bold))
                    .foregroundColor(.brandGreen)
            }
            PrimaryButton(
                title: checkoutLabel,
                loading: store.checkoutState.isBusy,
                enabled: !store.checkoutState.isBusy,
                // Not signed in -> sign-in sheet (StoreEvent.requireLogin); otherwise opens Hubtel.
                action: store.startCheckout
            )
            HStack(spacing: 4) {
                Text("Secure payment with Hubtel ·")
                    .foregroundColor(.muted)
                Button("Privacy Policy") { router.push(.privacy) }
                    .foregroundColor(.brandGreen)
            }
            .font(.tBodySmall)
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 8)
        .background(
            UnevenTopRoundedRectangle(radius: 20)
                .fill(Color.white)
                .shadow(color: .black.opacity(0.1), radius: 8, y: -2)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    private var checkoutLabel: String {
        switch store.checkoutState {
        case .starting: return "Opening Hubtel…"
        case .verifying: return "Checking payment…"
        default: return "Checkout"
        }
    }
}

/// Rectangle with only the top corners rounded (UnevenRoundedRectangle is iOS 17+).
private struct UnevenTopRoundedRectangle: Shape {
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        Path(UIBezierPath(roundedRect: rect, byRoundingCorners: [.topLeft, .topRight],
                          cornerRadii: CGSize(width: radius, height: radius)).cgPath)
    }
}

/// Shows what happened with the last Hubtel attempt (errors, pending, verifying).
private struct CheckoutStatusBanner: View {
    let state: CheckoutState
    let onCheckPayment: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        switch state {
        case .idle, .starting, .paid:
            EmptyView()
        case .awaitingPayment:
            StatusCard(systemImage: "hourglass", tint: .brandGreen, container: .promoStripStart,
                       message: "Complete your payment on the Hubtel page, then come back to Tamaade.",
                       actionLabel: "I've paid — check", onAction: onCheckPayment, onDismiss: onDismiss)
        case .verifying:
            StatusCard(systemImage: nil, tint: .brandGreen, container: .promoStripStart,
                       message: "Checking your payment with Hubtel…", loading: true)
        case let .pending(_, message):
            StatusCard(systemImage: "hourglass", tint: .brandGreen, container: .promoStripStart,
                       message: message, actionLabel: "Check again", onAction: onCheckPayment, onDismiss: onDismiss)
        case let .failed(message):
            StatusCard(systemImage: "exclamationmark.circle", tint: .errorRed, container: Color.errorRed.opacity(0.08),
                       message: message, onDismiss: onDismiss)
        }
    }
}

private struct StatusCard: View {
    let systemImage: String?
    let tint: Color
    let container: Color
    let message: String
    var loading = false
    var actionLabel: String? = nil
    var onAction: () -> Void = {}
    var onDismiss: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .trailing, spacing: 4) {
            HStack(spacing: 10) {
                if loading {
                    ProgressView().tint(tint)
                } else if let systemImage {
                    Image(systemName: systemImage).foregroundColor(tint)
                }
                Text(message)
                    .font(.tBody)
                    .foregroundColor(.foreground)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if onDismiss != nil || actionLabel != nil {
                HStack(spacing: 16) {
                    if let onDismiss {
                        Button("Dismiss", action: onDismiss).foregroundColor(.muted)
                    }
                    if let actionLabel {
                        Button(actionLabel, action: onAction)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.brandGreen)
                    }
                }
                .font(.system(size: 15))
                .padding(.vertical, 6)
            }
        }
        .padding(.horizontal, 14)
        .padding(.top, 12)
        .padding(.bottom, loading ? 12 : 4)
        .background(container)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct PaymentSuccess: View {
    let orderId: Int?
    let onContinueShopping: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: "checkmark.circle")
                .font(.system(size: 64))
                .foregroundColor(.brandGreen)
            Text("Payment successful")
                .font(.tTitleLarge.weight(.bold))
                .foregroundColor(.foreground)
                .padding(.top, 16)
            Text("Thank you for shopping with Tamaade." +
                 (orderId.map { " Your order #\($0) is confirmed." } ?? " Your order is confirmed."))
                .font(.tBody)
                .foregroundColor(.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            Button("Continue shopping", action: onContinueShopping)
                .buttonStyle(.borderedProminent)
                .tint(.brandGreen)
                .padding(.top, 24)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct CartLineRow: View {
    let line: CartLine
    let onIncrement: () -> Void
    let onDecrement: () -> Void
    let onRemove: () -> Void
    let onOpen: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onOpen) {
                RemoteImage(url: line.product.image, contentMode: .fill)
                    .frame(width: 72, height: 72)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .buttonStyle(.plain)
            VStack(alignment: .leading, spacing: 2) {
                Text(line.product.name)
                    .font(.tTitle.weight(.regular))
                    .foregroundColor(.foreground)
                    .lineLimit(2)
                Text(PriceFormat.format(line.product.price))
                    .font(.tBody.weight(.semibold))
                    .foregroundColor(.brandGreen)
                HStack(spacing: 12) {
                    StepButton(symbol: "minus", label: "Decrease quantity", action: onDecrement)
                    Text("\(line.quantity)")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.foreground)
                        .frame(minWidth: 20)
                    StepButton(symbol: "plus", label: "Increase quantity", action: onIncrement)
                }
                .padding(.top, 6)
            }
            Spacer(minLength: 0)
            Button(action: onRemove) {
                Image(systemName: "trash")
                    .foregroundColor(.muted)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Remove")
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct StepButton: View {
    let symbol: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.brandGreenDark)
                .frame(width: 32, height: 32)
                .background(Color.brandGreen.opacity(0.12))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
