import XCTest
@testable import Tamaade

final class PhoneNumberTests: XCTestCase {
    func testNormalizesGhanaNumbers() {
        XCTAssertEqual(PhoneNumber.normalizeGhana("024 123 4567"), "+233241234567")
        XCTAssertEqual(PhoneNumber.normalizeGhana("+233-24-123-4567"), "+233241234567")
        XCTAssertEqual(PhoneNumber.normalizeGhana("233501234567"), "+233501234567")
        XCTAssertEqual(PhoneNumber.normalizeGhana("(020) 123 4567"), "+233201234567")
    }

    func testRejectsInvalidNumbers() {
        XCTAssertNil(PhoneNumber.normalizeGhana(""))
        XCTAssertNil(PhoneNumber.normalizeGhana("0141234567"))
        XCTAssertNil(PhoneNumber.normalizeGhana("02412345"))
        XCTAssertNil(PhoneNumber.normalizeGhana("+44241234567"))
    }

    func testCodeFilterKeepsSixDigits() {
        XCTAssertEqual(PhoneNumber.filterCode("12a34-5678"), "123456")
    }

    func testEmailValidation() {
        XCTAssertTrue(Validation.isEmail("ama@tamaade.com"))
        XCTAssertFalse(Validation.isEmail("ama@tamaade"))
        XCTAssertFalse(Validation.isEmail("ama tamaade.com"))
    }
}

final class PriceFormatTests: XCTestCase {
    func testFormatsLikeAndroid() {
        XCTAssertEqual(PriceFormat.format("1250"), "GH₵1,250")
        XCTAssertEqual(PriceFormat.format("19.90"), "GH₵19.9")
        XCTAssertEqual(PriceFormat.format(0.5), "GH₵0.5")
        XCTAssertEqual(PriceFormat.format("abc"), "GH₵abc")
    }
}

final class StoreLinkTests: XCTestCase {
    private let categories = [ProductCategory(id: 3, name: "Home & Kitchen", icon: nil)]

    func testTabsAndPages() {
        XCTAssertEqual(StoreLink.resolve("/deals", categories: categories), .deals)
        XCTAssertEqual(StoreLink.resolve("allcategories", categories: categories), .categories)
        XCTAssertEqual(StoreLink.resolve("/checkout", categories: categories), .cart)
        XCTAssertEqual(StoreLink.resolve("/login", categories: categories), .profile)
        XCTAssertEqual(StoreLink.resolve("/privacy", categories: categories), .privacy)
        XCTAssertEqual(StoreLink.resolve("/", categories: categories), .home)
    }

    func testFallbackForEmptyAndUnknown() {
        XCTAssertEqual(StoreLink.resolve(nil, categories: categories), .productList(ProductListQuery()))
        XCTAssertEqual(StoreLink.resolve("/nowhere", categories: categories, fallback: .deals), .deals)
    }

    func testProductsAndCategories() {
        XCTAssertEqual(StoreLink.resolve("/products/12", categories: categories), .product(id: 12))
        XCTAssertEqual(StoreLink.resolve("/category/3", categories: categories),
                       .productList(ProductListQuery(category: "Home & Kitchen")))
        XCTAssertEqual(StoreLink.resolve("/category/99", categories: categories), .categories)
        XCTAssertEqual(
            StoreLink.resolve("/products?category=Shoes&maxPrice=100.0&search=red+sneakers&ordering=-created_at",
                              categories: categories),
            .productList(ProductListQuery(category: "Shoes", maxPrice: 100, query: "red sneakers", sort: ProductSort.newest))
        )
        XCTAssertEqual(StoreLink.resolve("/products?filter=top-picks&q=bag", categories: categories),
                       .productList(ProductListQuery(query: "bag", sort: ProductSort.top)))
    }

    func testExternalLinks() {
        XCTAssertEqual(StoreLink.resolve("https://tamaade.com/x", categories: categories),
                       .external(URL(string: "https://tamaade.com/x")!))
    }
}

final class CatalogLogicTests: XCTestCase {
    private func product(_ id: Int, name: String = "Item", category: String = "", price: String = "10",
                         compare: String? = nil, discount: Int? = nil, promo: String = "",
                         createdAt: String? = nil, quantity: Int = 5) -> Product {
        Product(id: id, name: name, desc: "", category: category, seller: "", image: nil, images: [],
                price: price, compareAtPrice: compare, discountPercent: discount, promoLabel: promo,
                brand: "", isNew: false, isExpress: false, saleEndsAt: nil, quantity: quantity,
                createdAt: createdAt)
    }

