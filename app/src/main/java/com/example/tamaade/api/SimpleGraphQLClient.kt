package com.example.tamaade.api

import com.example.tamaade.data.remote.model.Category
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Simple GraphQL client using OkHttp (Alternative to Apollo)
 * Use this if you prefer a lightweight solution without Apollo dependencies
 */
class SimpleGraphQLClient {
    
    private val baseUrl = "https://dashboard-production-8b7f.up.railway.app/graphql/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val gson = Gson()
    
    /**
     * Fetch categories using GraphQL query
     */
    suspend fun getCategories(first: Int = 10): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val query = """
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
            """.trimIndent()
            
            val variables = mapOf("first" to first)
            
            val graphQLRequest = GraphQLRequest(query, variables)
            val jsonBody = gson.toJson(graphQLRequest)
            
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val graphQLResponse = gson.fromJson(responseBody, CategoryGraphQLResponse::class.java)
            
            if (graphQLResponse.errors != null && graphQLResponse.errors.isNotEmpty()) {
                val errorMessage = graphQLResponse.errors.joinToString(", ") { it.message }
                return@withContext Result.failure(Exception("GraphQL Error: $errorMessage"))
            }
            
            val categories = graphQLResponse.data?.categories?.edges?.mapNotNull { edge ->
                edge.node?.let { node ->
                    Category(
                        id = node.id,
                        name = node.name ?: "",
                        description = node.description,
                        icon = null
                    )
                }
            } ?: emptyList()
            
            Result.success(categories)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // GraphQL Request/Response Models
    private data class GraphQLRequest(
        val query: String,
        val variables: Map<String, Any>
    )
    
    private data class CategoryGraphQLResponse(
        val data: CategoryData?,
        val errors: List<GraphQLError>?
    )
    
    private data class CategoryData(
        val categories: CategoryConnection?
    )
    
    private data class CategoryConnection(
        val edges: List<CategoryEdge>?
    )
    
    private data class CategoryEdge(
        val node: CategoryNode?
    )
    
    private data class CategoryNode(
        val id: String,
        val name: String?,
        val description: String?
    )
    
    private data class GraphQLError(
        val message: String,
        val locations: List<ErrorLocation>? = null,
        val path: List<String>? = null
    )
    
    private data class ErrorLocation(
        val line: Int,
        val column: Int
    )
}
