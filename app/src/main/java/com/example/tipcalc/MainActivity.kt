
package com.example.tipcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tipcalc.ui.theme.TipCalcTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TipCalcTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    TipCalcScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TipCalcScreen(modifier: Modifier = Modifier) {

    var orderAmount by remember { mutableStateOf("") }
    var dishCount by remember { mutableStateOf("") }
    var tipPercent by remember { mutableStateOf(0f) }
    var showTotal by remember(orderAmount, dishCount, tipPercent) {
        mutableStateOf(false)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val discountPercent = calculateDiscountPercent(dishCount)
    val discountAmount = calculateDiscount(
        orderAmount,
        discountPercent
    )

    val totalAmount = calculateTotal(
        orderAmount,
        tipPercent,
        discountPercent
    )


    Column(

        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Сумма заказа
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Сумма заказа:",
                modifier = Modifier.width(140.dp)
            )

            OutlinedTextField(
                value = orderAmount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() ||
                        newValue.matches(Regex("""\d*([.,]\d{0,2})?"""))
                    ) {
                        orderAmount = newValue
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                placeholder = { Text("0.00") }
            )

        }

        // Количество блюд
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Количество блюд:",
                modifier = Modifier.width(140.dp)
            )

            OutlinedTextField(
                value = dishCount,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        dishCount = newValue
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                placeholder = { Text("0") }
            )

        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ползунок чаевых
        Text(text = "Чаевые:")



        Slider(
            value = tipPercent,
            onValueChange = { newValue ->
                tipPercent = newValue
            },
            valueRange = 0f..25f,
            steps = 4,
            onValueChangeFinished = {
                val tipAmount = calculateTip(orderAmount, tipPercent)

                val formattedTip = String.format(
                    Locale.forLanguageTag("ru-RU"),
                    "%.2f",
                    tipAmount
                )

                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Сумма чаевых: $formattedTip ₽"
                    )
                }
            }
        )


        Text(
            text = "Выбрано чаевых: ${tipPercent.toInt()}%"
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0%")
            Text("25%")
        }

        // Группа скидок
        Text(text = "Скидка:")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(3, 5, 7, 10).forEach { percent ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RadioButton(
                        selected = percent == discountPercent,
                        onClick = null
                    )
                    Text(text = "$percent%")
                }
            }
        }

        // Результат


        val resultAmount = if (showTotal) totalAmount else discountAmount

        val resultLabel = if (showTotal) {
            "Итого"
        } else {
            "Сумма скидки"
        }

        Text(
            text = "$resultLabel: ${
                String.format(
                    Locale.forLanguageTag("ru-RU"),
                    "%.2f",
                    resultAmount
                )
            } ₽"
        )

        Button(
            onClick = {
                showTotal = true
            }
        ) {
            Text("Итого")
        }


        Spacer(modifier = Modifier.weight(1f))

        SnackbarHost(
            hostState = snackbarHostState
        )

    }
}


fun calculateTip(amountText: String, tipPercent: Float): Double {
    val amount = amountText
        .replace(',', '.')
        .toDoubleOrNull() ?: 0.0

    return amount.coerceAtLeast(0.0) * tipPercent / 100.0
}

fun calculateDiscountPercent(dishCountText: String): Int {
    val count = dishCountText.toIntOrNull() ?: 0

    return when (count) {
        in 1..2 -> 3
        in 3..5 -> 5
        in 6..10 -> 7
        in 11..Int.MAX_VALUE -> 10
        else -> 0
    }
}

fun calculateDiscount(
    amountText: String,
    discountPercent: Int
): Double {
    val amount = amountText
        .replace(',', '.')
        .toDoubleOrNull() ?: 0.0

    return amount.coerceAtLeast(0.0) * discountPercent / 100.0
}

fun calculateTotal(
    amountText: String,
    tipPercent: Float,
    discountPercent: Int
): Double {
    val amount = amountText
        .replace(',', '.')
        .toDoubleOrNull()
        ?.coerceAtLeast(0.0) ?: 0.0

    val tip = calculateTip(amountText, tipPercent)
    val discount = calculateDiscount(amountText, discountPercent)

    return amount + tip - discount
}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun TipCalcPreview() {
    TipCalcTheme {
        TipCalcScreen()
    }
}

