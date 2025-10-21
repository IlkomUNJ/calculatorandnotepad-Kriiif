package com.example.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.test.ui.theme.TestTheme
import kotlinx.coroutines.launch

// ---------- Data Model ----------
data class Note(
    val id: Int,
    val content: String
)

// ---------- App Entry ----------
@Composable
fun NotepadApp(navController: NavHostController) {
    val navController = rememberNavController()
    val notes = remember { mutableStateListOf<Note>() }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            NoteListScreen(navController, notes)
        }
        composable("editor/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull()
            TextEditorScreen(navController, notes, noteId)
        }
    }
}

// ---------- Menu Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(navController: NavController, notes: MutableList<Note>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Notepad", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFC0C0C0))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newId = (notes.maxOfOrNull { it.id } ?: 0) + 1
                    notes.add(Note(newId, ""))
                    navController.navigate("editor/$newId")
                },
                containerColor = Color(0xFF1976D2)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes yet. Tap + to create one!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("editor/${note.id}")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Text(
                            text = note.content.take(100).ifEmpty { "(Empty Note)" },
                            modifier = Modifier.padding(16.dp),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

// ---------- Your Original Editor (Unmodified core logic) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    navController: NavController,
    notes: MutableList<Note>,
    noteId: Int?
) {
    val note = notes.find { it.id == noteId } ?: return
    var textFieldValue by remember { mutableStateOf(TextFieldValue(note.content)) }
    var fontSize by remember { mutableIntStateOf(16) }

    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun buildAnnotatedFromFlags(text: String, bold: BooleanArray, italic: BooleanArray): AnnotatedString {
        val n = text.length
        return buildAnnotatedString {
            append(text)
            if (n == 0) return@buildAnnotatedString
            var i = 0
            while (i < n) {
                val bw = bold.getOrNull(i) ?: false
                val it = italic.getOrNull(i) ?: false
                var j = i + 1
                while (j < n && (bold.getOrNull(j) == bw) && (italic.getOrNull(j) == it)) j++
                if (bw || it) {
                    addStyle(
                        SpanStyle(
                            fontWeight = if (bw) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (it) FontStyle.Italic else FontStyle.Normal
                        ),
                        i,
                        j
                    )
                }
                i = j
            }
        }
    }

    fun readFlagsFromAnnotated(annotated: AnnotatedString, len: Int): Pair<BooleanArray, BooleanArray> {
        val bold = BooleanArray(len) { false }
        val italic = BooleanArray(len) { false }
        annotated.spanStyles.forEach { span ->
            val style = span.item
            val start = span.start.coerceAtLeast(0)
            val end = span.end.coerceAtMost(len)
            if (start >= end) return@forEach
            for (i in start until end) {
                if (style.fontWeight == FontWeight.Bold) bold[i] = true
                if (style.fontStyle == FontStyle.Italic) italic[i] = true
            }
        }
        return bold to italic
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                title = { Text("Text Editor", color = Color.Black) },
                actions = {
                    IconButton(onClick = {
                        val sel = textFieldValue.selection
                        val text = textFieldValue.text
                        if (!sel.collapsed) {
                            val (boldFlags, italicFlags) = readFlagsFromAnnotated(textFieldValue.annotatedString, text.length)
                            var allBold = true
                            for (i in sel.start until sel.end) {
                                if (!boldFlags.getOrNull(i)!!) { allBold = false; break }
                            }
                            val newBoldStateForSelection = !allBold
                            for (i in sel.start until sel.end) {
                                boldFlags[i] = newBoldStateForSelection
                            }
                            val newAnnotated = buildAnnotatedFromFlags(text, boldFlags, italicFlags)
                            textFieldValue = textFieldValue.copy(annotatedString = newAnnotated)
                        } else {
                            isBold = !isBold
                        }
                    }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (isBold) Color.Blue else Color.Black)
                    }

                    IconButton(onClick = {
                        val sel = textFieldValue.selection
                        val text = textFieldValue.text
                        if (!sel.collapsed) {
                            val (boldFlags, italicFlags) = readFlagsFromAnnotated(textFieldValue.annotatedString, text.length)
                            var allItalic = true
                            for (i in sel.start until sel.end) {
                                if (!italicFlags.getOrNull(i)!!) { allItalic = false; break }
                            }
                            val newItalicStateForSelection = !allItalic
                            for (i in sel.start until sel.end) {
                                italicFlags[i] = newItalicStateForSelection
                            }
                            val newAnnotated = buildAnnotatedFromFlags(text, boldFlags, italicFlags)
                            textFieldValue = textFieldValue.copy(annotatedString = newAnnotated)
                        } else {
                            isItalic = !isItalic
                        }
                    }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (isItalic) Color.Blue else Color.Black)
                    }

                    IconButton(onClick = { if (fontSize > 8) fontSize -= 2 }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Text", tint = Color.Black)
                    }

                    IconButton(onClick = { fontSize += 2 }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Increase Text", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFC0C0C0))
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        textFieldValue = TextFieldValue("")
                        scope.launch { snackbarHostState.showSnackbar("New note created 📝") }
                    },
                    containerColor = Color(0xFF1976D2)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Add", tint = Color.White)
                }

                FloatingActionButton(
                    onClick = {
                        notes.replaceAll {
                            if (it.id == note.id) it.copy(content = textFieldValue.text) else it
                        }
                        scope.launch { snackbarHostState.showSnackbar("Note saved 💾") }
                    },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val newText = newValue.text
                    val oldLen = oldText.length
                    val newLen = newText.length

                    val (oldBold, oldItalic) = readFlagsFromAnnotated(textFieldValue.annotatedString, oldLen)
                    val boldFlags = BooleanArray(newLen) { false }
                    val italicFlags = BooleanArray(newLen) { false }

                    val minLen = minOf(oldLen, newLen)
                    for (i in 0 until minLen) {
                        boldFlags[i] = oldBold[i]
                        italicFlags[i] = oldItalic[i]
                    }

                    if (newLen > oldLen) {
                        for (i in oldLen until newLen) {
                            boldFlags[i] = isBold
                            italicFlags[i] = isItalic
                        }
                    }

                    val newAnnotated = buildAnnotatedFromFlags(newText, boldFlags, italicFlags)
                    textFieldValue = newValue.copy(annotatedString = newAnnotated)
                },
                textStyle = TextStyle(fontSize = fontSize.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text("Start typing here...", style = TextStyle(color = Color.Gray))
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// ---------- Preview ----------
@Preview(showBackground = true)
@Composable
fun NotepadPreview() {
    TestTheme {
        val navController = rememberNavController()
        NotepadApp(navController = navController)
    }
}