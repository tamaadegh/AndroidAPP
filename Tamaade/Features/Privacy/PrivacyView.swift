import SwiftUI

enum PolicyBlock: Equatable {
    case heading(String)
    case paragraph(String)
    case bullet(String)

    /// Admin-edited plain text: blank lines separate blocks, "## " lines are headings,
    /// "- " lines are bullets, everything else is paragraph text.
    static func parse(_ content: String) -> [PolicyBlock] {
        var blocks: [PolicyBlock] = []
        let normalized = content.replacingOccurrences(of: "\r\n", with: "\n")
        let chunks = normalized.components(separatedBy: "\n").reduce(into: [[String]]([[]])) { result, line in
            if line.trimmingCharacters(in: .whitespaces).isEmpty {
                result.append([])
            } else {
                result[result.count - 1].append(line)
            }
        }
        for chunk in chunks {
            var paragraph = ""
            func flush() {
                if !paragraph.isBlank { blocks.append(.paragraph(paragraph.trimmingCharacters(in: .whitespaces))) }
                paragraph = ""
            }
            for raw in chunk {
                let line = raw.trimmingCharacters(in: .whitespaces)
                if line.isEmpty { continue }
                if line.hasPrefix("## ") {
                    flush()
                    blocks.append(.heading(String(line.dropFirst(3)).trimmingCharacters(in: .whitespaces)))
                } else if line.hasPrefix("- ") {
                    flush()
                    blocks.append(.bullet(String(line.dropFirst(2)).trimmingCharacters(in: .whitespaces)))
                } else {
                    if !paragraph.isEmpty { paragraph += " " }
                    paragraph += line
                }
            }
            flush()
        }
        return blocks
    }

    /// Shown when the backend has no policy yet or can't be reached.
    static let fallback = PrivacyContent(
        title: "Privacy Policy",
        content: """
        ## What we collect
        Tamaade only collects your data when you shop with us. We use it to run your basket, orders, payment and delivery — nothing else.
        - Cart: the products and quantities you add to your basket.
        - Orders: what you bought, when, and the order status.
        - Checkout and payment: payment references and status from Hubtel. Your card or mobile money details are entered on Hubtel's secure page and are never stored by Tamaade.
        - Delivery details: your name, phone number and address so we can deliver your order.
        - Account: your name and email address when you create an account.
        ## How we use it
        We use this information only to process your orders, take payment, deliver your items and support you. We do not sell your personal data.
        ## Your choices
        You can browse Tamaade without an account. You can sign out at any time, and you can contact us to update or delete your account data.
        """,
        updatedAt: nil
    )
}

struct PrivacyView: View {
    @State private var policy: PrivacyContent?

    var body: some View {
        Group {
            if let policy {
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        if let updated = policy.updatedAt {
                            Text("Last updated \(String(updated.prefix(10)))")
                                .font(.tBodySmall)
                                .foregroundColor(.muted)
                        }
                        ForEach(Array(PolicyBlock.parse(policy.content).enumerated()), id: \.offset) { _, block in
                            blockView(block)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                }
            } else {
                ProgressView()
                    .tint(.brandGreen)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(Color.pageBackground)
        .navigationTitle(policy?.title.nonBlank ?? "Privacy Policy")
        .navigationBarTitleDisplayMode(.inline)
        .brandNavigationBar()
        .task {
            guard policy == nil else { return }
            if let remote = try? await TamaadeAPI.shared.privacy(), !remote.content.isBlank {
                policy = remote
            } else {
                policy = PolicyBlock.fallback
            }
        }
    }

    @ViewBuilder
    private func blockView(_ block: PolicyBlock) -> some View {
        switch block {
        case .heading(let text):
            Text(text)
                .font(.tTitle.weight(.bold))
                .foregroundColor(.brandGreen)
                .padding(.top, 8)
        case .paragraph(let text):
            Text(text)
                .font(.tBody)
                .foregroundColor(.foreground)
        case .bullet(let text):
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("•").foregroundColor(.brandGreen)
                Text(text).foregroundColor(.foreground)
            }
            .font(.tBody)
            .padding(.leading, 4)
        }
    }
}
