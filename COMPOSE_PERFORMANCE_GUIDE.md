# 🎨 Jetpack Compose Performance Patterns

## 📊 How to Read Compose Metrics

After building, check `app/build/compose_metrics/` for these files:

### 1. `*-composables.txt` - Detailed Analysis
```
restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun ProductCard(
  stable product: Product
  stable onClick: Function0<Unit>
)
```

**Key Terms:**
- **restartable:** Can recompose independently
- **skippable:** Can skip recomposition if inputs unchanged
- **stable:** Parameters won't change unexpectedly
- **unstable:** Parameters might change (causes recomposition)

---

### 2. `*-classes.txt` - Stability Report
```
unstable class Product {
  stable val id: String
  unstable val price: Double
  runtime val name: String
}
```

**Stability Rules:**
- **Primitives:** Always stable (Int, String, Boolean, etc.)
- **Data classes:** Stable if all properties are stable
- **Mutable collections:** UNSTABLE (List, MutableList, etc.)
- **Interfaces:** UNSTABLE by default

---

## 🚨 Common Compose Anti-Patterns

### ❌ Anti-Pattern 1: Unstable Parameters
```kotlin
@Composable
fun ProductList(products: List<Product>) {  // List is UNSTABLE!
    LazyColumn {
        items(products) { product ->
            ProductCard(product)
        }
    }
}
```

**Problem:** `List` is unstable → recomposes even when data unchanged

**✅ Fix:** Use `ImmutableList` or mark stable
```kotlin
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ProductList(products: ImmutableList<Product>) {
    LazyColumn {
        items(products) { product ->
            ProductCard(product)
        }
    }
}

// Or mark your data class as immutable
@Immutable
data class Product(
    val id: String,
    val name: String,
    val price: Double
)
```

**Dependency:**
```gradle
implementation "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7"
```

---

### ❌ Anti-Pattern 2: Lambda Allocations
```kotlin
@Composable
fun ProductCard(product: Product, onAddToCart: (Product) -> Unit) {
    Button(onClick = { onAddToCart(product) }) {  // NEW LAMBDA EVERY RECOMPOSE!
        Text("Add to Cart")
    }
}
```

**Problem:** Creates new lambda on every recomposition

**✅ Fix:** Use `remember` with keys
```kotlin
@Composable
fun ProductCard(product: Product, onAddToCart: (Product) -> Unit) {
    val onClick = remember(product.id) {
        { onAddToCart(product) }
    }
    
    Button(onClick = onClick) {
        Text("Add to Cart")
    }
}
```

---

### ❌ Anti-Pattern 3: Heavy Calculations in Composition
```kotlin
@Composable
fun ProductPrice(product: Product) {
    val formattedPrice = formatCurrency(product.price)  // RUNS EVERY RECOMPOSE!
    Text(formattedPrice)
}
```

**Problem:** `formatCurrency` runs on every recomposition

**✅ Fix:** Use `remember`
```kotlin
@Composable
fun ProductPrice(product: Product) {
    val formattedPrice = remember(product.price) {
        formatCurrency(product.price)
    }
    Text(formattedPrice)
}
```

---

### ❌ Anti-Pattern 4: State Hoisting Abuse
```kotlin
@Composable
fun ProductScreen(viewModel: ProductViewModel = viewModel()) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val filters by viewModel.filters.collectAsState()
    
    // Entire screen recomposes when ANY state changes!
    ProductList(products, cart, favorites, filters)
}
```

**Problem:** All state in one composable → massive recomposition scope

**✅ Fix:** Split into smaller composables
```kotlin
@Composable
fun ProductScreen(viewModel: ProductViewModel = viewModel()) {
    Column {
        FilterBar(viewModel)  // Only recomposes when filters change
        ProductList(viewModel)  // Only recomposes when products change
        CartSummary(viewModel)  // Only recomposes when cart changes
    }
}

@Composable
private fun ProductList(viewModel: ProductViewModel) {
    val products by viewModel.products.collectAsState()
    LazyColumn {
        items(products) { product ->
            ProductCard(product)
        }
    }
}
```

---

### ❌ Anti-Pattern 5: Side Effects in Composition
```kotlin
@Composable
fun ProductCard(product: Product) {
    // WRONG! Runs on every recomposition
    logAnalyticsEvent("product_viewed", product.id)
    
    Card { /* ... */ }
}
```

