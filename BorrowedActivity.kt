package com.example.homeaccountingapp

import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlinx.coroutines.launch

class BorrowedActivity : ComponentActivity() {
    private val borrowedViewModel: BorrowedViewModel by viewModels { BorrowedViewModelFactory(application) }
    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
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
                                val intent = Intent(this@BorrowedActivity, MainActivity::class.java).apply {
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
                                title = { Text("Позичено", color = Color.White) },
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
                                BorrowedScreen(viewModel = borrowedViewModel)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BorrowedScreen(
    viewModel: BorrowedViewModel,
    modifier: Modifier = Modifier // Додаємо параметр modifier
) {
    var showAddBorrowedDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<BorrowedTransaction?>(null) }
    val transactions by viewModel.transactions.collectAsState()

    // Обчислюємо загальну суму позичань
    val totalBorrowed = transactions.sumOf { it.amount }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Застосовуємо weight до LazyColumn
            ) {
                items(transactions) { borrowedTransaction ->
                    BorrowedTransactionRow(borrowedTransaction, viewModel, onEdit = { transactionToEdit = it })
                }
            }

            // Додаємо підсумок знизу екрана
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Позичено всього: ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${totalBorrowed.formatBorrowedAmount(2)} грн", // Форматування суми
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddBorrowedDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFFFFA500)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = Color.White)
        }

        if (showAddBorrowedDialog || transactionToEdit != null) {
            AddOrEditBorrowedTransactionDialog(
                onDismiss = {
                    showAddBorrowedDialog = false
                    transactionToEdit = null
                },
                onSave = { newTransaction ->
                    if (transactionToEdit != null) {
                        viewModel.updateBorrowedTransaction(newTransaction)
                    } else {
                        viewModel.addBorrowedTransaction(newTransaction)
                    }
                    transactionToEdit = null
                    showAddBorrowedDialog = false
                },
                transactionToEdit = transactionToEdit
            )
        }
    }
}

// Перейменована функція для форматування суми
fun Double.formatBorrowedAmount(digits: Int): String {
    return "%.${digits}f".format(this)
}

@Composable
fun BorrowedTransactionRow(borrowedTransaction: BorrowedTransaction, viewModel: BorrowedViewModel, onEdit: (BorrowedTransaction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFA500).copy(alpha = 0.7f), Color(0xFFFFA500).copy(alpha = 0.1f))
                )
            )
            .clickable {
                // Handle click, if needed
            }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Сума: ${borrowedTransaction.amount.formatBorrowedAmount(2)} грн", // Додаємо "грн" до суми
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Ім'я: ${borrowedTransaction.borrowerName}",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Дата видачі: ${borrowedTransaction.issueDate}",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.LightGray),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Дата погашення: ${borrowedTransaction.dueDate}",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.LightGray),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Коментар: ${borrowedTransaction.comment}",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.LightGray),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { onEdit(borrowedTransaction) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
            }
            IconButton(onClick = { viewModel.removeBorrowedTransaction(borrowedTransaction) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditBorrowedTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (BorrowedTransaction) -> Unit,
    transactionToEdit: BorrowedTransaction? = null
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var borrowerName by remember { mutableStateOf(transactionToEdit?.borrowerName ?: "") }
    var issueDate by remember { mutableStateOf(transactionToEdit?.issueDate ?: getCurrentDateForBorrowed()) }
    var dueDate by remember { mutableStateOf(transactionToEdit?.dueDate ?: getCurrentDateForBorrowed()) }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showIssueDatePicker) {
        BorrowedDatePickerDialog(
            onDateSelected = { selectedDate ->
                issueDate = selectedDate
                showIssueDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        BorrowedDatePickerDialog(
            onDateSelected = { selectedDate ->
                dueDate = selectedDate
                showDueDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (transactionToEdit != null) "Редагування транзакції" else "Додавання нової транзакції", style = TextStyle(color = Color.White)) },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сума", style = TextStyle(color = Color.White)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Дата позичання",
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showIssueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = issueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Дата погашення",
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = dueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = borrowerName,
                    onValueChange = { borrowerName = it },
                    label = { Text("Позичальник", style = TextStyle(color = Color.White)) },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Коментар", style = TextStyle(color = Color.White)) },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null) {
                        onSave(BorrowedTransaction(amountValue, borrowerName, issueDate, dueDate, comment))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Зберегти", style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = Color.White)
            }
        },
        containerColor = Color.DarkGray
    )
}

@Composable
fun BorrowedDatePickerDialog(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            onDateSelected(formattedDate)
        },
        year, month, day
    )
    LaunchedEffect(Unit) {
        datePickerDialog.show()
    }
}

fun getCurrentDateForBorrowed(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

class BorrowedViewModel(application: Application) : AndroidViewModel(application) {
    private val _transactions = MutableStateFlow<List<BorrowedTransaction>>(emptyList())
    val transactions: StateFlow<List<BorrowedTransaction>> = _transactions

    init {
        loadTransactions(application)
    }

    private fun loadTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("BorrowedPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = sharedPreferences.getString("BorrowedTransactions", "[]")
        val type = object : TypeToken<List<BorrowedTransaction>>() {}.type
        val loadedTransactions: List<BorrowedTransaction> = gson.fromJson(transactionsJson, type)
        _transactions.update { loadedTransactions }
    }

    private fun saveTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("BorrowedPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = gson.toJson(_transactions.value)
        sharedPreferences.edit().putString("BorrowedTransactions", transactionsJson).apply()
    }

    fun addBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList + transaction
        }
        saveTransactions(getApplication())
    }

    fun updateBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList.map { if (it.id == transaction.id) transaction else it }
        }
        saveTransactions(getApplication())
    }

    fun removeBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList - transaction
        }
        saveTransactions(getApplication())
    }
}

class BorrowedViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BorrowedViewModel::class.java)) {
            return BorrowedViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class BorrowedTransaction(
    val amount: Double,
    val borrowerName: String,
    val issueDate: String,
    val dueDate: String,
    val comment: String,
    val id: UUID = UUID.randomUUID()
)