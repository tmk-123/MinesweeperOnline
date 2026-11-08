package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import java.awt.*;

/** Apply the same readable buttons to every Swing message and confirmation dialog. */
public final class ThemedOptionPaneUI extends BasicOptionPaneUI {
    /** Cung cấp UI delegate để Swing áp dụng kiểu hộp thoại tùy chỉnh. */
    public static ComponentUI createUI(JComponent component) {
        return new ThemedOptionPaneUI();
    }

    /** Tạo vùng nút của JOptionPane rồi áp dụng kiểu nút thống nhất. */
    @Override
    protected Container createButtonArea() {
        Container area = super.createButtonArea();
        styleButtons(area);
        return area;
    }

    /** Duyệt các component con để áp dụng giao diện cho nút trong hộp thoại. */
    private void styleButtons(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                UITheme.styleOutlineButton(button, UITheme.BORDER_CARD,
                        UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
            } else if (component instanceof Container child) {
                styleButtons(child);
            }
        }
    }
}
