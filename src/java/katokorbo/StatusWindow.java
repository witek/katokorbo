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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class StatusWindow extends JFrame {

	private JTextField statusField;
	private JButton browserButton;

	public StatusWindow() {
		getContentPane().add(createContent());

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new Adapter());

		setTitle(Katokorbo.appTitle);
		pack();
		setLocation(100, 100);
		setVisible(true);
	}

	public void setStatus(String status) {
		statusField.setText(status);
	}

	public void setReady(final int port) {
		final String url = "http://localhost:" + port + "/";
		setStatus("Started on " + url);
		browserButton.setEnabled(true);
		browserButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Utl.startBrowser(url);
				} catch (Throwable ex) {
					Katokorbo.error("Starting browser failed. " + ex.getLocalizedMessage());
				}
			}
		});
	}

	private Component createContent() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(createHead(), BorderLayout.NORTH);
		panel.add(new SysOutPanel(), BorderLayout.CENTER);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		return panel;
	}

	private Component createHead() {
		browserButton = new JButton("Open in Browser");
		browserButton.setEnabled(false);

		statusField = new JTextField(30);
		statusField.setText("Starting...");
		statusField.setEditable(false);
		// statusField.setPreferredSize(new Dimension((int) statusField.getPreferredSize().getWidth(), (int)
		// browserButton
		// .getPreferredSize().getHeight()));

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(statusField, BorderLayout.CENTER);
		panel.add(browserButton, BorderLayout.EAST);

		return panel;
	}

	private class Adapter extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent e) {
			Katokorbo.shutdown(0);
		}
	}

}
