package com.example.day3homework

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.ArrayDeque

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvFormula: TextView
    private lateinit var display: TextView

    private var current = "0"                 // 当前正在输入的数字（可带 - 和 .）
    private val tokens = mutableListOf<String>() // 表达式 token：number op number op...
    private var justPressedOp = false
    private var justEvaluated = false

    private var selectedOpBtn: AppCompatButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculate)

        tvFormula = findViewById(R.id.tvFormula)
        display = findViewById(R.id.display)

        // 数字
        bindDigit(R.id.btn0, "0")
        bindDigit(R.id.btn1, "1")
        bindDigit(R.id.btn2, "2")
        bindDigit(R.id.btn3, "3")
        bindDigit(R.id.btn4, "4")
        bindDigit(R.id.btn5, "5")
        bindDigit(R.id.btn6, "6")
        bindDigit(R.id.btn7, "7")
        bindDigit(R.id.btn8, "8")
        bindDigit(R.id.btn9, "9")

        // 小数点
        bind(R.id.btnDot) { onDot() }

        // 功能
        bind(R.id.btnAC) { onAC() }
        bind(R.id.btnSign) { onToggleSign() }

        // 运算（含取余）
        bindOp(R.id.btnAdd, "+")
        bindOp(R.id.btnSub, "-")
        bindOp(R.id.btnMul, "*")
        bindOp(R.id.btnDiv, "/")
        bindOp(R.id.btnMod, "%")

        // 等号
        bind(R.id.btnEq) { onEqual() }

        updateUI()
    }

    private fun bindDigit(id: Int, digit: String) {
        bind(id) { onDigit(digit) }
    }

    private fun bindOp(id: Int, op: String) {
        val btn = findViewById<AppCompatButton>(id)
        btn.setOnClickListener {
            tap(btn)
            onOperator(op, btn)
        }
    }

    private fun bind(id: Int, action: () -> Unit) {
        val btn = findViewById<AppCompatButton>(id)
        btn.setOnClickListener {
            tap(btn)
            action()
        }
    }

    private fun tap(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        // 更“炫酷”的回弹：先缩小再回到 1
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun onDigit(d: String) {
        if (justEvaluated) {
            // 刚算完，输入数字就开始新一轮
            tokens.clear()
            selectedOpBtn = null
            tvFormula.text = ""
            current = "0"
            justEvaluated = false
        }

        if (justPressedOp) {
            current = "0"
            justPressedOp = false
        }

        current = when {
            current == "0" -> d
            current == "-0" -> "-$d"
            else -> current + d
        }
        updateUI()
    }

    private fun onDot() {
        if (justEvaluated) {
            tokens.clear()
            selectedOpBtn = null
            tvFormula.text = ""
            current = "0"
            justEvaluated = false
        }
        if (justPressedOp) {
            current = "0"
            justPressedOp = false
        }
        if (!current.contains(".")) {
            current += "."
        }
        updateUI()
    }

    private fun onToggleSign() {
        if (current == "0" || current == "0.") return
        current = if (current.startsWith("-")) current.drop(1) else "-$current"
        updateUI()
    }

    private fun onAC() {
        tokens.clear()
        current = "0"
        justPressedOp = false
        justEvaluated = false
        selectedOpBtn = null
        tvFormula.text = ""
        updateUI()
        // 轻微“重置”动画
        display.animate().alpha(0.4f).setDuration(60).withEndAction {
            display.animate().alpha(1f).setDuration(120).start()
        }.start()
    }

    private fun onOperator(op: String, btn: AppCompatButton) {
        if (justEvaluated) {
            justEvaluated = false
        }

        // 如果连续点运算符：替换最后一个运算符
        if (tokens.isNotEmpty() && isOp(tokens.last()) && justPressedOp) {
            tokens[tokens.lastIndex] = op
        } else {
            // 先把当前数入栈
            tokens.add(current)
            tokens.add(op)
        }

        justPressedOp = true
        highlightOp(btn)
        updateUI()
    }

    private fun highlightOp(btn: AppCompatButton) {
        selectedOpBtn?.alpha = 1f
        selectedOpBtn = btn
        // 选中态：稍微变亮
        btn.alpha = 0.86f
    }

    private fun onEqual() {
        try {
            // 完整表达式：tokens + current
            val expr = buildList {
                addAll(tokens)
                if (tokens.isEmpty() || !justPressedOp) add(current) else add(tokens[tokens.lastIndex - 1]) // 防止末尾是 op
            }

            val value = eval(expr)
            val formatted = format(value)

            // 更新显示
            tvFormula.text = expr.joinToString(" ").replace("*", "×").replace("/", "÷")
            current = formatted
            display.text = current

            // “结果弹出”动画
            display.animate().scaleX(1.04f).scaleY(1.04f).setDuration(90).withEndAction {
                display.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()

            tokens.clear()
            justPressedOp = false
            justEvaluated = true
            selectedOpBtn?.alpha = 1f
            selectedOpBtn = null
        } catch (e: ArithmeticException) {
            showError("Error")
        } catch (e: Exception) {
            showError("Error")
        }
    }

    private fun showError(msg: String) {
        display.text = msg
        // 抖动动画
        ObjectAnimator.ofFloat(display, "translationX", 0f, 16f, -16f, 10f, -10f, 0f).apply {
            duration = 260
            start()
        }
        tokens.clear()
        current = "0"
        justPressedOp = false
        justEvaluated = false
        selectedOpBtn?.alpha = 1f
        selectedOpBtn = null
        tvFormula.text = ""
    }

    private fun updateUI() {
        display.text = current
        tvFormula.text = tokens.joinToString(" ")
            .replace("*", "×")
            .replace("/", "÷")
    }

    // ----------------- 表达式求值（支持 + - * / %，按优先级） -----------------

    private fun eval(tokens: List<String>): Double {
        val output = ArrayDeque<String>()
        val ops = ArrayDeque<String>()

        for (t in tokens) {
            if (!isOp(t)) {
                output.addLast(t)
            } else {
                while (ops.isNotEmpty() && isOp(ops.last()) && precedence(ops.last()) >= precedence(t)) {
                    output.addLast(ops.removeLast())
                }
                ops.addLast(t)
            }
        }
        while (ops.isNotEmpty()) output.addLast(ops.removeLast())

        val st = ArrayDeque<Double>()
        for (t in output) {
            if (!isOp(t)) {
                st.addLast(t.toDouble())
            } else {
                val b = st.removeLast()
                val a = st.removeLast()
                val r = when (t) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Divide by zero")
                        a / b
                    }
                    "%" -> {
                        if (b == 0.0) throw ArithmeticException("Mod by zero")
                        a % b
                    }
                    else -> error("Unknown op")
                }
                st.addLast(r)
            }
        }
        return st.last()
    }

    private fun isOp(s: String) = s == "+" || s == "-" || s == "*" || s == "/" || s == "%"
    private fun precedence(op: String) = if (op == "+" || op == "-") 1 else 2

    private fun format(v: Double): String {
        // 处理 -0
        if (kotlin.math.abs(v) < 1e-12) return "0"

        // 保留最多 10 位小数并去掉无意义 0
        val bd = BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString()
    }
}
