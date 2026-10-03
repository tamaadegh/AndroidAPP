package com.tamaade.ecommerce

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tamaade.ecommerce.data.CatalogViewModel
import com.tamaade.ecommerce.data.filterProducts
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.data.StoreEvent
import com.tamaade.ecommerce.data.StoreViewModel
import com.tamaade.ecommerce.ui.auth.LoginScreen
import com.tamaade.ecommerce.ui.auth.SignUpScreen
import com.tamaade.ecommerce.ui.cart.CartScreen
import com.tamaade.ecommerce.ui.categories.CategoriesScreen
import com.tamaade.ecommerce.ui.components.StoreTopBar
import com.tamaade.ecommerce.ui.components.TamaadeBottomBar
import com.tamaade.ecommerce.ui.deals.DealsScreen
import com.tamaade.ecommerce.ui.home.HomeScreen
import com.tamaade.ecommerce.ui.navigation.Routes
import com.tamaade.ecommerce.ui.navigation.StoreLink
import com.tamaade.ecommerce.ui.navigation.resolveStoreLink
import com.tamaade.ecommerce.ui.navigation.TopLevelDestination
import com.tamaade.ecommerce.ui.privacy.PrivacyScreen
import com.tamaade.ecommerce.ui.product.ProductDetailScreen
import com.tamaade.ecommerce.ui.product.ProductListScreen
import com.tamaade.ecommerce.ui.profile.DeleteAccountScreen
import com.tamaade.ecommerce.ui.profile.EditProfileScreen
import com.tamaade.ecommerce.ui.profile.ProfileScreen
import com.tamaade.ecommerce.ui.splash.SplashScreen
import com.tamaade.ecommerce.ui.util.formatPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TamaadeApp(
    store: StoreViewModel = viewModel(),
    catalog: CatalogViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }
    val showTopBar = showBottomBar && currentRoute != TopLevelDestination.Cart.route

    fun openProductList(category: String = "", maxPrice: Int = -1, q: String = "", sort: String = "") {
        navController.navigate(Routes.productList(category, maxPrice, q, sort))
    }

    fun openTab(dest: TopLevelDestination) {
        navController.navigate(dest.route) {
            popUpTo(TopLevelDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Admin-configured links (banners, tiles, promos, "view all") -> app screens. */
    fun openLink(link: String?, default: String) {
        val target = resolveStoreLink(link?.ifBlank { null } ?: default, catalog.data.categories)
        when (target) {
            StoreLink.Home -> openTab(TopLevelDestination.Home)
            StoreLink.Deals -> openTab(TopLevelDestination.Deals)
            StoreLink.Categories -> openTab(TopLevelDestination.Categories)
            StoreLink.Cart -> openTab(TopLevelDestination.Cart)
            StoreLink.Profile -> openTab(TopLevelDestination.Profile)
            StoreLink.Privacy -> navController.navigate(Routes.Privacy)
            is StoreLink.Product -> navController.navigate(Routes.productDetail(target.id))
            is StoreLink.ProductList ->
                openProductList(target.category, target.maxPrice, target.query, target.sort)
            is StoreLink.External -> openInBrowser(context, target.url)
        }
    }

    // Keep the saved basket in line with the live catalog (removed products, new prices).
    LaunchedEffect(catalog.productsVersion) {
        if (catalog.loadedOnce) store.syncCartWithCatalog(catalog.data.products)
    }

    fun showCart() {
        if (navController.currentDestination?.route == Routes.Splash) {
            navController.navigate(TopLevelDestination.Home.route) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        }
        navController.navigate(TopLevelDestination.Cart.route) {
            popUpTo(TopLevelDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(store) {
        store.events.collect { event ->
            when (event) {
                is StoreEvent.Message -> scope.launch { snackbarHostState.showSnackbar(event.text) }
                is StoreEvent.OpenUrl -> {
                    if (!openInBrowser(context, event.url)) {
                        scope.launch { snackbarHostState.showSnackbar("No browser found to open Hubtel checkout") }
                    }
                }
                StoreEvent.RequireLogin -> navController.navigate(Routes.login()) { launchSingleTop = true }
                StoreEvent.ShowCart -> showCart()
            }
        }
    }

    fun addProduct(product: Product) {
        store.addToCart(product)
        scope.launch { snackbarHostState.showSnackbar("${product.name} added to basket") }
    }

    fun addProductById(id: Int) {
        catalog.data.productById(id)?.let { addProduct(it) }
    }

    /** Leaves the auth screens (Login and any Sign up on top of it) back to where the user came from. */
    fun onSignedIn(message: String) {
        val route = navController.currentDestination?.route
        if (route == Routes.Login || route == Routes.SignUp) {
            if (!navController.popBackStack(Routes.Login, inclusive = true)) navController.popBackStack()
            if (navController.currentDestination?.route == Routes.SignUp) navController.popBackStack()
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                StoreTopBar(
                    onSearchClick = { openProductList() }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                TamaadeBottomBar(
                    currentRoute = currentRoute,
                    cartCount = store.cartCount,
                    onNavigate = ::openTab
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Splash) {
                LaunchedEffect(Unit) {
                    delay(1400)
                    navController.navigate(TopLevelDestination.Home.route) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
                SplashScreen()
            }

            composable(TopLevelDestination.Home.route) {
                HomeScreen(
                    catalog = catalog.data,
                    loading = catalog.loading,
                    refreshing = catalog.refreshing,
                    error = catalog.error,
                    onRefresh = catalog::refresh,
                    onRetry = catalog::retry,
                    onProductClick = { navController.navigate(Routes.productDetail(it)) },
                    onAddToCart = { addProductById(it) },
                    onOpenLink = ::openLink,
                    onOpenMaxPrice = { openProductList(maxPrice = it) }
                )
            }

            composable(TopLevelDestination.Categories.route) {
                CategoriesScreen(
                    categories = catalog.data.categories,
                    loading = catalog.loading,
                    refreshing = catalog.refreshing,
                    error = catalog.error,
                    onRefresh = catalog::refresh,
                    onRetry = catalog::retry,
                    onCategoryClick = { openProductList(category = it) }
                )
            }

            composable(TopLevelDestination.Deals.route) {
                DealsScreen(
                    catalog = catalog.data,
                    loading = catalog.loading,
                    refreshing = catalog.refreshing,
                    error = catalog.error,
                    onRefresh = catalog::refresh,
                    onRetry = catalog::retry,
                    onProductClick = { navController.navigate(Routes.productDetail(it)) },
                    onAddToCart = { addProductById(it) },
                    onOpenLink = ::openLink
                )
            }

            composable(TopLevelDestination.Cart.route) {
                CartScreen(
                    lines = store.cartLines,
                    total = store.cartTotal,
                    checkoutState = store.checkoutState,
                    onIncrement = { id ->
                        val line = store.cartLines.find { it.product.id == id } ?: return@CartScreen
                        store.setQuantity(id, line.quantity + 1)
                    },
                    onDecrement = { id ->
                        val line = store.cartLines.find { it.product.id == id } ?: return@CartScreen
                        store.setQuantity(id, line.quantity - 1)
                    },
                    onRemove = store::removeFromCart,
                    // Not signed in -> Login (via StoreEvent.RequireLogin); otherwise opens Hubtel in the browser.
                    onCheckout = store::startCheckout,
                    onCheckPayment = store::checkPaymentAgain,
                    onDismissStatus = store::dismissCheckoutStatus,
                    onOpenPrivacy = { navController.navigate(Routes.Privacy) },
                    onContinueShopping = {
                        navController.navigate(TopLevelDestination.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(TopLevelDestination.Profile.route) {
                ProfileScreen(
                    user = store.user,
                    onLogin = { navController.navigate(Routes.login()) },
                    onLogout = {
                        store.logout()
                        scope.launch { snackbarHostState.showSnackbar("Signed out") }
                    },
                    onOpenCart = {
                        navController.navigate(TopLevelDestination.Cart.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    musicEnabled = store.musicEnabled,
                    onMusicEnabledChange = store::updateMusicEnabled,
                    onOpenPrivacy = { navController.navigate(Routes.Privacy) },
                    onDeleteAccount = { navController.navigate(Routes.DeleteAccount) },
                    onEditDetails = { navController.navigate(Routes.EditProfile) }
                )
            }

            composable(Routes.EditProfile) {
                LaunchedEffect(Unit) { store.clearProfileError() }
                val signedIn = store.user
                if (signedIn == null) {
                    // Signed out (e.g. session expired) while on this screen.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                EditProfileScreen(
                    user = signedIn,
                    saving = store.savingProfile,
                    error = store.profileError,
                    fieldErrors = store.profileFieldErrors,
                    onClearError = store::clearProfileError,
                    onSave = { firstName, lastName, email, phone ->
                        store.updateProfile(firstName, lastName, email, phone) {
                            navController.popBackStack()
                            scope.launch { snackbarHostState.showSnackbar("Details updated") }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DeleteAccount) {
                LaunchedEffect(Unit) { store.clearDeleteAccountError() }
                DeleteAccountScreen(
                    email = store.user?.email,
                    phoneNumber = store.user?.phoneNumber,
                    deleting = store.deletingAccount,
                    error = store.deleteAccountError,
                    onClearError = store::clearDeleteAccountError,
                    onConfirm = { password ->
                        store.deleteAccount(password) { message ->
                            navController.navigate(TopLevelDestination.Home.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ProductDetail,
                arguments = listOf(navArgument("productId") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("productId") ?: return@composable
                var fetching by remember(id) { mutableStateOf(true) }
                val product by produceState(catalog.data.productById(id), id, catalog.productsVersion) {
                    if (value == null) value = catalog.findProduct(id)
                    fetching = false
                }
                ProductDetailScreen(
                    product = product,
                    loading = product == null && (catalog.loading || fetching),
                    onBack = { navController.popBackStack() },
                    onAddToCart = { store.addToCart(it) }
                )
            }

            composable(
                route = Routes.ProductList,
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("maxPrice") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("q") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("sort") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val category = entry.arguments?.getString("category").orEmpty()
                val maxPrice = entry.arguments?.getInt("maxPrice") ?: -1
                val initialQuery = entry.arguments?.getString("q").orEmpty()
                val sort = entry.arguments?.getString("sort").orEmpty()
                var query by rememberSaveable { mutableStateOf(initialQuery) }
                val products = filterProducts(catalog.data.products, category, maxPrice, query, sort)
                val title = when {
                    category.isNotBlank() -> category
                    maxPrice > 0 -> "Under ${formatPrice(maxPrice.toDouble())}"
                    else -> "All products"
                }
                ProductListScreen(
                    title = title,
                    products = products,
                    query = query,
                    onQueryChange = { query = it },
                    loading = catalog.loading,
                    error = catalog.error,
                    onRetry = catalog::retry,
                    onProductClick = { navController.navigate(Routes.productDetail(it)) },
                    onAddToCart = { addProductById(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.Login,
                arguments = listOf(
                    navArgument("phone") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                LaunchedEffect(Unit) { store.clearAuthError() }
                LoginScreen(
                    loading = store.authLoading,
                    error = store.authError,
                    errorCode = store.authErrorCode,
                    initialPhone = entry.arguments?.getString("phone").orEmpty(),
                    otpCooldownPhone = store.otpCooldownPhone,
                    otpCooldownUntil = store.otpCooldownUntil,
                    onBack = { navController.popBackStack() },
                    onSubmit = { email, password ->
                        store.login(email, password) { onSignedIn("Signed in") }
                    },
                    onRequestCode = { phone, onSent ->
                        store.requestOtp(phone) { onSent(it.phoneNumber) }
                    },
                    onVerifyCode = { phone, code ->
                        store.verifyOtp(phone, code) { onSignedIn("Signed in") }
                    },
                    onClearError = store::clearAuthError,
                    onSignUp = { phone ->
                        navController.navigate(Routes.signUp(phone)) {
                            popUpTo(Routes.SignUp) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.SignUp,
                arguments = listOf(
                    navArgument("phone") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                LaunchedEffect(Unit) { store.clearAuthError() }
                SignUpScreen(
                    loading = store.authLoading,
                    error = store.authError,
                    fieldErrors = store.authFieldErrors,
                    initialPhone = entry.arguments?.getString("phone").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onSubmit = { firstName, lastName, email, phone, password ->
                        store.register(firstName, lastName, password, email, phone) {
                            onSignedIn("Account created — welcome to Tamaade!")
                        }
                    },
                    onClearError = store::clearAuthError,
                    onSignIn = { phone ->
                        navController.navigate(Routes.login(phone)) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                    onOpenPrivacy = { navController.navigate(Routes.Privacy) }
                )
            }

            composable(Routes.Privacy) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun openInBrowser(context: Context, url: String): Boolean = try {
    // NEW_TASK keeps the browser in its own task, so the tamaade://checkout redirect
    // brings this (singleTop) activity back via onNewIntent instead of stacking a new one.
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (e: ActivityNotFoundException) {
    false
}
