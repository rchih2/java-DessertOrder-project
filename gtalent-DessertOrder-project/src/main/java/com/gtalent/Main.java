package com.gtalent;

import com.gtalent.controller.UserController;
import com.gtalent.view.MainFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame(new UserController()));
    }
}
