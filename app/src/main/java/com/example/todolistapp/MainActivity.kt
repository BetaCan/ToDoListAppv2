package com.example.todolistapp

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todolistapp.ui.theme.ToDoListAppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

// Task Categories
enum class TaskCategory { Work, Personal, Shopping, Other }

// Task Data Class
data class Task(
    val description: String,
    val isCompleted: Boolean = false,
    val category: TaskCategory = TaskCategory.Other,
    val dueDate: LocalDate? = null
)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyToDoApp()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyToDoApp() {
    var darkMode by remember { mutableStateOf(false) }
    ToDoListAppTheme(darkTheme = darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dark Mode Toggle 🌙
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("Dark Mode", modifier = Modifier.padding(end = 8.dp))
                    Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                ToDoListApp()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ToDoListApp() {
    var taskDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.Other) }
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var selectedDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Task Input
        TextField(
            value = taskDescription,
            onValueChange = { taskDescription = it },
            label = { Text("Enter Task") },
            modifier = Modifier.fillMaxWidth()
        )

        // Category Selector
        TaskCategoryDropdown(selectedCategory) { newCategory -> selectedCategory = newCategory }

        // Due Date Picker
        var showDatePicker by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Due Date: ${selectedDueDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None"}")
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showDatePicker = true }) {
                Text("Pick Date")
            }
        }

        if (showDatePicker) {
            ShowDatePicker { selectedDueDate = it; showDatePicker = false }
        }

        // Add Task Button
        Button(
            onClick = {
                if (taskDescription.isNotEmpty()) {
                    tasks = tasks + Task(
                        description = taskDescription,
                        category = selectedCategory,
                        dueDate = selectedDueDate
                    )
                    taskDescription = ""
                    selectedDueDate = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Task")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Tracking 📊
        val completedTasks = tasks.count { it.isCompleted }
        val progress = if (tasks.isEmpty()) 0f else completedTasks.toFloat() / tasks.size.toFloat()

        Text("Progress: $completedTasks/${tasks.size} tasks completed")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Task List
        LazyColumn {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onTaskCompleted = { completedTask ->
                        tasks = tasks.map {
                            if (it == completedTask) it.copy(isCompleted = !it.isCompleted) else it
                        }
                    },
                    onTaskEdit = { editingTask = it },
                    onTaskDelete = { taskToDelete ->
                        tasks = tasks.filter { it != taskToDelete }
                    }
                )
            }
        }
    }

    // Show Edit Dialog when editingTask is set
    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { updatedTask ->
                tasks = tasks.map {
                    if (it == task) updatedTask else it
                }
                editingTask = null
            }
        )
    }
}

// Edit Task Dialog
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var newDescription by remember { mutableStateOf(task.description) }
    var newDueDate by remember { mutableStateOf(task.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column {
                // Task Description Input
                TextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    label = { Text("Task Description") }
                )

                // Due Date Picker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Due Date: ${newDueDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None"}")
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showDatePicker = true }) { Text("Pick Date") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newDescription.isNotEmpty()) {
                    onSave(task.copy(description = newDescription, dueDate = newDueDate))
                }
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        ShowDatePicker { newDueDate = it; showDatePicker = false }
    }
}

// Show Date Picker Dialog
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShowDatePicker(onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
@Composable
fun TaskCategoryDropdown(selectedCategory: TaskCategory, onCategorySelected: (TaskCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedCategory.name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskItem(
    task: Task,
    onTaskCompleted: (Task) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit
) {
    val isOverdue = task.dueDate?.isBefore(LocalDate.now()) == true && !task.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isOverdue) Color.Red else Color.Unspecified
                )
                Text("Category: ${task.category.name}", style = MaterialTheme.typography.bodySmall)
                task.dueDate?.let {
                    Text(
                        "Due: ${it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        color = if (isOverdue) Color.Red else Color.Gray
                    )
                }
            }
            Row {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onTaskCompleted(task) }
                )
                IconButton(onClick = { onTaskEdit(task) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Task")
                }
                IconButton(onClick = { onTaskDelete(task) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Task")
                }
            }
        }
    }
}
