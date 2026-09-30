package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ServiceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ServiceViewModel = viewModel()
            val companyProfile by viewModel.companyProfile.collectAsState()
            val colorTheme = companyProfile?.colorTheme ?: "NEO_BLUE"
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (companyProfile?.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDark, colorTheme = colorTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeoServiceAppRouter(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NeoServiceAppRouter(viewModel: ServiceViewModel) {
    val navController = rememberNavController()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val startDest = if (isOnboardingCompleted) "welcome" else "getting_started"

    NavHost(navController = navController, startDestination = startDest) {
        composable("getting_started") {
            GettingStartedScreen(
                viewModel = viewModel,
                onOnboardingComplete = { role ->
                    when (role) {
                        "ADMIN" -> navController.navigate("admin_dashboard") {
                            popUpTo("getting_started") { inclusive = true }
                        }
                        "TECHNICIAN" -> navController.navigate("tech_dashboard") {
                            popUpTo("getting_started") { inclusive = true }
                        }
                        else -> navController.navigate("welcome") {
                            popUpTo("getting_started") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("welcome") {
            WelcomeScreen(
                viewModel = viewModel,
                onNavigateStaffLogin = { navController.navigate("staff_login") },
                onNavigateCustomerPortal = { navController.navigate("customer_portal") },
                onNavigateCreateTicket = { navController.navigate("create_ticket") },
                onNavigateDailySummary = { navController.navigate("daily_summary") }
            )
        }
        composable("staff_login") {
            StaffLoginScreen(
                viewModel = viewModel,
                onAdminLoggedIn = { navController.navigate("admin_dashboard") { popUpTo("welcome") } },
                onTechLoggedIn = { navController.navigate("tech_dashboard") { popUpTo("welcome") } },
                onBack = { navController.popBackStack() }
            )
        }
        composable("customer_portal") {
            CustomerPortalScreen(
                viewModel = viewModel,
                onNavigateCreateTicket = { navController.navigate("create_ticket") },
                onTicketClick = { ticket ->
                    viewModel.selectTicket(ticket)
                    navController.navigate("ticket_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onNavigateCreateTicket = { navController.navigate("create_ticket") },
                onNavigateDailySummary = { navController.navigate("daily_summary") },
                onNavigateUserManagement = { navController.navigate("user_management") },
                onNavigateDatabaseManagement = { navController.navigate("database_management") },
                onNavigateCompanySettings = { navController.navigate("company_settings") },
                onTicketClick = { ticket ->
                    viewModel.selectTicket(ticket)
                    navController.navigate("ticket_detail")
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }
        composable("daily_summary") {
            DailySummaryReportScreen(
                viewModel = viewModel,
                onTicketClick = { ticket ->
                    viewModel.selectTicket(ticket)
                    navController.navigate("ticket_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("user_management") {
            UserManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("database_management") {
            DatabaseManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("tech_dashboard") {
            TechnicianDashboardScreen(
                viewModel = viewModel,
                onTicketClick = { ticket ->
                    viewModel.selectTicket(ticket)
                    navController.navigate("ticket_detail")
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }
        composable("ticket_detail") {
            TicketDetailScreen(
                viewModel = viewModel,
                onNavigateQuotationPreview = { navController.navigate("quotation_preview") },
                onNavigateInvoicePreview = { navController.navigate("invoice_preview") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("create_ticket") {
            CreateTicketScreen(
                viewModel = viewModel,
                onTicketCreated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable("company_settings") {
            CompanySettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("quotation_preview") {
            QuotationPreviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("invoice_preview") {
            InvoicePreviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
