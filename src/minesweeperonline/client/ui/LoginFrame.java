package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ đăng nhập hiện đại của MinesweeperOnline.
 */
public class LoginFrame extends JFrame {

    private final ServerConnection connection;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private RegisterFrame registerFrame;
    private ServerConnection.MessageListener loginListener;

    public LoginFrame(ServerConnection connection) {
        this.connection = connection;
        initComponents();
        setupNetworkListener();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Đăng nhập");
        setSize(420, 390);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        mainPanel.setBackground(UITheme.BG_CARD);

        // Card bao ngoài
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        cardWrapper.setBackground(UITheme.BG_MAIN);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));
        cardPanel.setBackground(UITheme.BG_CARD);

        // Header Tiêu đề
        JLabel lblTitle = new JLabel("MINESWEEPER ONLINE", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Đấu trí dò mìn đối kháng thời gian thực", SwingConstants.CENTER);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(lblTitle);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(lblSub);
        cardPanel.add(Box.createVerticalStrut(20));

        // Form Fields
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_TITLE);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new JTextField();
        UITheme.styleTextField(txtUsername);
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_TITLE);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField();
        UITheme.stylePasswordField(txtPassword);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel formBox = new JPanel();
        formBox.setLayout(new BoxLayout(formBox, BoxLayout.Y_AXIS));
        formBox.setOpaque(false);
        formBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        formBox.add(lblUser);
        formBox.add(Box.createVerticalStrut(4));
        formBox.add(txtUsername);
        formBox.add(Box.createVerticalStrut(12));
        formBox.add(lblPass);
        formBox.add(Box.createVerticalStrut(4));
        formBox.add(txtPassword);

        cardPanel.add(formBox);
        cardPanel.add(Box.createVerticalStrut(22));

        // Các nút bấm
        btnLogin = new JButton("Đăng nhập");
        UITheme.styleButton(btnLogin, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, Color.WHITE);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRegister = new JButton("Chưa có tài khoản? Đăng ký ngay");
        UITheme.styleOutlineButton(btnRegister, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY, UITheme.COLOR_PRIMARY);
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(btnLogin);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(btnRegister);

        cardWrapper.add(cardPanel, BorderLayout.CENTER);
        add(cardWrapper);

        // Gắn các action listener
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        btnRegister.addActionListener(e -> {
            if (registerFrame == null || !registerFrame.isDisplayable()) {
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

    /**
     * Điền sẵn thông tin đăng nhập (ví dụ sau khi đăng ký tài khoản thành công).
     */
    public void setCredentials(String username, String password) {
        if (txtUsername != null) {
            txtUsername.setText(username);
        }
        if (txtPassword != null) {
            txtPassword.setText(password);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(true);
            btnLogin.setText("Đăng nhập");
            btnLogin.requestFocusInWindow();
        }
    }

    @Override
    public void dispose() {
        // Nếu không gỡ listener, frame đã dispose vẫn tiếp tục phản hồi LOGIN_SUCCESS sau khi đăng xuất
        connection.removeMessageListener(loginListener);
        super.dispose();
    }

    private void setupNetworkListener() {
        loginListener = message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.LOGIN_SUCCESS) {
                    int userId = message.getPayload().get("userId").getAsInt();
                    String username = message.getPayload().get("username").getAsString();
                    int totalScore = message.getPayload().get("totalScore").getAsInt();

                    User loggedUser = new User(userId, username, totalScore);

                    // Mở MainMenuFrame và đóng LoginFrame
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
        };
        connection.addMessageListener(loginListener);
    }
}
