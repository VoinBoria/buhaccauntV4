package com.example.homeaccountingapp

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.ceil
import kotlinx.coroutines.*
import com.example.homeaccountingapp.DrawerContent
class BudgetPlanningActivity : ComponentActivity() {
    private val viewModel: BudgetPlanningViewModel by viewModels()

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("com.example.homeaccountingapp.UPDATE_EXPENSES" == intent.action) {
                viewModel.loadExpensesFromMainActivity(context)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@BudgetPlanningActivity, MainActivity::class.java).apply {
                                    putExtra("SHOW_SPLASH_SCREEN", false)
                                }
                                startActivity(intent)
                            },
                            onNavigateToIncomes = { navigateToActivity(IncomeActivity::class.java) },
                            onNavigateToExpenses = { navigateToActivity(ExpenseActivity::class.java) },
                            onNavigateToIssuedOnLoan = { navigateToActivity(IssuedOnLoanActivity::class.java) },
                            onNavigateToBorrowed = { navigateToActivity(BorrowedActivity::class.java) },
                            onNavigateToAllTransactionIncome = { navigateToActivity(AllTransactionIncomeActivity::class.java) },
                            onNavigateToAllTransactionExpense = { navigateToActivity(AllTransactionExpenseActivity::class.java) },
                            onNavigateToBudgetPlanning = { navigateToActivity(BudgetPlanningActivity::class.java) },
                            onNavigateToTaskActivity = { navigateToActivity(TaskActivity::class.java) }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Планування витрат", color = Color.White) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color.White)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                            )
                        },
                        content = { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .paint(
                                        painter = painterResource(id = R.drawable.background_app),
                                        contentScale = ContentScale.Crop
                                    )
                                    .padding(innerPadding)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Spacer(modifier = Modifier.height(40.dp))
                                    BudgetPlanningScreen(viewModel)
                                }
                            }
                        }
                    )
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, IntentFilter("com.example.homeaccountingapp.UPDATE_EXPENSES"))

        viewModel.loadExpenseCategories(this)
        viewModel.loadMaxExpenses(this)
        viewModel.loadExpensesFromMainActivity(this)
        viewModel.loadIncomesFromMainActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
    }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
}
class BudgetPlanningViewModel(application: Application) : AndroidViewModel(application) {
    val expenseCategories = MutableLiveData<Map<String, Double>>(emptyMap())
    val maxExpenses = MutableLiveData<Map<String, Double>>(emptyMap())
    val expenses = MutableLiveData<Map<String, Double>>(emptyMap())
    val incomes = MutableLiveData<Map<String, Double>>(emptyMap())
    val savedAmounts = MutableLiveData<List<Double>>(emptyList())
    val saveMessage = MutableLiveData<String?>(null)
    val isAddingLimit = MutableLiveData<Boolean>(false)
    val isAddingGoal = MutableLiveData<Boolean>(false)
    var currentCategory: String? = null

    var goalAmount by mutableStateOf("")
    var goalPeriod by mutableStateOf("")
    var weeklySaving by mutableStateOf("")
    var monthlySaving by mutableStateOf("")
    var savedAmount by mutableStateOf("")

    private val standardExpenseCategories = listOf("Їжа", "Транспорт", "Розваги")
    private val standardIncomeCategories = listOf("Зарплата", "Подарунки", "Пасивний дохід")

    init {
        loadSavedAmounts(application.applicationContext)
    }

    fun loadExpenseCategories(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("categories", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val categories: List<String> = gson.fromJson(json, type)

        val categoriesMap = categories.associateWith { 0.0 }
        expenseCategories.value = categoriesMap
    }

    fun loadMaxExpenses(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("max_expenses", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val maxExpensesMap: Map<String, Double> = gson.fromJson(json, type)
        maxExpenses.value = maxExpensesMap
    }

    fun loadExpensesFromMainActivity(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("expenses", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val expensesMap: Map<String, Double> = gson.fromJson(json, type)
        expenses.value = expensesMap
    }

    fun loadIncomesFromMainActivity(context: Context) {
        val sharedPreferences = context.getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("incomes", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val incomesMap: Map<String, Double> = gson.fromJson(json, type)
        incomes.value = incomesMap
    }

    fun loadSavedAmounts(context: Context) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_amounts", "[]")
        val type = object : TypeToken<List<Double>>() {}.type
        val savedAmountsList: List<Double> = gson.fromJson(json, type)
        savedAmounts.value = savedAmountsList
    }

    fun saveSavedAmounts(context: Context, savedAmountsList: List<Double>) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(savedAmountsList)
        sharedPreferences.edit().putString("saved_amounts", json).apply()
    }

    fun updateMaxExpense(context: Context, category: String, maxExpense: Double) {
        val currentMaxExpenses = maxExpenses.value ?: emptyMap()
        val updatedMaxExpenses = currentMaxExpenses.toMutableMap()
        updatedMaxExpenses[category] = maxExpense
        maxExpenses.value = updatedMaxExpenses

        saveMaxExpenses(context, updatedMaxExpenses)
        saveMessage.value = "Ліміт збережено"
        isAddingLimit.value = false
    }

    private fun saveMaxExpenses(context: Context, maxExpenses: Map<String, Double>) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(maxExpenses)
        sharedPreferences.edit().putString("max_expenses", json).apply()
    }

    fun toggleAddingLimit(category: String) {
        currentCategory = category
        isAddingLimit.value = !(isAddingLimit.value ?: false)
    }

    fun toggleAddingGoal() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        if (!isAddingGoal.value!!) {
            goalAmount = sharedPreferences.getString("goal_amount", "") ?: ""
            goalPeriod = sharedPreferences.getString("goal_period", "") ?: ""
            weeklySaving = sharedPreferences.getString("weekly_saving", "") ?: ""
            monthlySaving = sharedPreferences.getString("monthly_saving", "") ?: ""
            savedAmount = "" // Скидання поля вводу збереження на пусте значення
        }
        isAddingGoal.value = !(isAddingGoal.value ?: false)
    }

