package com.gtalent.controller;

import com.gtalent.dao.UserDAO;
import com.gtalent.dao.UserDAOImpl;
import com.gtalent.model.User;
import com.gtalent.util.Validator;

public class UserController {
    private final UserDAO userDAO;

    public UserController() {
        this.userDAO = new UserDAOImpl();
    }

    public boolean validateUser(User user) {
        return Validator.isValidName(user.getCustomerName()) &&
                Validator.isValidPhone(user.getPhone()) &&
                Validator.isValidLineAccount(user.getLineAccount());
    }

    public boolean saveOrder(User user) {
        userDAO.save(user);
        return true;
    }
}
