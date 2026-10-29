package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Themed confirmation shown before sending a one-to-one challenge. */
public class ChallengeConfirmDialog extends JDialog {
    private boolean confirmed;

    private ChallengeConfirmDialog(Window owner, String opponentName) {
        super(owner, "Xác nhận thách đấu", Dialog.ModalityType.APPLICATION_MODAL);
        setSize(480, 315);
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel scene = new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.DUEL, new BorderLayout(8, 8));
        scene.setBorder(new EmptyBorder(18, 26, 14, 26));

        JLabel title = new JLabel("THÁCH ĐẤU 1V1", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_LARGE_TITLE);
        title.setForeground(UITheme.COLOR_PRIMARY);
        scene.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        JPanel versus = new JPanel(new GridLayout(1, 3, 10, 0));
        versus.setOpaque(false);
        versus.add(createPlayer("Bạn", new Color(78, 164, 231)));
        JLabel vs = new JLabel("VS", SwingConstants.CENTER);
        vs.setFont(new Font("Segoe UI", Font.BOLD, 23));
        vs.setForeground(UITheme.COLOR_PRIMARY);
        versus.add(vs);
        versus.add(createPlayer(opponentName, UITheme.COLOR_DANGER));
        center.add(versus, BorderLayout.CENTER);

        JLabel prompt = new JLabel("Gửi lời mời đấu với " + opponentName + "?", SwingConstants.CENTER);
        prompt.setFont(UITheme.FONT_BOLD);
        prompt.setForeground(UITheme.TEXT_TITLE);
        center.add(prompt, BorderLayout.SOUTH);
        scene.add(center, BorderLayout.CENTER);

        JPanel actions = new GlassPanel(new FlowLayout(FlowLayout.CENTER, 16, 10), UITheme.BG_CARD, 132);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));
        JButton send = new JButton("Gửi lời mời");
        UITheme.styleButton(send, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        JButton cancel = new JButton("Hủy");
        UITheme.styleOutlineButton(cancel, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        actions.add(send);
        actions.add(cancel);
        scene.add(actions, BorderLayout.SOUTH);
        setContentPane(scene);

        send.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(send);
    }

    private JPanel createPlayer(String name, Color accent) {
        JPanel player = new JPanel(new BorderLayout(0, 5));
        player.setOpaque(false);
        JLabel avatar = new JLabel(GameUiIcons.playerAvatarIcon(name, accent, 68, false));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel label = new JLabel(name, SwingConstants.CENTER);
        label.setFont(UITheme.FONT_BOLD);
        label.setForeground(UITheme.TEXT_TITLE);
        player.add(avatar, BorderLayout.CENTER);
        player.add(label, BorderLayout.SOUTH);
        return player;
    }

    public static boolean confirm(Component parent, String opponentName) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ChallengeConfirmDialog dialog = new ChallengeConfirmDialog(owner, opponentName);
        dialog.setVisible(true);
        return dialog.confirmed;
    }
}
