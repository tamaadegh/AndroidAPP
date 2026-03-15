package com.example.tamaade.data.repository

import com.example.tamaade.api.SimpleGraphQLClient
import com.example.tamaade.data.remote.model.Category

/**
 * Alternative CategoryRepository using SimpleGraphQLClient (no Apollo required)
 * This is a lightweight alternative if you don't want to use Apollo
 */
class SimpleCategoryRepository {
    
    private val graphQLClient = SimpleGraphQLClient()
    
    /**
     * Fetches categories from GraphQL API using simple HTTP client
     * @param first Number of categories to fetch (default: 10)
     * @return Result containing list of categories or error
     */
    suspend fun getCategories(first: Int = 10): Result<List<Category>> {
        return graphQLClient.getCategories(first)
    }
}