**Problem:** Side effects run multiple times

**✅ Fix:** Use `LaunchedEffect`
```kotlin
@Composable
fun ProductCard(product: Product) {
    LaunchedEffect(product.id) {
        logAnalyticsEvent("product_viewed", product.id)
    }
    
    Card { /* ... */ }
}
```

---

### ❌ Anti-Pattern 6: Mutable State in Data Classes
```kotlin
data class Product(
    val id: String,
    var isFavorite: Boolean  // MUTABLE! Makes class UNSTABLE
)

@Composable
fun ProductCard(product: Product) {  // UNSTABLE parameter
    // Recomposes unnecessarily
}
```

**Problem:** Mutable properties make entire class unstable

**✅ Fix:** Use immutable data + separate state
```kotlin
data class Product(
    val id: String,
    val name: String,
    val price: Double
)

data class ProductUiState(
    val product: Product,
    val isFavorite: Boolean
)

@Composable
fun ProductCard(uiState: ProductUiState) {
    // Now stable and skippable
}
```

---

## 🎯 Compose Performance Best Practices

### 1. Use `derivedStateOf` for Computed Values
```kotlin
@Composable
fun ProductList(products: List<Product>, searchQuery: String) {
    val filteredProducts = remember(products, searchQuery) {
        derivedStateOf {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }.value
    
    LazyColumn {
        items(filteredProducts) { product ->
            ProductCard(product)
        }
    }
}
```

---

### 2. Use `key()` in Lists
```kotlin
@Composable
fun ProductList(products: List<Product>) {
    LazyColumn {
        items(
            items = products,
            key = { it.id }  // Helps Compose track items efficiently
        ) { product ->
            ProductCard(product)
        }
    }
}
```

---

### 3. Defer Reads with `Modifier.drawWithContent`
```kotlin
@Composable
fun ProductCard(product: Product) {
    var isVisible by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { isVisible = true }
    ) {
        if (isVisible) {
            // Only compose when visible
            ProductDetails(product)
        }
    }
}
```

---

### 4. Use `@Stable` and `@Immutable` Annotations
```kotlin
@Immutable
data class Product(
    val id: String,
    val name: String,
    val price: Double
)

@Stable
interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
}
```

---

### 5. Avoid Unnecessary State Reads
```kotlin
// ❌ BAD: Reads state in every item
@Composable
fun ProductList(viewModel: ProductViewModel) {
    val selectedId by viewModel.selectedProductId.collectAsState()
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                isSelected = product.id == selectedId  // Reads state!
            )
        }
    }
}

// ✅ GOOD: Pass state down
@Composable
fun ProductList(viewModel: ProductViewModel) {
    val selectedId by viewModel.selectedProductId.collectAsState()
    
    LazyColumn {
        items(products) { product ->
            key(product.id) {
                ProductCard(
                    product = product,
                    isSelected = product.id == selectedId
                )
            }
        }
    }
}
```

---

## 🔍 Debugging Recomposition

### Enable Recomposition Highlighting
```kotlin
// In your debug builds
@Composable
fun DebugProductCard(product: Product) {
    var recompositionCount by remember { mutableStateOf(0) }
    
    SideEffect {
        recompositionCount++
        Log.d("Recomposition", "ProductCard recomposed $recompositionCount times")
    }
    
    ProductCard(product)
}
```

---

### Use Layout Inspector
1. Run app in debug mode
2. Tools → Layout Inspector
3. Enable "Show Recomposition Counts"
4. Interact with app
5. Check which composables recompose most

**Red flags:**
- Recomposition count > 10 for static content
- Entire screen recomposes on small state change

---

## 📚 Recommended Dependencies

### Immutable Collections
```gradle
implementation "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7"
```

### Compose Runtime Tracing
```gradle
debugImplementation "androidx.compose.runtime:runtime-tracing:1.0.0-beta01"
```

---

## 🎓 Learning Resources

- [Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Compose Phases](https://developer.android.com/jetpack/compose/phases)
- [Compose Stability](https://developer.android.com/jetpack/compose/performance/stability)
- [Compose Metrics](https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md)

---

**Last Updated:** 2026-02-01  
**Next:** Analyze your Compose metrics after next build
