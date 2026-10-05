package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Registration window for creating new player accounts.
 */
public class RegisterFrame extends JFrame {

    private final ServerConnection connection;
    private final LoginFrame loginFrame;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    private JButton btnBack;

    public RegisterFrame(ServerConnection connection, LoginFrame loginFrame) {
        this.connection = connection;
        this.loginFrame = loginFrame;
        initComponents();
        setupNetworkListener();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Đăng ký tài khoản");
        setSize(400, 280);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("ĐĂNG KÝ TÀI KHOẢN MỚI", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setForeground(new Color(40, 130, 70));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);

        formPanel.add(new JLabel("Nhập lại mật khẩu:"));
        txtConfirmPassword = new JPasswordField();
        formPanel.add(txtConfirmPassword);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnRegister = new JButton("Đăng ký");
        btnRegister.setFont(new Font("Arial", Font.BOLD, 12));
        btnBack = new JButton("Quay lại");

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnBack);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        btnRegister.addActionListener(e -> performRegister());
        btnBack.addActionListener(e -> {
            this.dispose();
            loginFrame.setVisible(true);
        });
    }

    private void performRegister() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tên đăng nhập và mật khẩu!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Mật khẩu xác nhận không khớp!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        Message registerMsg = JsonProtocol.createRegister(username, password);
        connection.sendMessage(registerMsg);
    }

    private void setupNetworkListener() {
        connection.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.REGISTER_SUCCESS) {
                    JOptionPane.showMessageDialog(this,
                            "Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    this.dispose();
                    loginFrame.setVisible(true);

                } else if (message.getType() == MessageType.REGISTER_FAILED) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký");

                    String reason = message.getPayload().has("reason") ?
                            message.getPayload().get("reason").getAsString() : "Đăng ký thất bại!";
                    JOptionPane.showMessageDialog(this, reason, "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }
}
