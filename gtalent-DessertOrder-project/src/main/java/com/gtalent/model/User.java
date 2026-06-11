package com.gtalent.model;

public class User {
    private String customerName;
    private String phone;
    private String lineAccount;
    private int qtyCookie;
    private int qtyBrownie;
    private int qtyBasque;
    private int qtyLemonTart;
    private int qtyLemonCake;
    private int subtotal;
    private int shipping;
    private int total;

    // Getters and Setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLineAccount() { return lineAccount; }
    public void setLineAccount(String lineAccount) { this.lineAccount = lineAccount; }

    public int getQtyCookie() { return qtyCookie; }
    public void setQtyCookie(int qtyCookie) { this.qtyCookie = qtyCookie; }

    public int getQtyBrownie() { return qtyBrownie; }
    public void setQtyBrownie(int qtyBrownie) { this.qtyBrownie = qtyBrownie; }

    public int getQtyBasque() { return qtyBasque; }
    public void setQtyBasque(int qtyBasque) { this.qtyBasque = qtyBasque; }

    public int getQtyLemonTart() { return qtyLemonTart; }
    public void setQtyLemonTart(int qtyLemonTart) { this.qtyLemonTart = qtyLemonTart; }

    public int getQtyLemonCake() { return qtyLemonCake; }
    public void setQtyLemonCake(int qtyLemonCake) { this.qtyLemonCake = qtyLemonCake; }

    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }

    public int getShipping() { return shipping; }
    public void setShipping(int shipping) { this.shipping = shipping; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
