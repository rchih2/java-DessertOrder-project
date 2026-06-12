CREATE DATABASE IF NOT EXISTS dessert_db;
USE dessert_db;

CREATE TABLE orders (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        customer_name VARCHAR(50),
                        phone VARCHAR(20),
                        line_account VARCHAR(50),
                        qty_cookie INT,
                        qty_brownie INT,
                        qty_basque INT,
                        qty_lemon_tart INT,
                        qty_lemon_cake INT,
                        subtotal INT,
                        shipping INT,
                        total INT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
