package com.example.homeaccountingapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import com.example.homeaccountingapp.DrawerContent
import kotlinx.coroutines.launch

class TaskActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)

        val viewModel: TaskViewModel by viewModels {
            TaskViewModelFactory(sharedPreferences, gson)
        }

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@TaskActivity, MainActivity::class.java).apply {
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
                                title = { Text("Задачник", color = Color.White) },
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
                                // Завантажуємо задачі з SharedPreferences
                                viewModel.loadTasks()
                                TaskScreen(viewModel)
                            }
                        }
                    )
                }
            }
        }
    }
}

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: Date,
    val endDate: Date,
    var isCompleted: Boolean = false
)

class TaskViewModel(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    fun addTask(task: Task) {
        _tasks.add(task)
        saveTasks()
    }

    fun removeTask(task: Task) {
        _tasks.remove(task)
        saveTasks()
    }

    fun toggleTaskCompletion(task: Task) {
        val index = _tasks.indexOf(task)
        if (index != -1) {
            _tasks[index] = task.copy(isCompleted = !task.isCompleted)
            saveTasks()
        }
    }

    // Завантаження задач з SharedPreferences
    fun loadTasks() {
        val tasksJson = sharedPreferences.getString("tasks", "[]")
        val type = object : TypeToken<List<Task>>() {}.type
        val loadedTasks: List<Task> = gson.fromJson(tasksJson, type)
        _tasks.clear()
        _tasks.addAll(loadedTasks)
    }

    // Збереження задач у SharedPreferences
    private fun saveTasks() {
        val editor = sharedPreferences.edit()
        val tasksJson = gson.toJson(_tasks)
        editor.putString("tasks", tasksJson)
        editor.apply()
    }
}

class TaskViewModelFactory(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(sharedPreferences, gson) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF228B22)
            ) {
                Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 72.dp), // Нижній відступ для кнопки "+", щоб уникнути перекриття
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TaskList(viewModel.tasks, viewModel::toggleTaskCompletion, viewModel::removeTask)
                }

                if (showAddTaskDialog) {
                    AddTaskDialog(
                        onDismiss = { showAddTaskDialog = false },
                        onSave = { task ->
                            viewModel.addTask(task)
                            showAddTaskDialog = false
                            Toast.makeText(context, "Задача додана", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task, onToggleCompletion, onDeleteTask)
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.8f)) // Яскравіше, але прозоре
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold, // Жирний шрифт
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = Color.White
                )
            )
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = Color.White
                    )
                )
            }
            Text(
                text = "Початок: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.startDate)}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )
            Text(
                text = "Кінець: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.endDate)}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )
            if (task.isCompleted) {
                Text(
                    text = "Виконано",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Green)
                )
            }
        }
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion(task) },
            colors = CheckboxDefaults.colors(checkedColor = Color.Green)
        )
        IconButton(onClick = { onDeleteTask(task) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Видалити", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showStartDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showStartDatePicker = false
            },
            startDate.year + 1900,
            startDate.month,
            startDate.date
        ).show()
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showEndDatePicker = false
            },
            endDate.year + 1900,
            endDate.month,
            endDate.date
        ).show()
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text("Додати задачу", color = Color.White)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва задачі", color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис задачі", color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                ) {
                    Text(
                        text = "Початок: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                ) {
                    Text(
                        text = "Кінець: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        onSave(Task(UUID.randomUUID().toString(), title, description.ifEmpty { null }, startDate, endDate))
                    } else {
                        // Show error message
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Green.copy(alpha = 0.6f) // Зелений з прозорістю
                )
            ) {
                Text("Зберегти", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.6f) // Червоний з прозорістю
                )
            ) {
                Text("Відмінити", color = Color.White)
            }
        },
        containerColor = Color.Black.copy(alpha = 0.8f), // Чорний і прозорий фон
        textContentColor = Color.White
    )
}