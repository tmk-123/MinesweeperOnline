package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ đăng ký hiện đại để tạo tài khoản người chơi mới.
 */
public class RegisterFrame extends JFrame {

    private final ServerConnection connection;
    private final LoginFrame loginFrame;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    private JButton btnBack;
    private ServerConnection.MessageListener registerListener;

    private String lastRegisteredUsername;
    private String lastRegisteredPassword;

    public RegisterFrame(ServerConnection connection, LoginFrame loginFrame) {
        this.connection = connection;
        this.loginFrame = loginFrame;
        initComponents();
        setupNetworkListener();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Đăng ký tài khoản");
        setSize(920, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        mainPanel.setBackground(UITheme.BG_MAIN);
        mainPanel.add(new MinesweeperHeroPanel());
        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setBorder(BorderFactory.createEmptyBorder(24, 34, 24, 34));
        formArea.setBackground(UITheme.BG_MAIN);
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));
        cardPanel.setBackground(UITheme.BG_CARD);
        cardPanel.setPreferredSize(new Dimension(370, 455));

        // Header Tiêu đề
        JLabel lblTitle = new JLabel("ĐĂNG KÝ TÀI KHOẢN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Gia nhập đấu trường Minesweeper Online", SwingConstants.CENTER);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel eyebrow = new JLabel("01  •  TẠO TÀI KHOẢN", SwingConstants.CENTER);
        eyebrow.setFont(UITheme.FONT_SMALL_BOLD);
        eyebrow.setForeground(UITheme.COLOR_PRIMARY);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(eyebrow);
        cardPanel.add(Box.createVerticalStrut(9));
        cardPanel.add(lblTitle);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(lblSub);
        cardPanel.add(Box.createVerticalStrut(18));

        // Form Fields
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_BODY);

        txtUsername = new JTextField();
        UITheme.styleTextField(txtUsername);
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_BODY);

        txtPassword = new JPasswordField();
        UITheme.stylePasswordField(txtPassword);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblConfirm = new JLabel("Nhập lại mật khẩu:");
        lblConfirm.setFont(UITheme.FONT_BOLD);
        lblConfirm.setForeground(UITheme.TEXT_BODY);

        txtConfirmPassword = new JPasswordField();
        UITheme.stylePasswordField(txtConfirmPassword);
        txtConfirmPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtConfirmPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formBox = new JPanel();
        formBox.setLayout(new BoxLayout(formBox, BoxLayout.Y_AXIS));
        formBox.setOpaque(false);
        formBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        formBox.add(lblUser);
        formBox.add(Box.createVerticalStrut(3));
        formBox.add(txtUsername);
        formBox.add(Box.createVerticalStrut(10));
        formBox.add(lblPass);
        formBox.add(Box.createVerticalStrut(3));
        formBox.add(txtPassword);
        formBox.add(Box.createVerticalStrut(10));
        formBox.add(lblConfirm);
        formBox.add(Box.createVerticalStrut(3));
        formBox.add(txtConfirmPassword);

        cardPanel.add(formBox);
        cardPanel.add(Box.createVerticalStrut(20));

        // Các nút bấm
        btnRegister = new JButton("Đăng ký tài khoản");
        UITheme.styleButton(btnRegister, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnBack = new JButton("Đã có tài khoản? Quay lại đăng nhập");
        UITheme.styleOutlineButton(btnBack, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_BODY);
        btnBack.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(btnRegister);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(btnBack);

        formArea.add(cardPanel);
        mainPanel.add(formArea);
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

        this.lastRegisteredUsername = username;
        this.lastRegisteredPassword = password;

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        Message registerMsg = JsonProtocol.createRegister(username, password);
        connection.sendMessage(registerMsg);
    }

    @Override
    public void dispose() {
        connection.removeMessageListener(registerListener);
        super.dispose();
    }

    private void setupNetworkListener() {
        registerListener = message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.REGISTER_SUCCESS) {
                    JOptionPane.showMessageDialog(this,
                            "Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    this.dispose();
                    if (loginFrame != null) {
                        loginFrame.setCredentials(lastRegisteredUsername, lastRegisteredPassword);
                        loginFrame.setVisible(true);
                    }

                } else if (message.getType() == MessageType.REGISTER_FAILED) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký tài khoản");

                    String reason = message.getPayload().has("reason") ?
                            message.getPayload().get("reason").getAsString() : "Đăng ký thất bại!";
                    JOptionPane.showMessageDialog(this, reason, "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
                }
            });
        };
        connection.addMessageListener(registerListener);
    }
}
