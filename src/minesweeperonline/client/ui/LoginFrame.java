package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Login window for MinesweeperOnline.
 */
public class LoginFrame extends JFrame {

    private final ServerConnection connection;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private RegisterFrame registerFrame;

    public LoginFrame(ServerConnection connection) {
        this.connection = connection;
        initComponents();
        setupNetworkListener();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Đăng nhập");
        setSize(380, 240);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Title
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setForeground(new Color(30, 80, 150));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 12));
        btnRegister = new JButton("Đăng ký mới");

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Action listeners
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());

        btnRegister.addActionListener(e -> {
            if (registerFrame == null) {
                registerFrame = new RegisterFrame(connection, this);
            }
            registerFrame.setVisible(true);
            this.setVisible(false);
        });
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        Message loginMsg = JsonProtocol.createLogin(username, password);
        connection.sendMessage(loginMsg);
    }

    private void setupNetworkListener() {
        connection.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.LOGIN_SUCCESS) {
                    int userId = message.getPayload().get("userId").getAsInt();
                    String username = message.getPayload().get("username").getAsString();
                    int totalScore = message.getPayload().get("totalScore").getAsInt();

                    User loggedUser = new User(userId, username, totalScore);

                    // Open MainMenuFrame and close LoginFrame
                    MainMenuFrame mainMenu = new MainMenuFrame(connection, loggedUser);
                    mainMenu.setVisible(true);
                    this.dispose();

                } else if (message.getType() == MessageType.LOGIN_FAILED) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");

                    String reason = message.getPayload().has("reason") ?
                            message.getPayload().get("reason").getAsString() : "Đăng nhập thất bại!";
                    JOptionPane.showMessageDialog(this, reason, "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }
}
