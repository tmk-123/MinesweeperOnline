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
        setSize(920, 570);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        mainPanel.setBackground(UITheme.BG_MAIN);
        mainPanel.add(new MinesweeperHeroPanel());

        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        formArea.setBackground(UITheme.BG_MAIN);
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(28, 30, 28, 30)));
        cardPanel.setBackground(UITheme.BG_CARD);
        cardPanel.setPreferredSize(new Dimension(370, 370));

        JLabel lblEyebrow = new JLabel("01  •  ĐẤU TRƯỜNG ONLINE");
        lblEyebrow.setFont(UITheme.FONT_SMALL_BOLD);
        lblEyebrow.setForeground(UITheme.COLOR_PRIMARY);
        lblEyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblTitle = new JLabel("Chào mừng trở lại");
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.TEXT_TITLE);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblSub = new JLabel("Đăng nhập để tiếp tục thi đấu");
        lblSub.setFont(UITheme.FONT_REGULAR);
        lblSub.setForeground(UITheme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(lblEyebrow);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(lblTitle);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(lblSub);
        cardPanel.add(Box.createVerticalStrut(28));

        JLabel lblUser = new JLabel("Tên đăng nhập");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_BODY);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtUsername = new JTextField();
        UITheme.styleTextField(txtUsername);
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_BODY);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPassword = new JPasswordField();
        UITheme.stylePasswordField(txtPassword);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardPanel.add(lblUser);
        cardPanel.add(Box.createVerticalStrut(7));
        cardPanel.add(txtUsername);
        cardPanel.add(Box.createVerticalStrut(16));
        cardPanel.add(lblPass);
        cardPanel.add(Box.createVerticalStrut(7));
        cardPanel.add(txtPassword);
        cardPanel.add(Box.createVerticalStrut(24));

        btnLogin = new JButton("Đăng nhập");
        UITheme.styleButton(btnLogin, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRegister = new JButton("Chưa có tài khoản?  Đăng ký ngay");
        UITheme.styleOutlineButton(btnRegister, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_BODY);
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnRegister.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(btnLogin);
        cardPanel.add(Box.createVerticalStrut(11));
        cardPanel.add(btnRegister);
        formArea.add(cardPanel);
        mainPanel.add(formArea);
        add(mainPanel);

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
