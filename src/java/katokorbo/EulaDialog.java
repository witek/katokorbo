/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package katokorbo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class EulaDialog {

	public static boolean ask(String eula) {
		JEditorPane textPane = new JEditorPane("text/plain", eula);
		textPane.setEditable(false);
		textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane scroller = new JScrollPane(textPane);
		scroller.setPreferredSize(new Dimension(600, 400));

		JLabel agreeLabel = new JLabel("Do you agree with this terms of use?");

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(scroller, BorderLayout.CENTER);
		panel.add(agreeLabel, BorderLayout.SOUTH);

		String title = "End User License Agreement";
		return JOptionPane.showConfirmDialog(Katokorbo.window, panel, title, JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
	}
}
