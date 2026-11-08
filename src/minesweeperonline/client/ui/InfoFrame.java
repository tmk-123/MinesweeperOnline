package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Cửa sổ thông tin không khóa cửa sổ chính, hỗ trợ nút phóng to/khôi phục của hệ điều hành và phím F11. */
public class InfoFrame extends JFrame {
    private final Frame parentFrame;
    private final WindowAdapter parentListener = new WindowAdapter() {
        @Override public void windowClosed(WindowEvent event) { dispose(); }
    };

    protected InfoFrame(Frame parentFrame, String title) {
        super(title);
        this.parentFrame = parentFrame;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (parentFrame != null) parentFrame.addWindowListener(parentListener);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleMaximize");
        getRootPane().getActionMap().put("toggleMaximize", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                setExtendedState((getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH
                        ? NORMAL : MAXIMIZED_BOTH);
            }
        });
    }

    protected Frame getParentFrame() { return parentFrame; }

    @Override public void dispose() {
        if (parentFrame != null) parentFrame.removeWindowListener(parentListener);
        super.dispose();
    }
}
