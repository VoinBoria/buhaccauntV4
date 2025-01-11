// DrawerContent.kt
package com.example.homeaccountingapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun DrawerContent(
    onNavigateToMainActivity: () -> Unit, // Додаємо новий параметр для головного меню
    onNavigateToIncomes: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIssuedOnLoan: () -> Unit,
    onNavigateToBorrowed: () -> Unit,
    onNavigateToAllTransactionIncome: () -> Unit,
    onNavigateToAllTransactionExpense: () -> Unit,
    onNavigateToBudgetPlanning: () -> Unit,
    onNavigateToTaskActivity: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val iconSize = when {
        screenWidthDp < 360.dp -> 20.dp  // Small screens
        screenWidthDp < 600.dp -> 24.dp  // Normal screens
        else -> 28.dp  // Large screens
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.8f)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            // Додаємо новий пункт меню для головного меню
            CategoryItem(
                text = "Головне меню",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Іконка головного меню",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToMainActivity,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Доходи",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_income),
                        contentDescription = "Іконка доходів",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToIncomes,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Витрати",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_expense),
                        contentDescription = "Іконка витрат",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToExpenses,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Всі транзакції доходів",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_all_income_transactions),
                        contentDescription = "Іконка всіх транзакцій доходів",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToAllTransactionIncome,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Всі транзакції витрат",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_all_expense_transactions),
                        contentDescription = "Іконка всіх транзакцій витрат",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToAllTransactionExpense,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Видано в борг",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loan_issued),
                        contentDescription = "Іконка виданих боргів",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToIssuedOnLoan,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Отримано в борг",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loan_borrowed),
                        contentDescription = "Іконка отриманих боргів",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToBorrowed,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Планування бюджету",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_budget_planning),
                        contentDescription = "Іконка планування бюджету",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToBudgetPlanning,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Пункт меню задачника
            CategoryItem(
                text = "Задачник",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_task),
                        contentDescription = "Іконка задачника",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToTaskActivity,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
        }
    }
}