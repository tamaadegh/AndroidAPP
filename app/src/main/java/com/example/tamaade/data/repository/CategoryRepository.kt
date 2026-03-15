package com.example.tamaade.data.repository

import com.apollographql.apollo3.exception.ApolloException
import com.example.tamaade.GetCategoriesQuery
import com.example.tamaade.api.GraphQLClient
import com.example.tamaade.data.remote.model.Category

class CategoryRepository {
    
    private val apolloClient = GraphQLClient.apolloClient
    
    /**
     * Fetches categories from GraphQL API
     * @param first Number of categories to fetch (default: 10)
     * @return List of Category objects
     */
    suspend fun getCategories(first: Int = 10): Result<List<Category>> {
        return try {
            val response = apolloClient.query(GetCategoriesQuery(first)).execute()
            
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown GraphQL error"))
            } else {
                val categories = response.data?.categories?.edges?.mapNotNull { edge ->
                    edge?.node?.let { node ->
                        Category(
                            id = node.id,
                            name = node.name ?: "",
                            description = node.description,
                            icon = null // Add icon if available in your GraphQL schema
                        )
                    }
                } ?: emptyList()
                
                Result.success(categories)
            }
        } catch (e: ApolloException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
