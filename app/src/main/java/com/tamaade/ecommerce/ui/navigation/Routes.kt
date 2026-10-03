package com.tamaade.ecommerce.ui.navigation

import android.net.Uri

/** Mirrors TamaadeWeb mobileNavItems */
sealed class TopLevelDestination(
    val route: String,
    val label: String
) {
    data object Home : TopLevelDestination("home", "Home")
    data object Categories : TopLevelDestination("categories", "Category")
    data object Deals : TopLevelDestination("deals", "Deals")
    data object Cart : TopLevelDestination("cart", "Cart")
    data object Profile : TopLevelDestination("profile", "Me")

    companion object {
        val entries = listOf(Home, Categories, Deals, Cart, Profile)
    }
}

object Routes {
    const val Splash = "splash"
    const val ProductDetail = "product/{productId}"
    const val ProductList = "product_list?category={category}&maxPrice={maxPrice}&q={q}&sort={sort}"
    /** Optional ?phone= prefills the phone number (e.g. "Create an account" for an unknown number). */
    const val Login = "login?phone={phone}"
    const val SignUp = "signup?phone={phone}"
    const val Privacy = "privacy"
    const val DeleteAccount = "delete_account"
    const val EditProfile = "edit_profile"

    fun productDetail(id: Int) = "product/$id"

    fun login(phone: String = "") = "login?phone=${Uri.encode(phone)}"

    fun signUp(phone: String = "") = "signup?phone=${Uri.encode(phone)}"

    /** Arguments are URL-encoded so names like "Home & Kitchen" survive the route. */
    fun productList(
        category: String = "",
        maxPrice: Int = -1,
        q: String = "",
        sort: String = ""
    ) = "product_list?category=${Uri.encode(category)}&maxPrice=$maxPrice&q=${Uri.encode(q)}&sort=${Uri.encode(sort)}"
}
