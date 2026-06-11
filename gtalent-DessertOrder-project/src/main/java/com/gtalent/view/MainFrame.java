package com.gtalent.view;

import com.gtalent.controller.UserController;
import com.gtalent.model.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class MainFrame extends JFrame {
    // 控制器
    private final UserController controller;

    // UI 元件
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField lineField;
    private JTextArea cartTextArea;
    private JLabel subtotalLabel;
    private JLabel shippingLabel;
    private JLabel totalLabel;

    // 購物車資料結構
    private final int[] quantities = new int[5]; // 5 種甜點數量

    public MainFrame(UserController controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("甜點快速下單系統");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1200, 700);
        setLocationRelativeTo(null);

        // 創建各個面板
        createLeftPanel();
        createMiddlePanel();
        createRightPanel();

        setVisible(true);
    }

    private void createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("甜點選購區"));
        leftPanel.setBackground(new Color(253, 251, 247));
        leftPanel.setPreferredSize(new Dimension(400, 600));

        // 主容器使用垂直BoxLayout
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] items = {"美式餅乾 NT$120", "布朗尼 NT$150", "巴斯克 NT$450", "法式檸檬塔 NT$380", "老奶奶檸檬棒蛋糕 NT$420"};

        // 建立每個甜點的卡片型面板
        for (int i = 0; i < items.length; i++) {
            JPanel productCard = createProductCard(i, items[i]);
            menuPanel.add(productCard);
            menuPanel.add(Box.createVerticalStrut(15)); // 卡片間距
        }

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
    }

    private JPanel createProductCard(int index, String itemName) {
        // 建立卡片面板
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // 第一行：產品名稱與價格
        JLabel nameLabel = new JLabel(itemName);
        nameLabel.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 第二行：數量選擇器
        JPanel quantityPanel = new JPanel();
        quantityPanel.setBackground(Color.WHITE);
        quantityPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        quantityPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel qtyLabel = new JLabel("數量：");
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
        spinner.setPreferredSize(new Dimension(60, 25));

        // 儲存spinner引用以供後續使用
        final JSpinner finalSpinner = spinner;

        quantityPanel.add(qtyLabel);
        quantityPanel.add(spinner);

        // 第三行：加入購物車按鈕
        JButton addButton = new JButton("加入購物車");
        addButton.setBackground(new Color(144, 238, 144)); // 淺綠色
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setPreferredSize(new Dimension(200, 35));
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 180, 100)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // 設定按鈕事件處理
        addButton.addActionListener(e -> {
            int quantity = (Integer) finalSpinner.getValue();
            quantities[index] = quantity;
            updateCart(index, quantity);
        });

        // 將所有元件加入卡片
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(quantityPanel);
        card.add(Box.createVerticalStrut(15));
        card.add(addButton);

        return card;
    }

    private void createMiddlePanel() {
        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.setBorder(BorderFactory.createTitledBorder("我的購物車"));
        middlePanel.setBackground(new Color(253, 251, 247));
        middlePanel.setPreferredSize(new Dimension(300, 600));

        // 購物車內容顯示
        cartTextArea = new JTextArea();
        cartTextArea.setEditable(false);
        cartTextArea.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        cartTextArea.setBackground(Color.WHITE);
        cartTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JScrollPane scrollPane = new JScrollPane(cartTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 計算按鈕面板
        JPanel bottomPanel = new JPanel(new GridLayout(3, 2));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        subtotalLabel = new JLabel("小計：NT$0");
        shippingLabel = new JLabel("運費：NT$100");
        totalLabel = new JLabel("總金額：NT$0");

        // 設定字體與樣式
        Font font = new Font("微軟正黑體", Font.BOLD, 14);
        subtotalLabel.setFont(font);
        shippingLabel.setFont(font);
        totalLabel.setFont(font);

        bottomPanel.add(subtotalLabel);
        bottomPanel.add(new JLabel(""));
        bottomPanel.add(shippingLabel);
        bottomPanel.add(new JLabel(""));
        bottomPanel.add(totalLabel);

        // 清空購物車按鈕
        JButton clearButton = new JButton("清空購物車");
        clearButton.setBackground(Color.RED);
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 0, 0)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        clearButton.addActionListener(e -> clearCart());

        JPanel clearPanel = new JPanel();
        clearPanel.add(clearButton);
        clearPanel.setBackground(Color.WHITE);

        middlePanel.add(scrollPane, BorderLayout.CENTER);
        middlePanel.add(bottomPanel, BorderLayout.SOUTH);
        middlePanel.add(clearPanel, BorderLayout.NORTH);

        add(middlePanel, BorderLayout.CENTER);
    }

    private void createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("客戶資料"));
        rightPanel.setBackground(new Color(253, 251, 247));
        rightPanel.setPreferredSize(new Dimension(400, 600));

        // 客戶資料輸入區
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // 姓名欄位
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.setBackground(Color.WHITE);
        namePanel.add(new JLabel("客戶姓名："));
        nameField = new JTextField(20);
        namePanel.add(nameField);

        // 電話欄位
        JPanel phonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        phonePanel.setBackground(Color.WHITE);
        phonePanel.add(new JLabel("電話："));
        phoneField = new JTextField(20);
        phonePanel.add(phoneField);

        // LINE 帳號欄位
        JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linePanel.setBackground(Color.WHITE);
        linePanel.add(new JLabel("LINE 帳號："));
        lineField = new JTextField(20);
        linePanel.add(lineField);

        // 下單時間 (唯讀)
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.setBackground(Color.WHITE);
        timePanel.add(new JLabel("下單時間："));
        JLabel timeLabel = new JLabel();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timeLabel.setText(LocalDateTime.now().format(formatter));
        timePanel.add(timeLabel);

        inputPanel.add(namePanel);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(phonePanel);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(linePanel);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(timePanel);

        // 結帳按鈕
        JButton confirmButton = new JButton("確認下單結帳");
        confirmButton.setBackground(new Color(255, 215, 0)); // 金黃色
        confirmButton.setForeground(Color.BLACK);
        confirmButton.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        confirmButton.setPreferredSize(new Dimension(380, 50));
        confirmButton.setFocusPainted(false);
        confirmButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 170, 0)),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        confirmButton.addActionListener(e -> {
            String validationMessage = validateInputs();
            if (validationMessage != null) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "請填寫正確資訊：" + validationMessage,
                        "輸入錯誤",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                calculateTotal();
                saveOrder();
            }
        });

        rightPanel.add(inputPanel, BorderLayout.CENTER);
        rightPanel.add(confirmButton, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
    }

    private String validateInputs() {
        // 驗證姓名
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            return "姓名不可為空";
        }

        // 驗證電話
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            return "電話號碼不可為空";
        }
        if (!phone.matches("^09\\d{8}$")) {
            return "電話號碼格式不符（應為09xxxxxxxx）";
        }

        // 驗證LINE帳號
        String line = lineField.getText().trim();
        if (line.isEmpty()) {
            return "LINE 帳號不可為空";
        }
        if (!line.matches("^[a-zA-Z0-9_\\-]{5,30}$")) {
            return "LINE 帳號格式不符（5-30個字母、數字、底線或連字號）";
        }

        // 驗證購物車是否為空
        boolean hasItems = false;
        for (int qty : quantities) {
            if (qty > 0) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            return "購物車內至少需有一項商品";
        }

        return null; // 驗證通過
    }

    private void updateCart(int index, int quantity) {
        String[] items = {"美式餅乾", "布朗尼", "巴斯克", "法式檸檬塔", "老奶奶檸檬棒蛋糕"};
        int[] prices = {120, 150, 450, 380, 420};

        // 如果數量為0，從購物車中移除該項目
        if (quantity == 0) {
            // 移除該項目
            updateCartDisplay();
        } else {
            // 新增或更新項目到購物車
            cartTextArea.append(String.format("%s x%d = NT$%d\n",
                    items[index], quantity, quantity * prices[index]));
        }

        // 重新計算總金額
        calculateTotal();
    }

    private void updateCartDisplay() {
        // 清空購物車顯示區域
        cartTextArea.setText("");

        // 根據目前數量重新顯示
        String[] items = {"美式餅乾", "布朗尼", "巴斯克", "法式檸檬塔", "老奶奶檸檬棒蛋糕"};
        int[] prices = {120, 150, 450, 380, 420};

        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] > 0) {
                cartTextArea.append(String.format("%s x%d = NT$%d\n",
                        items[i], quantities[i], quantities[i] * prices[i]));
            }
        }
    }

    private void clearCart() {
        // 清空所有數量
        Arrays.fill(quantities, 0);

        // 清空購物車顯示
        cartTextArea.setText("");

        // 重新計算總金額
        calculateTotal();
    }

    private void calculateTotal() {
        int[] prices = {120, 150, 450, 380, 420};

        // 計算小計
        int subtotal = 0;
        for (int i = 0; i < quantities.length; i++) {
            subtotal += quantities[i] * prices[i];
        }

        // 計算運費
        int shipping = subtotal >= 2000 ? 0 : 100;
        int total = subtotal + shipping;

        subtotalLabel.setText("小計：NT$" + subtotal);
        shippingLabel.setText(subtotal >= 2000 ? "運費：FREE" : "運費：NT$100");
        totalLabel.setText("總金額：NT$" + total);
    }

    private void saveOrder() {
        User user = new User();
        user.setCustomerName(nameField.getText());
        user.setPhone(phoneField.getText());
        user.setLineAccount(lineField.getText());

        // 計算小計、運費、總金額
        int[] prices = {120, 150, 450, 380, 420};

        int subtotal = 0;
        for (int i = 0; i < quantities.length; i++) {
            subtotal += quantities[i] * prices[i];
        }

        int shipping = subtotal >= 2000 ? 0 : 100;
        int total = subtotal + shipping;

        // 設置訂單資訊
        user.setQtyCookie(quantities[0]);
        user.setQtyBrownie(quantities[1]);
        user.setQtyBasque(quantities[2]);
        user.setQtyLemonTart(quantities[3]);
        user.setQtyLemonCake(quantities[4]);
        user.setSubtotal(subtotal);
        user.setShipping(shipping);
        user.setTotal(total);

        try {
            // 呼叫控制器儲存訂單
            if (controller.saveOrder(user)) {
                JOptionPane.showMessageDialog(this, "訂單已成功結帳！", "成功", JOptionPane.INFORMATION_MESSAGE);
                clearCart(); // 清空購物車
                nameField.setText("");
                phoneField.setText("");
                lineField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "訂單儲存失敗，請重新嘗試。", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "儲存訂單時發生錯誤：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
}