    func testDiscountRules() {
        XCTAssertEqual(product(1, price: "75", compare: "100").effectiveDiscount, 25)
        XCTAssertNil(product(1, price: "100", compare: "90").originalPrice)
        XCTAssertEqual(product(1, discount: 40).effectiveDiscount, 40)
        XCTAssertTrue(product(1, promo: "Flash sale").isDeal)
        XCTAssertFalse(product(1).isDeal)
    }

    func testFilteringMatchesWeb() {
        let items = [
            product(1, name: "Red kettle", category: "Home & Kitchen", price: "120"),
            product(2, name: "Blue shoes", category: "Shoes", price: "300"),
            product(3, name: "Pan", category: "Kitchen", price: "80"),
        ]
        XCTAssertEqual(filterProducts(items, category: "kitchen").map(\.id), [1, 3])
        XCTAssertEqual(filterProducts(items, maxPrice: 150).map(\.id), [1, 3])
        XCTAssertEqual(filterProducts(items, query: " SHOES ").map(\.id), [2])
    }

    func testNewestSection() {
        let items = [
            product(1, createdAt: "2026-01-01"),
            product(2, createdAt: "2026-03-01"),
            product(3, createdAt: nil),
        ]
        XCTAssertEqual(productsForSection("newest", items).map(\.id), [2, 1, 3])
        XCTAssertEqual(productsForSection("featured", items).map(\.id), [1, 2, 3])
    }
}

final class ParsingTests: XCTestCase {
    private let api = TamaadeAPI(baseURL: "https://api.example.com")

    func testParsesProductWithGalleryAndMediaPaths() throws {
        let json: JSONObject = [
            "id": "7",
            "name": "Kettle",
            "price": 120.5,
            "quantity": 3,
            "is_new": true,
            "promo_label": "  Hot  ",
            "images": [
                ["url": "/media/b.jpg", "is_primary": false, "order": 2],
                ["url": "/media/a.jpg", "is_primary": true, "order": 5],
                ["url": "https://cdn.example.com/c.jpg", "is_primary": false, "order": 1],
            ],
        ]
        let product = try XCTUnwrap(api.parseProduct(json))
        XCTAssertEqual(product.id, 7)
        XCTAssertEqual(product.price, "120.5")
        XCTAssertEqual(product.promoLabel, "Hot")
        XCTAssertEqual(product.images, [
            "https://api.example.com/media/a.jpg",
            "https://cdn.example.com/c.jpg",
            "https://api.example.com/media/b.jpg",
        ])
        XCTAssertEqual(product.image, "https://api.example.com/media/a.jpg")
    }

    func testProductRoundTripsThroughBasketJSON() throws {
        let original = try XCTUnwrap(api.parseProduct([
            "id": 1, "name": "Bag", "price": "50.00", "compare_at_price": "80.00",
            "image": "https://cdn.example.com/bag.jpg", "quantity": 2,
        ]))
        let data = try JSONSerialization.data(withJSONObject: TamaadeAPI.productJSON(original))
        let decoded = try XCTUnwrap(JSON.parse(data) as? JSONObject)
        XCTAssertEqual(api.parseProduct(decoded), original)
    }

    func testErrorMessages() {
        XCTAssertEqual(APIErrorParser.message(body: ["detail": "Nope"], status: 400), "Nope")
        XCTAssertEqual(APIErrorParser.message(body: ["non_field_errors": ["Bad", "login"]], status: 400), "Bad login")
        XCTAssertEqual(APIErrorParser.message(body: ["phone_number": ["Already used."], "code": "x"], status: 400),
                       "Phone number: Already used.")
        XCTAssertEqual(APIErrorParser.message(body: ["errors": ["password": ["Too short.", "Too common."]]], status: 400),
                       "Too short. Too common.")
        XCTAssertEqual(APIErrorParser.message(body: ["detail": "x"], status: 401), APIError.sessionExpiredMessage)
        XCTAssertEqual(APIErrorParser.message(body: nil, status: 404), "This feature isn't available on the server yet.")
        XCTAssertEqual(APIErrorParser.message(body: nil, status: 502), "Tamaade server error (502). Please try again shortly.")
    }

    func testPolicyParsing() {
        let blocks = PolicyBlock.parse("## Title\nFirst line\nsecond line\n- one\n\n- two\nAfter")
        XCTAssertEqual(blocks, [
            .heading("Title"),
            .paragraph("First line second line"),
            .bullet("one"),
            .bullet("two"),
            .paragraph("After"),
        ])
    }

    func testBaseURLNormalization() {
        XCTAssertEqual(AppConfig.normalizedBase("https://x.onrender.com"), "https://x.onrender.com/")
        XCTAssertEqual(AppConfig.normalizedBase("http://localhost:8000//"), "http://localhost:8000/")
        XCTAssertNil(AppConfig.normalizedBase("$(API_BASE_URL)"))
        XCTAssertNil(AppConfig.normalizedBase("ftp://x"))
    }
}
