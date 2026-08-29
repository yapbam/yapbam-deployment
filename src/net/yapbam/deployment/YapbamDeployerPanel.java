package net.yapbam.deployment;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.border.TitledBorder;

import net.astesana.widget.LoginPanel;

import javax.swing.JLabel;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.UIManager;

import com.fathzer.soft.ajlib.swing.widget.TextWidget;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class YapbamDeployerPanel extends JPanel {
	private JPanel panel;
	private JPanel panel_1;
	private JButton btnNewButton;
	private LoginPanel loginPanel;
	private JLabel lblVersionToDeploy;
	private TextWidget toDeploy;
	private JLabel lblVersionToRemove;
	private TextWidget toRemove;
	private JLabel lblDeploymentSourceDirectory;
	private TextWidget srcFolder;
	private JCheckBox betaCheckBox;
	/**
	 * Create the panel.
	 */
	public YapbamDeployerPanel() {
		setLayout(new BorderLayout(0, 0));
		add(getPanel_1(), BorderLayout.CENTER);
		add(getPanel_1_1(), BorderLayout.SOUTH);

	}
	private JPanel getPanel_1() {
		if (panel == null) {
			panel = new JPanel();
			GridBagLayout gbl_panel = new GridBagLayout();
			gbl_panel.columnWeights = new double[]{0.0, 1.0};
			panel.setLayout(gbl_panel);
			GridBagConstraints gbc_loginPanel = new GridBagConstraints();
			gbc_loginPanel.gridwidth = 0;
			gbc_loginPanel.insets = new Insets(0, 0, 5, 0);
			gbc_loginPanel.anchor = GridBagConstraints.WEST;
			gbc_loginPanel.weightx = 1.0;
			gbc_loginPanel.gridx = 0;
			gbc_loginPanel.gridy = 0;
			panel.add(getLoginPanel(), gbc_loginPanel);
			GridBagConstraints gbc_lblDeploymentSourceDirectory = new GridBagConstraints();
			gbc_lblDeploymentSourceDirectory.anchor = GridBagConstraints.WEST;
			gbc_lblDeploymentSourceDirectory.insets = new Insets(0, 5, 5, 5);
			gbc_lblDeploymentSourceDirectory.gridx = 0;
			gbc_lblDeploymentSourceDirectory.gridy = 1;
			panel.add(getLblDeploymentSourceDirectory(), gbc_lblDeploymentSourceDirectory);
			GridBagConstraints gbc_srcFolder = new GridBagConstraints();
			gbc_srcFolder.fill = GridBagConstraints.HORIZONTAL;
			gbc_srcFolder.anchor = GridBagConstraints.WEST;
			gbc_srcFolder.insets = new Insets(0, 0, 5, 0);
			gbc_srcFolder.gridx = 1;
			gbc_srcFolder.gridy = 1;
			panel.add(getSrcFolder(), gbc_srcFolder);
			GridBagConstraints gbc_lblVersionToDeploy = new GridBagConstraints();
			gbc_lblVersionToDeploy.anchor = GridBagConstraints.WEST;
			gbc_lblVersionToDeploy.insets = new Insets(0, 5, 5, 5);
			gbc_lblVersionToDeploy.gridx = 0;
			gbc_lblVersionToDeploy.gridy = 2;
			panel.add(getLblVersionToDeploy(), gbc_lblVersionToDeploy);
			GridBagConstraints gbc_toDeploy = new GridBagConstraints();
			gbc_toDeploy.insets = new Insets(0, 0, 5, 0);
			gbc_toDeploy.anchor = GridBagConstraints.WEST;
			gbc_toDeploy.gridx = 1;
			gbc_toDeploy.gridy = 2;
			panel.add(getToDeploy(), gbc_toDeploy);
			GridBagConstraints gbc_lblVersionToRemove = new GridBagConstraints();
			gbc_lblVersionToRemove.anchor = GridBagConstraints.EAST;
			gbc_lblVersionToRemove.insets = new Insets(0, 5, 5, 5);
			gbc_lblVersionToRemove.gridx = 0;
			gbc_lblVersionToRemove.gridy = 3;
			panel.add(getLblVersionToRemove(), gbc_lblVersionToRemove);
			GridBagConstraints gbc_toRemove = new GridBagConstraints();
			gbc_toRemove.insets = new Insets(0, 0, 5, 0);
			gbc_toRemove.anchor = GridBagConstraints.WEST;
			gbc_toRemove.gridx = 1;
			gbc_toRemove.gridy = 3;
			panel.add(getToRemove(), gbc_toRemove);
			GridBagConstraints gbcBetaCheckBox = new GridBagConstraints();
			gbcBetaCheckBox.anchor = GridBagConstraints.WEST;
			gbcBetaCheckBox.insets = new Insets(0, 5, 0, 5);
			gbcBetaCheckBox.gridx = 0;
			gbcBetaCheckBox.gridy = 4;
			panel.add(getBetaCheckBox(), gbcBetaCheckBox);
		}
		return panel;
	}
	private JPanel getPanel_1_1() {
		if (panel_1 == null) {
			panel_1 = new JPanel();
			panel_1.setLayout(new BorderLayout(0, 0));
			panel_1.add(getBtnNewButton(), BorderLayout.EAST);
		}
		return panel_1;
	}
	private JButton getBtnNewButton() {
		if (btnNewButton == null) {
			btnNewButton = new JButton("Start ...");
			btnNewButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try (DeployYapbam yapbamDeployer = new DeployYapbam(getLoginPanel().getLogin().getUser(), getLoginPanel().getLogin().getPassword(),
							getSrcFolder().getText().trim(), getToDeploy().getText(), getToRemove().getText(), getBetaCheckBox().isSelected())) {
						yapbamDeployer.doIt();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(btnNewButton, "An error occurred", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		}
		return btnNewButton;
	}
	private LoginPanel getLoginPanel() {
		if (loginPanel == null) {
			loginPanel = new LoginPanel();
			loginPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Yapbam site access", TitledBorder.LEFT, TitledBorder.TOP));
			loginPanel.addPropertyChangeListener(LoginPanel.LOGIN_PROPERTY, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					System.out.println (evt.getOldValue()+" -> "+evt.getNewValue());
				}
			});
		}
		return loginPanel;
	}
	
	public LoginPanel.Login getLogin() {
		return getLoginPanel().getLogin();
	}
	private JLabel getLblVersionToDeploy() {
		if (lblVersionToDeploy == null) {
			lblVersionToDeploy = new JLabel("Version to deploy:");
		}
		return lblVersionToDeploy;
	}
	private TextWidget getToDeploy() {
		if (toDeploy == null) {
			toDeploy = new TextWidget();
			toDeploy.setColumns(10);
		}
		return toDeploy;
	}
	private JLabel getLblVersionToRemove() {
		if (lblVersionToRemove == null) {
			lblVersionToRemove = new JLabel("Version to remove from autoupdate:");
		}
		return lblVersionToRemove;
	}
	private TextWidget getToRemove() {
		if (toRemove == null) {
			toRemove = new TextWidget();
			toRemove.setColumns(10);
		}
		return toRemove;
	}
	
	private Preferences getPreferences() {
		return Preferences.userNodeForPackage(getClass());
	}
	private JLabel getLblDeploymentSourceDirectory() {
		if (lblDeploymentSourceDirectory == null) {
			lblDeploymentSourceDirectory = new JLabel("Deployment source directory:");
		}
		return lblDeploymentSourceDirectory;
	}
	private TextWidget getSrcFolder() {
		if (srcFolder == null) {
			srcFolder = new TextWidget();
			srcFolder.setColumns(10);
		}
		return srcFolder;
	}

	private JCheckBox getBetaCheckBox() {
		if (betaCheckBox == null) {
			betaCheckBox = new JCheckBox("Beta");
		}
		return betaCheckBox;
	}

	public void saveState() {
		Preferences prefs = getPreferences();
		prefs.put("user", getLogin().getUser());
		prefs.put("password", getLogin().getPassword());
		prefs.put("srcFolder", getSrcFolder().getText());
		prefs.put("version", getToDeploy().getText());
		prefs.put("toRemove", getToRemove().getText());
		prefs.putBoolean("beta", getBetaCheckBox().isSelected());
	}
	
	public void restoreState() {
		Preferences prefs = getPreferences();
		getLoginPanel().setLogin(prefs.get("user", ""), prefs.get("password", ""));
		getSrcFolder().setText(prefs.get("srcFolder", ""));
		getToDeploy().setText(prefs.get("version", ""));
		getToRemove().setText(prefs.get("toRemove", ""));
		getBetaCheckBox().setSelected(prefs.getBoolean("beta", false));
	}
}
