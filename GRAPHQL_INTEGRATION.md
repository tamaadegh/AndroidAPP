# GraphQL Integration Guide - Categories API

## Overview
This guide explains how to fetch categories from your GraphQL API using Apollo Android client.

## 🚀 What's Been Set Up

### 1. **Apollo GraphQL Client** (`GraphQLClient.kt`)
- Configured Apollo client pointing to `http://localhost:8000/graphql/`
- Includes HTTP logging for debugging
- Singleton pattern for efficient resource usage

### 2. **GraphQL Query** (`GetCategories.graphql`)
```graphql
query GetCategories($first: Int!) {
  categories(first: $first) {
    edges {
      node {
        id
        name
        description
      }
    }
  }
}
```

### 3. **Category Repository** (`CategoryRepository.kt`)
- Clean separation of concerns
- Returns `Result<List<Category>>` for better error handling
- Handles Apollo exceptions gracefully

### 4. **Updated Category Model**
- Changed `id` from `Int` to `String` (GraphQL IDs are strings)
- Added `description` field
- Kept `icon` field for future use

## 📝 Usage Examples

### Basic Usage in ViewModel
```kotlin
class MyViewModel : ViewModel() {
    private val categoryRepository = CategoryRepository()
    
    fun loadCategories() {
        viewModelScope.launch {
            val result = categoryRepository.getCategories(first = 10)
            
            result.onSuccess { categories ->
                // Update UI with categories
                _categories.value = categories
            }.onFailure { error ->
                // Handle error
                _errorMessage.value = error.message
            }
        }
    }
}
```

### Usage in Fragment/Activity
```kotlin
lifecycleScope.launch {
    val result = categoryRepository.getCategories(first = 20)
    
    result.onSuccess { categories ->
        categories.forEach { category ->
            Log.d("Category", "${category.name}: ${category.description}")
        }
    }.onFailure { error ->
        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
    }
}
```

### With Loading States
```kotlin
fun fetchCategories() {
    viewModelScope.launch {
        _isLoading.value = true
        
        val result = categoryRepository.getCategories(first = 10)
        
        result.onSuccess { categories ->
            _categories.value = categories
            _isLoading.value = false
        }.onFailure { error ->
            _errorMessage.value = error.message
            _isLoading.value = false
        }
    }
}
```

## 🔧 Build Steps

1. **Sync Gradle**: The Apollo plugin will generate Kotlin code from your `.graphql` files
2. **Build Project**: This generates the `GetCategoriesQuery` class
3. **Run App**: Categories will be fetched from your GraphQL endpoint

## 📦 Dependencies Added

```gradle
// Apollo GraphQL
implementation 'com.apollographql.apollo3:apollo-runtime:3.8.2'
implementation 'com.apollographql.apollo3:apollo-normalized-cache:3.8.2'
```

## 🎯 Key Benefits

1. **Type Safety**: Apollo generates type-safe Kotlin classes from GraphQL queries
2. **Error Handling**: Built-in error handling with `Result` wrapper
3. **Caching**: Apollo provides automatic response caching
4. **Code Generation**: No manual JSON parsing needed
5. **GraphQL Native**: Built specifically for GraphQL APIs

## 🔄 Migration from REST to GraphQL

### Before (REST with Retrofit)
```kotlin
interface ProductApi {
    @GET("categories/")
    suspend fun getCategories(): List<Category>
}
```

### After (GraphQL with Apollo)
```kotlin
val result = categoryRepository.getCategories(first = 10)
result.onSuccess { categories -> /* use categories */ }
```

## 📱 Network Configuration

### For Localhost Testing
Make sure your `AndroidManifest.xml` has:
```xml
<application
    android:usesCleartextTraffic="true">
```

### For Production
Update the base URL in `GraphQLClient.kt`:
```kotlin
private const val BASE_URL = "https://tamaade.com/graphql/"
```

## 🐛 Troubleshooting

### Build Error: "Unresolved reference: GetCategoriesQuery"
**Solution**: Sync and rebuild the project. Apollo generates this class during build.

### Network Error: "Failed to connect"
**Solution**: 
- Ensure your GraphQL server is running on `localhost:8000`
- For Android emulator, use `10.0.2.2:8000` instead of `localhost:8000`
- Check `AndroidManifest.xml` has `usesCleartextTraffic="true"`

### GraphQL Error: "Field not found"
**Solution**: Verify your GraphQL schema matches the query fields

## 🎨 Customization

### Fetch More Categories
```kotlin
categoryRepository.getCategories(first = 50)
```

### Add More Fields to Query
Edit `GetCategories.graphql`:
```graphql
query GetCategories($first: Int!) {
  categories(first: $first) {
    edges {
      node {
        id
        name
        description
        slug
        backgroundImage {
          url
        }
      }
    }
  }
}
```

Then update `Category.kt` model accordingly.

## 📚 Additional Resources

- [Apollo Android Documentation](https://www.apollographql.com/docs/kotlin/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

## ✅ Next Steps

1. Sync Gradle files
2. Build the project to generate GraphQL code
3. Test the category fetching
4. Implement UI to display categories
5. Add caching strategy if needed
