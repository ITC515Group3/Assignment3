package library.panels.borrow;

/**
 * Created by Tom on 16/9/16.
 */

import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

public class CancelledPanel extends ABorrowPanel {
    private static final long serialVersionUID = 1L;

    public CancelledPanel() {
        setLayout(null);
        setBorder(new TitledBorder(null, "Cancelled", 4, 2, null, null));
        setBounds(12, 23, 460, 640);

        JLabel lblCancelled = new JLabel("Cancelled");
        lblCancelled.setHorizontalAlignment(0);
        lblCancelled.setFont(new Font("Tahoma", 0, 42));
        lblCancelled.setBounds(12, 187, 436, 78);
        add(lblCancelled);
    }
}