    fun calculateGoal() {
        val goalAmountValue = goalAmount.toDoubleOrNull() ?: 0.0
        val goalPeriodValue = goalPeriod.toIntOrNull() ?: 0
        weeklySaving = if (goalPeriodValue > 0) (goalAmountValue / (goalPeriodValue * 4)).formatBudgetAmount(2) else "0.0"
        monthlySaving = if (goalPeriodValue > 0) (goalAmountValue / goalPeriodValue).formatBudgetAmount(2) else "0.0"
    }

    fun saveGoal(context: Context) {
        calculateGoal()
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("goal_amount", goalAmount)
            .putString("goal_period", goalPeriod)
            .putString("weekly_saving", weeklySaving)
            .putString("monthly_saving", monthlySaving)
            .apply()

        saveMessage.value = "Ціль збережено"
        isAddingGoal.value = false
    }

    fun addSaving(context: Context) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val savedAmountsList = savedAmounts.value.orEmpty().toMutableList()
        val savedAmountValue = savedAmount.toDoubleOrNull() ?: 0.0
        savedAmountsList.add(savedAmountValue)
        saveSavedAmounts(context, savedAmountsList)

        savedAmounts.value = savedAmountsList
        savedAmount = "" // Очистити поле вводу після збереження
    }

    fun getPercentageToGoal(): Double {
        val goalAmountValue = goalAmount.toDoubleOrNull() ?: 0.0
        val totalSaved = savedAmounts.value?.sum() ?: 0.0
        return if (goalAmountValue > 0) (totalSaved / goalAmountValue) * 100 else 0.0
    }

    fun updateSaving(context: Context, index: Int, newAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedAmountsList = savedAmounts.value.orEmpty().toMutableList()
            if (index in savedAmountsList.indices) {
                savedAmountsList[index] = newAmount
                withContext(Dispatchers.Main) {
                    savedAmounts.value = savedAmountsList
                }
                saveSavingsToPrefs(context, savedAmountsList)
            }
        }
    }

    fun deleteSaving(context: Context, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedAmountsList = savedAmounts.value.orEmpty().toMutableList()
            if (index in savedAmountsList.indices) {
                savedAmountsList.removeAt(index)
                withContext(Dispatchers.Main) {
                    savedAmounts.value = savedAmountsList
                }
                saveSavingsToPrefs(context, savedAmountsList)
            }
        }
    }

    private fun saveSavingsToPrefs(context: Context, savings: List<Double>) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("saved_amounts", Gson().toJson(savings))
            .apply()
    }
}
@Composable
fun BudgetPlanningScreen(viewModel: BudgetPlanningViewModel) {
    val expenseCategories by viewModel.expenseCategories.observeAsState(emptyMap())
    val maxExpenses by viewModel.maxExpenses.observeAsState(emptyMap())
    val expenses by viewModel.expenses.observeAsState(emptyMap())
    val saveMessage by viewModel.saveMessage.observeAsState(null)
    val isAddingLimit by viewModel.isAddingLimit.observeAsState(false)
    val isAddingGoal by viewModel.isAddingGoal.observeAsState(false)
    val savingsList by viewModel.savedAmounts.observeAsState(emptyList())
    var isViewingSavings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    saveMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.saveMessage.value = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            // Сортування категорій за витратами у зростаючому порядку
            items(expenseCategories.toList().sortedBy { expenses[it.first] ?: 0.0 }) { (category, _) ->
                val expense = expenses[category] ?: 0.0
                val maxExpense = maxExpenses[category] ?: 0.0
                BudgetCategoryItemWithRedBackground(
                    category = category,
                    expense = expense,
                    maxExpense = maxExpense,
                    onToggleAddingLimit = {
                        viewModel.toggleAddingLimit(category)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(
            onClick = {
                viewModel.toggleAddingGoal()
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400).copy(alpha = 0.8f))
        ) {
            Text("Моя ціль", color = Color.White)
        }
    }

    if (isAddingLimit) {
        AddLimitDialog(
            category = viewModel.currentCategory ?: "",
            onDismissRequest = { viewModel.isAddingLimit.value = false },
            onSaveMaxExpense = { maxExpense ->
                viewModel.currentCategory?.let { category ->
                    viewModel.updateMaxExpense(context, category, maxExpense)
                }
            }
        )
    }

    if (isAddingGoal) {
        val percentageToGoal = viewModel.getPercentageToGoal() // Обчислення прогресу до цілі
        AddGoalDialog(
            goalAmount = viewModel.goalAmount,
            goalPeriod = viewModel.goalPeriod,
            weeklySaving = viewModel.weeklySaving,
            monthlySaving = viewModel.monthlySaving,
            savedAmount = viewModel.savedAmount, // Передача savedAmount
            onGoalAmountChange = { viewModel.goalAmount = it },
            onGoalPeriodChange = { viewModel.goalPeriod = it },
            onSavedAmountChange = { viewModel.savedAmount = it },
            onDismissRequest = { viewModel.isAddingGoal.value = false },
            onCalculateGoal = { viewModel.calculateGoal() },
            onSaveGoal = {
                viewModel.saveGoal(context)
                Toast.makeText(context, "Ціль збережено", Toast.LENGTH_SHORT).show() // Показ повідомлення після збереження
            },
            onAddSaving = { viewModel.addSaving(context) },
            onViewSavings = {
                viewModel.loadSavedAmounts(context)
                isViewingSavings = true
            },
            percentageToGoal = percentageToGoal, // Передача прогресу до цілі
            savedAmounts = savingsList, // Передача списку заощаджень
            context = context // Передача контексту
        )
    }

    if (isViewingSavings) {
        SavingsListDialog(
            savingsList = savingsList,
            onDismissRequest = { isViewingSavings = false },
            onUpdateSaving = { index, newAmount -> viewModel.updateSaving(context, index, newAmount) },
            onDeleteSaving = { index -> viewModel.deleteSaving(context, index) }
        )
    }
}

// Додано: функція для форматування відсотка
fun Double.format(digits: Int) = "%.${digits}f".format(this)
@Composable
fun BudgetCategoryItemWithRedBackground(
    category: String,
    expense: Double,
    maxExpense: Double,
    onToggleAddingLimit: () -> Unit
) {
    // Обчислення прогресу та відсотка витрат
    val progress = if (maxExpense > 0) expense / maxExpense else 0.0
    val percentage = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD32F2F).copy(alpha = 0.7f),
                        Color(0xFFB00020).copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            Button(
                onClick = onToggleAddingLimit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                val buttonText = if (maxExpense > 0) "Змінити ліміт" else "Додати ліміт"
                Text(buttonText, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ліміт: ${if (maxExpense > 0) maxExpense.formatBudgetAmount(2) + " грн" else "не заданий"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = Color.Gray
            )
            Text(
                text = "Витрачено $percentage%",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// Допоміжна функція для форматування чисел
fun Double.formatBudgetAmount(digits: Int): String {
    return "%.${digits}f".format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLimitDialog(
    category: String,
    onDismissRequest: () -> Unit,
    onSaveMaxExpense: (Double) -> Unit,
    textColor: Color = Color.White // Параметр для кольору тексту
) {
    var limitValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black.copy(alpha = 0.8f), // Темний прозорий фон
        title = {
            Text(text = "Додати ліміт для $category", color = textColor)
        },
        text = {
            OutlinedTextField(
                value = limitValue,
                onValueChange = { value -> limitValue = value },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = textColor // Колір курсора
                ),
                textStyle = LocalTextStyle.current.copy(color = textColor, fontWeight = FontWeight.Bold), // Жирний шрифт
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f)) // Темний прозорий фон для поля вводу
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxExpenseValue = limitValue.toDoubleOrNull() ?: 0.0
                    onSaveMaxExpense(maxExpenseValue)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.6f)) // Темно-зелена прозора кнопка
            ) {
                Text("Зберегти", color = textColor)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)) // Темно-червона прозора кнопка
            ) {
                Text("Скасувати", color = textColor)
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    goalAmount: String,
    goalPeriod: String,
    weeklySaving: String,
    monthlySaving: String,
    savedAmount: String,
    onGoalAmountChange: (String) -> Unit,
    onGoalPeriodChange: (String) -> Unit,
    onSavedAmountChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onCalculateGoal: () -> Unit,
    onSaveGoal: () -> Unit,
    onAddSaving: () -> Unit,
    onViewSavings: () -> Unit,
    percentageToGoal: Double,
    savedAmounts: List<Double>,
    context: Context // Додано для збереження даних
) {
    val localContext = LocalContext.current // Отримання контексту для Toast

    val formattedGoalAmount = remember(goalAmount) {
        goalAmount.formatWithSpaces()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black.copy(alpha = 0.8f), // Темний прозорий фон
        title = {
            Text(text = "Моя ціль", color = Color.White)
        },
        text = {
            Column(modifier = Modifier.width(400.dp)) { // Збільшення ширини діалогового вікна
                OutlinedTextField(
                    value = formattedGoalAmount,
                    onValueChange = { value -> onGoalAmountChange(value.filter { it.isDigit() }) },
                    label = { Text("Моя ціль (грн)", color = Color.Gray) }, // Додано "грн"
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White // Колір курсора
                    ),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold), // Жирний шрифт
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = goalPeriod,
                    onValueChange = onGoalPeriodChange,
                    label = { Text("Період (місяців)", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White // Колір курсора
                    ),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold), // Жирний шрифт
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCalculateGoal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00008B).copy(alpha = 0.6f)), // Темно-синя прозора кнопка
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Розрахувати", color = Color.White, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Відкладати щотижня: $weeklySaving грн",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Відкладати щомісяця: $monthlySaving грн",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp)) // Додано відступ
                Divider(color = Color.Gray) // Додано роздільну лінію
                Spacer(modifier = Modifier.height(16.dp)) // Додано відступ
                Text(
                    text = "Накопичена сума збережень: ${savedAmounts.sum()} грн",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Прогрес до цілі: ${percentageToGoal.format(2)}%",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onViewSavings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.6f)), // Сіра прозора кнопка
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Переглянути заощадження", color = Color.White, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = savedAmount,
                    onValueChange = onSavedAmountChange,
                    label = { Text("Додати суму", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White // Колір курсора
                    ),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold), // Жирний шрифт
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Додаємо нову суму заощаджень до списку
                        val newSavedAmount = savedAmount.toDoubleOrNull() ?: 0.0
                        onAddSaving()

                        // Зберігаємо нову суму заощаджень у SharedPreferences
                        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        val updatedSavedAmounts = savedAmounts + newSavedAmount
                        editor.putString("saved_amounts", Gson().toJson(updatedSavedAmounts))
                        editor.apply()

                        // Відображаємо повідомлення з введеною сумою
                        Toast.makeText(localContext, "Ви заощадили $newSavedAmount грн", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.4f)), // Прозора темно-зелена кнопка
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(
                modifier = Modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.4f)), // Прозора темно-червона кнопка
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Відміна", color = Color.White, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSaveGoal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.4f)), // Прозора темно-зелена кнопка
                    modifier = Modifier.weight(1f)
                ) {
                    Text("OK", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsListDialog(
    savingsList: List<Double>,
    onDismissRequest: () -> Unit,
    onUpdateSaving: (Int, Double) -> Unit,
    onDeleteSaving: (Int) -> Unit
) {
    var editingIndex by remember { mutableStateOf(-1) }
    var editedAmount by remember { mutableStateOf("") }

    if (editingIndex != -1) {
        Dialog(onDismissRequest = { editingIndex = -1 }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Редагувати заощадження",
                        color = Color.White,
                        fontSize = 20.sp,  // Використання конкретного розміру шрифту
                        fontWeight = FontWeight.Bold // Використання жирного шрифту
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedAmount,
                        onValueChange = { editedAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Сума", color = Color.Gray) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { editingIndex = -1 }) {
                            Text("Відміна", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val newAmount = editedAmount.toDoubleOrNull()
                                if (newAmount != null) {
                                    onUpdateSaving(editingIndex, newAmount)
                                    editingIndex = -1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.6f))
                        ) {
                            Text("Зберегти", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp)
        ) {
            Text(
                text = "Список заощаджень",
                fontSize = 20.sp,  // Використання конкретного розміру шрифту
                fontWeight = FontWeight.Bold, // Використання жирного шрифту
                color = Color.White, // Колір тексту
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                itemsIndexed(savingsList) { index, saving ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${saving} грн",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                editingIndex = index
                                editedAmount = saving.toString()
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Green) // Іконка олівця
                        }
                        IconButton(
                            onClick = { onDeleteSaving(index) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)), // Темно-червона прозора кнопка
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Закрити", color = Color.White)
            }
        }
    }
}
fun String.formatWithSpaces(): String {
    return this.reversed().chunked(3).joinToString(" ").reversed()
}
