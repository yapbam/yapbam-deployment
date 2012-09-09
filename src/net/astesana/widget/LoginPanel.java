package net.astesana.widget;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.astesana.ajlib.swing.widget.PasswordWidget;
import net.astesana.ajlib.swing.widget.TextWidget;
import javax.swing.JCheckBox;

import java.awt.event.ItemEvent;

@SuppressWarnings("serial")
public class LoginPanel extends JPanel {
	public final static String LOGIN_PROPERTY = "login";
	public static final class Login {
		private String user;
		private String password;
		
		public Login(String user, String password) {
			this.user = user;
			this.password = password;
		}

		/**
		 * @return the user
		 */
		public String getUser() {
			return user;
		}

		/**
		 * @return the password
		 */
		public String getPassword() {
			return password;
		}
		
		@Override
		public String toString() {
			return user+":"+password;
		}
	}
	
	private JLabel lblNewLabel;
	private TextWidget userName;
	private JLabel lblNewLabel_1;
	private PasswordWidget passwordField;
	private JCheckBox showPassword;
	
	private Login login;
	private PropertyChangeListener listener;

	/**
	 * Create the panel.
	 */
	public LoginPanel() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(getLblNewLabel(), gbc_lblNewLabel);
		GridBagConstraints gbc_userName = new GridBagConstraints();
		gbc_userName.insets = new Insets(0, 0, 0, 5);
		gbc_userName.anchor = GridBagConstraints.WEST;
		gbc_userName.gridx = 1;
		gbc_userName.gridy = 0;
		add(getUserName(), gbc_userName);
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.gridx = 2;
		gbc_lblNewLabel_1.gridy = 0;
		add(getLblNewLabel_1(), gbc_lblNewLabel_1);
		GridBagConstraints gbc_passwordField = new GridBagConstraints();
		gbc_passwordField.insets = new Insets(0, 0, 0, 5);
		gbc_passwordField.gridx = 3;
		gbc_passwordField.gridy = 0;
		add(getPasswordField(), gbc_passwordField);
		GridBagConstraints gbc_showPassword = new GridBagConstraints();
		gbc_showPassword.fill = GridBagConstraints.BOTH;
		gbc_showPassword.gridx = 4;
		gbc_showPassword.gridy = 0;
		add(getShowPassword(), gbc_showPassword);
		login = buildLogin();
	}
	
	@SuppressWarnings("deprecation")
	private Login buildLogin() {
		return new Login(getUserName().getText(), getPasswordField().getText());
	}
	
	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			lblNewLabel = new JLabel("Login:");
		}
		return lblNewLabel;
	}
	private TextWidget getUserName() {
		if (userName == null) {
			userName = new TextWidget(10);
			userName.addPropertyChangeListener(TextWidget.TEXT_PROPERTY, getListener());
		}
		return userName;
	}
	
	private PropertyChangeListener getListener() {
		if (listener==null) {
			listener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					Login oldValue = login;
					login = buildLogin();
					firePropertyChange(LOGIN_PROPERTY, oldValue, login);
				}
			};
		}
		return listener;
	}
	private JLabel getLblNewLabel_1() {
		if (lblNewLabel_1 == null) {
			lblNewLabel_1 = new JLabel("Password:");
		}
		return lblNewLabel_1;
	}
	private PasswordWidget getPasswordField() {
		if (passwordField == null) {
			passwordField = new PasswordWidget(10);
			passwordField.addPropertyChangeListener(PasswordWidget.TEXT_PROPERTY, getListener());
		}
		return passwordField;
	}
	private JCheckBox getShowPassword() {
		if (showPassword == null) {
			showPassword = new JCheckBox("show password");
			showPassword.addItemListener(new java.awt.event.ItemListener() {
				char oldEcho;
				@Override
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (e.getStateChange()==ItemEvent.DESELECTED) {
						passwordField.setEchoChar(oldEcho);
					} else {
						oldEcho = passwordField.getEchoChar();
						passwordField.setEchoChar((char) 0);
					}
				}
			});
		}
		return showPassword;
	}

	public Login getLogin() {
		return login;
	}
	
	public void setLogin(String user, String password) {
		getUserName().setText(user);
		getPasswordField().setText(password);
	}
}
