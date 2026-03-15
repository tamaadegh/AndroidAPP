# Quick Start: Fetching Categories with GraphQL

## 🎯 The Better Way

Instead of manually constructing GraphQL queries in URLs, use **Apollo Android Client** for:
- ✅ Type-safe queries
- ✅ Automatic code generation
- ✅ Built-in caching
- ✅ Better error handling

## 📋 Quick Usage

### In ViewModel
```kotlin
private val categoryRepository = CategoryRepository()

fun loadCategories() {
    viewModelScope.launch {
        categoryRepository.getCategories(first = 10)
            .onSuccess { categories ->
                _categories.value = categories
            }
            .onFailure { error ->
                _errorMessage.value = error.message
            }
    }
}
```

### In Fragment/Activity
```kotlin
lifecycleScope.launch {
    val result = CategoryRepository().getCategories(first = 20)
    
    result.onSuccess { categories ->
        // Display categories
        adapter.submitList(categories)
    }
}
```

## 🔨 Build Steps

1. **Sync Gradle** - Apollo will generate code from `.graphql` files
2. **Build Project** - Generates `GetCategoriesQuery` class
3. **Run** - Start fetching categories!

## 📁 Files Created

- `app/src/main/graphql/com/example/tamaade/GetCategories.graphql` - GraphQL query
- `app/src/main/java/com/example/tamaade/api/GraphQLClient.kt` - Apollo client
- `app/src/main/java/com/example/tamaade/data/repository/CategoryRepository.kt` - Repository
- `app/src/main/java/com/example/tamaade/ui/categories/CategoryFragment.kt` - Example usage

## 📝 Files Modified

- `build.gradle` (project) - Added Apollo plugin
- `app/build.gradle` - Added Apollo dependencies & configuration
- `Category.kt` - Updated to include `description` field
- `ProductViewModel.kt` - Updated to use new CategoryRepository
- `CategoryAdapter.kt` - Updated to display description

## 🌐 Network Setup

### For Local Development (Android Emulator)
Update `GraphQLClient.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/graphql/"
```

### For Production
```kotlin
private const val BASE_URL = "https://tamaade.com/graphql/"
```

## 🚀 Next Steps

1. Sync and build the project
2. Test category fetching
3. Customize the query to fetch more fields
4. Implement UI to display categories

For detailed documentation, see `GRAPHQL_INTEGRATION.md`
