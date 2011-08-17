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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class SysOutPanel extends JPanel {

	private StringBuilder buffer = new StringBuilder();
	private JEditorPane textPane = new JEditorPane("text/plain", "");

	public SysOutPanel() {
		super(new BorderLayout());

		textPane.setEditable(false);
		textPane.setSize(new Dimension(3000, 300));
		textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JScrollPane scroller = new JScrollPane(textPane);
		scroller.setPreferredSize(new Dimension(750, 450));
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		scroller.setBorder(new TitledBorder("Log"));

		add(scroller, BorderLayout.CENTER);

		System.setOut(new PrintStream(new CustomStream(System.out)));
		System.setErr(new PrintStream(new CustomStream(System.err)));
	}

	public void update() {
		synchronized (buffer) {
			textPane.setText(buffer.toString());
		}
	}

	private class CustomStream extends OutputStream {

		private PrintStream original;
		private StringBuilder sb = new StringBuilder();

		public CustomStream(PrintStream original) {
			super();
			this.original = original;
		}

		@Override
		public synchronized void write(int b) throws IOException {
			sb.append((char) b);
			if (b == 10) flush();
		}

		@Override
		public synchronized void flush() throws IOException {
			String s = sb.toString();
			sb = new StringBuilder();
			original.print(s);
			synchronized (buffer) {
				buffer.append(s);
			}
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					update();
				}
			});
		}

	}

}
