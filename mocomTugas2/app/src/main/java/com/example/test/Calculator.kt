package com.example.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.test.ui.theme.CalculatorLogic
import com.example.test.ui.theme.TestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorUI(navController: NavController) {
    var isScientificMode by remember { mutableStateOf(false) }
    val calculatorLogic = remember { CalculatorLogic() }
    var displayValue by remember { mutableStateOf(calculatorLogic.getDisplay()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.Black
                        )
                    }
                },
                title = { Text("Calculator", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFC0C0C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = displayValue,
                        fontSize = 32.sp,
                        textAlign = TextAlign.End
                    )
                }

                val operatorColor = Color(0xFFFF9800)
                val specialColor = Color(0xFFC0C0C0)
                val digitColor = Color(0xFFF0F0F0)

                val mainButtons = listOf(
                    listOf("AC", "Del", "x^y", "/"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "−"),
                    listOf("1", "2", "3", "+"),
                    listOf("Sc", "0", ".", "=")
                )

                val scientificButtons = listOf(
                    listOf("log", "ln", "sin", "cos", "tan"),
                    listOf("sqrt", "asin", "acos", "atan", "x!")
                )

                if (isScientificMode) {
                    scientificButtons.forEach { rowButtons ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowButtons.forEach { buttonText ->
                                CalcButton(buttonText, background = specialColor) {
                                    calculatorLogic.onScientificFunction(buttonText)
                                    displayValue = calculatorLogic.getDisplay()
                                }
                            }
                        }
                    }
                }

                mainButtons.forEachIndexed { index, rowButtons ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (isScientificMode) {
                            when (index) {
                                0 -> CalcButton("1/x", background = specialColor) {
                                    calculatorLogic.onScientificFunction("1/x")
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                1 -> CalcButton("(", background = specialColor) {
                                    calculatorLogic.onOperator("(")
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                2 -> CalcButton(")", background = specialColor) {
                                    calculatorLogic.onOperator(")")
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                3 -> CalcButton("e", background = specialColor) {
                                    calculatorLogic.onConstant("e")
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                4 -> CalcButton("%", background = specialColor) {
                                    calculatorLogic.onScientificFunction("%")
                                    displayValue = calculatorLogic.getDisplay()
                                }
                            }
                        }

                        rowButtons.forEach { buttonText ->
                            val (backgroundColor, onClickAction) = when (buttonText) {
                                "AC" -> specialColor to {
                                    calculatorLogic.onClear()
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                "Del" -> specialColor to {
                                    calculatorLogic.onDelete()
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                "Sc" -> specialColor to {
                                    isScientificMode = !isScientificMode
                                }
                                "=" -> operatorColor to {
                                    calculatorLogic.onEquals()
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                "x^y", "/", "×", "−", "+" -> operatorColor to {
                                    val operatorToSend = when (buttonText) {
                                        "×" -> "x"
                                        "−" -> "-"
                                        "x^y" -> "^"
                                        else -> buttonText
                                    }
                                    calculatorLogic.onOperator(operatorToSend)
                                    displayValue = calculatorLogic.getDisplay()
                                }
                                else -> digitColor to {
                                    calculatorLogic.onDigit(buttonText)
                                    displayValue = calculatorLogic.getDisplay()
                                }
                            }

                            CalcButton(textButton = buttonText, background = backgroundColor, onClick = onClickAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.CalcButton(
    textButton: String,
    weight: Float = 1f,
    background: Color = Color(0xFFEEEEEE),
    onClick: () -> Unit = {}
) {
    Button(
        modifier = Modifier
            .weight(weight)
            .aspectRatio(1f)
            .padding(4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = textButton,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorUIPreview() {
    TestTheme {
        val navController = rememberNavController()
        CalculatorUI(navController = navController)
    }
}