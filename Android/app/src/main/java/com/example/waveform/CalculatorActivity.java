package com.example.waveform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;
import java.util.Stack;

public class CalculatorActivity extends AppCompatActivity {

    private TextView displayTextView;
    private TextView formulaTextView;
    private StringBuilder currentInput = new StringBuilder();
    private StringBuilder formula = new StringBuilder();
    private double previousResult = 0;
    private String currentOperator = "";
    private boolean isNewInput = true;
    private boolean hasError = false;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // 初始化视图
        displayTextView = findViewById(R.id.displayTextView);
        formulaTextView = findViewById(R.id.formulaTextView);

        // 设置按钮点击事件
        findViewById(R.id.btnC).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnPlusMinus).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnPercent).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnDivide).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn7).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn8).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn9).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnMultiply).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn4).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn5).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn6).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnSubtract).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn1).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn2).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn3).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnAdd).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btn0).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnDot).setOnClickListener(this::onButtonClick);
        findViewById(R.id.btnEquals).setOnClickListener(this::onButtonClick);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setTitle("计算器");
        // 由于使用NoActionBar主题，不设置ActionBar返回按钮
    }

    private void onButtonClick(View view) {
        Button button = (Button) view;
        String text = button.getText().toString();

        if (hasError) {
            if (text.equals("C")) {
                clearAll();
            }
            return;
        }

        switch (text) {
            case "C":
                clearAll();
                break;
            case "±":
                toggleSign();
                break;
            case "%":
                percentage();
                break;
            case "÷":
            case "×":
            case "-":
            case "+":
                setOperator(text);
                break;
            case "=":
                calculate();
                break;
            case ".":
                addDecimalPoint();
                break;
            default: // 数字
                addDigit(text);
                break;
        }
    }

    private void clearAll() {
        currentInput.setLength(0);
        formula.setLength(0);
        previousResult = 0;
        currentOperator = "";
        isNewInput = true;
        hasError = false;
        updateDisplay("0", "");
    }

    private void addDigit(String digit) {
        if (isNewInput) {
            currentInput.setLength(0);
            isNewInput = false;
        }

        // 避免多个前导零
        if (currentInput.length() == 0 && digit.equals("0")) {
            return;
        }

        currentInput.append(digit);
        updateDisplay(currentInput.toString(), formula.toString());
    }

    private void addDecimalPoint() {
        if (isNewInput) {
            currentInput.append("0.");
            isNewInput = false;
        } else if (!currentInput.toString().contains(".")) {
            currentInput.append(".");
        }
        updateDisplay(currentInput.toString(), formula.toString());
    }

    private void toggleSign() {
        if (currentInput.length() > 0) {
            String input = currentInput.toString();
            if (input.startsWith("-")) {
                currentInput = new StringBuilder(input.substring(1));
            } else {
                currentInput.insert(0, "-");
            }
            updateDisplay(currentInput.toString(), formula.toString());
        }
    }

    private void percentage() {
        if (currentInput.length() > 0) {
            try {
                double value = Double.parseDouble(currentInput.toString());
                value = value / 100;
                currentInput = new StringBuilder(decimalFormat.format(value));
                updateDisplay(currentInput.toString(), formula.toString());
            } catch (NumberFormatException e) {
                hasError = true;
                updateDisplay("错误", "");
            }
        }
    }

    private void setOperator(String operator) {
        if (currentInput.length() > 0) {
            double currentValue = Double.parseDouble(currentInput.toString());

            if (!currentOperator.isEmpty() && !isNewInput) {
                // 连续运算
                previousResult = calculateResult(previousResult, currentValue, currentOperator);
                currentInput = new StringBuilder(decimalFormat.format(previousResult));
            } else {
                // 新的运算
                previousResult = currentValue;
            }

            formula.append(decimalFormat.format(previousResult)).append(" ").append(operator).append(" ");
            currentOperator = operator;
            isNewInput = true;

            updateDisplay(decimalFormat.format(previousResult), formula.toString());
        }
    }

    private void calculate() {
        if (currentInput.length() > 0 && !currentOperator.isEmpty()) {
            try {
                double currentValue = Double.parseDouble(currentInput.toString());
                double result = calculateResult(previousResult, currentValue, currentOperator);

                formula.append(decimalFormat.format(currentValue)).append(" =");
                updateDisplay(decimalFormat.format(result), formula.toString());

                // 准备下一次运算
                previousResult = result;
                currentInput = new StringBuilder(decimalFormat.format(result));
                currentOperator = "";
                isNewInput = true;
            } catch (Exception e) {
                hasError = true;
                updateDisplay("错误", "");
            }
        }
    }

    private double calculateResult(double operand1, double operand2, String operator) {
        switch (operator) {
            case "+":
                return operand1 + operand2;
            case "-":
                return operand1 - operand2;
            case "×":
                return operand1 * operand2;
            case "÷":
                if (operand2 == 0) {
                    hasError = true;
                    throw new ArithmeticException("除零错误");
                }
                return operand1 / operand2;
            default:
                return operand2;
        }
    }

    private void updateDisplay(String display, String formulaText) {
        displayTextView.setText(display);
        formulaTextView.setText(formulaText);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}