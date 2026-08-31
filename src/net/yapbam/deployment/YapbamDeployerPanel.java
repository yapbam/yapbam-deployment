package net.yapbam.deployment;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.UIManager;

import com.fathzer.soft.ajlib.swing.widget.TextWidget;

import net.astesana.widget.LoginPanel;

@SuppressWarnings("serial")
public class YapbamDeployerPanel extends JPanel {
	private JPanel panel;
	private JPanel panel1;
	private JButton startButton;
	private LoginPanel loginPanel;
	private LoginPanel webLoginPanel;
	private JPanel webAccessPanel;
	private JLabel lblWebRoot;
	private TextWidget webRoot;
	private JLabel lblVersionToDeploy;
	private TextWidget toDeploy;
	private JLabel lblVersionToRemove;
	private TextWidget toRemove;
	private JLabel lblDeploymentSourceDirectory;
	private TextWidget srcFolder;
	private JCheckBox betaCheckBox;
	private JLabel lblUpdaterJar;
	private TextWidget updaterJar;
	private JButton srcFolderBrowseButton;
	private JButton updaterJarBrowseButton;
	/**
	 * Create the panel.
	 */
	public YapbamDeployerPanel() {
		setLayout(new BorderLayout(0, 0));
		add(getMainPanel(), BorderLayout.CENTER);
		add(getBottomPanel(), BorderLayout.SOUTH);
		registerValidationListeners();
	}
	private JPanel getMainPanel() {
		if (panel == null) {
			panel = new JPanel();
			GridBagLayout gblPanel = new GridBagLayout();
			gblPanel.columnWeights = new double[]{0.0, 1.0, 0.0};
			panel.setLayout(gblPanel);
			// Web site access (web root URL + credentials) at the top
			GridBagConstraints gbcWebAccessPanel = new GridBagConstraints();
			gbcWebAccessPanel.gridwidth = 0;
			gbcWebAccessPanel.insets = new Insets(0, 0, 5, 0);
			gbcWebAccessPanel.anchor = GridBagConstraints.WEST;
			gbcWebAccessPanel.weightx = 1.0;
			gbcWebAccessPanel.gridx = 0;
			gbcWebAccessPanel.gridy = 0;
			panel.add(getWebAccessPanel(), gbcWebAccessPanel);
			// SourceForge download site access
			GridBagConstraints gbcLoginPanel = new GridBagConstraints();
			gbcLoginPanel.gridwidth = 0;
			gbcLoginPanel.insets = new Insets(0, 0, 5, 0);
			gbcLoginPanel.anchor = GridBagConstraints.WEST;
			gbcLoginPanel.weightx = 1.0;
			gbcLoginPanel.gridx = 0;
			gbcLoginPanel.gridy = 1;
			panel.add(getLoginPanel(), gbcLoginPanel);
			GridBagConstraints gbcLblDeploymentSourceDirectory = new GridBagConstraints();
			gbcLblDeploymentSourceDirectory.anchor = GridBagConstraints.WEST;
			gbcLblDeploymentSourceDirectory.insets = new Insets(0, 5, 5, 5);
			gbcLblDeploymentSourceDirectory.gridx = 0;
			gbcLblDeploymentSourceDirectory.gridy = 2;
			panel.add(getLblDeploymentSourceDirectory(), gbcLblDeploymentSourceDirectory);
			GridBagConstraints gbcSrcFolder = new GridBagConstraints();
			gbcSrcFolder.fill = GridBagConstraints.HORIZONTAL;
			gbcSrcFolder.anchor = GridBagConstraints.WEST;
			gbcSrcFolder.insets = new Insets(0, 0, 5, 0);
			gbcSrcFolder.gridx = 1;
			gbcSrcFolder.gridy = 2;
			panel.add(getSrcFolder(), gbcSrcFolder);
			GridBagConstraints gbcSrcFolderBrowse = new GridBagConstraints();
			gbcSrcFolderBrowse.anchor = GridBagConstraints.WEST;
			gbcSrcFolderBrowse.insets = new Insets(0, 0, 5, 5);
			gbcSrcFolderBrowse.gridx = 2;
			gbcSrcFolderBrowse.gridy = 2;
			panel.add(getSrcFolderBrowseButton(), gbcSrcFolderBrowse);
			GridBagConstraints gbcLblVersionToDeploy = new GridBagConstraints();
			gbcLblVersionToDeploy.anchor = GridBagConstraints.WEST;
			gbcLblVersionToDeploy.insets = new Insets(0, 5, 5, 5);
			gbcLblVersionToDeploy.gridx = 0;
			gbcLblVersionToDeploy.gridy = 3;
			panel.add(getLblVersionToDeploy(), gbcLblVersionToDeploy);
			GridBagConstraints gbcToDeploy = new GridBagConstraints();
			gbcToDeploy.insets = new Insets(0, 0, 5, 0);
			gbcToDeploy.anchor = GridBagConstraints.WEST;
			gbcToDeploy.gridx = 1;
			gbcToDeploy.gridy = 3;
			panel.add(getToDeploy(), gbcToDeploy);
			GridBagConstraints gbcLblVersionToRemove = new GridBagConstraints();
			gbcLblVersionToRemove.anchor = GridBagConstraints.EAST;
			gbcLblVersionToRemove.insets = new Insets(0, 5, 5, 5);
			gbcLblVersionToRemove.gridx = 0;
			gbcLblVersionToRemove.gridy = 4;
			panel.add(getLblVersionToRemove(), gbcLblVersionToRemove);
			GridBagConstraints gbcToRemove = new GridBagConstraints();
			gbcToRemove.insets = new Insets(0, 0, 5, 0);
			gbcToRemove.anchor = GridBagConstraints.WEST;
			gbcToRemove.gridx = 1;
			gbcToRemove.gridy = 4;
			panel.add(getToRemove(), gbcToRemove);
			GridBagConstraints gbcBetaCheckBox = new GridBagConstraints();
			gbcBetaCheckBox.anchor = GridBagConstraints.WEST;
			gbcBetaCheckBox.insets = new Insets(0, 5, 0, 5);
			gbcBetaCheckBox.gridx = 0;
			gbcBetaCheckBox.gridy = 5;
			panel.add(getBetaCheckBox(), gbcBetaCheckBox);
			GridBagConstraints gbcLblUpdaterJar = new GridBagConstraints();
			gbcLblUpdaterJar.anchor = GridBagConstraints.WEST;
			gbcLblUpdaterJar.insets = new Insets(0, 5, 0, 5);
			gbcLblUpdaterJar.gridx = 0;
			gbcLblUpdaterJar.gridy = 6;
			panel.add(getLblUpdaterJar(), gbcLblUpdaterJar);
			GridBagConstraints gbcUpdaterJar = new GridBagConstraints();
			gbcUpdaterJar.fill = GridBagConstraints.HORIZONTAL;
			gbcUpdaterJar.anchor = GridBagConstraints.WEST;
			gbcUpdaterJar.gridx = 1;
			gbcUpdaterJar.gridy = 6;
			panel.add(getUpdaterJar(), gbcUpdaterJar);
			GridBagConstraints gbcUpdaterJarBrowse = new GridBagConstraints();
			gbcUpdaterJarBrowse.anchor = GridBagConstraints.WEST;
			gbcUpdaterJarBrowse.insets = new Insets(0, 0, 0, 5);
			gbcUpdaterJarBrowse.gridx = 2;
			gbcUpdaterJarBrowse.gridy = 6;
			panel.add(getUpdaterJarBrowseButton(), gbcUpdaterJarBrowse);
		}
		return panel;
	}
	private JPanel getBottomPanel() {
		if (panel1 == null) {
			panel1 = new JPanel(new BorderLayout(0, 0));
			panel1.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
			panel1.add(getStartButton(), BorderLayout.EAST);
		}
		return panel1;
	}
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton("Start ...");
			startButton.addActionListener(e -> {
				try {
					String srcPath = getSrcFolder().getText().trim();
					String version = getToDeploy().getText();
					File forcedUpdater = null;
					if (getBetaCheckBox().isSelected()) {
						String updaterPath = getUpdaterJar().getText().trim();
						forcedUpdater = updaterPath.isEmpty() ? null : new File(updaterPath);
					}
					DeploymentPreparation prep = new DeploymentPreparation(new File(srcPath), version, forcedUpdater);
					File preparedFolder = prep.prepare();
					try (DeployYapbam yapbamDeployer = new DeployYapbam(getLoginPanel().getLogin().getUser(), getLoginPanel().getLogin().getPassword(),
							getWebRoot().getText().trim(), getWebLoginPanel().getLogin().getUser(), getWebLoginPanel().getLogin().getPassword(),
							preparedFolder.getAbsolutePath(), version, getToRemove().getText(), getBetaCheckBox().isSelected())) {
						yapbamDeployer.doIt();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(startButton, "An error occurred", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		return startButton;
	}
	private LoginPanel getLoginPanel() {
		if (loginPanel == null) {
			loginPanel = new LoginPanel();
			loginPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Download site access (Sourceforge)", TitledBorder.LEFT, TitledBorder.TOP));
			loginPanel.addPropertyChangeListener(LoginPanel.LOGIN_PROPERTY, evt -> System.out.println (evt.getOldValue()+" -> "+evt.getNewValue()));
		}
		return loginPanel;
	}
	
	public LoginPanel.Login getLogin() {
		return getLoginPanel().getLogin();
	}
	private LoginPanel getWebLoginPanel() {
		if (webLoginPanel == null) {
			webLoginPanel = new LoginPanel();
		}
		return webLoginPanel;
	}

	private JPanel getWebAccessPanel() {
		if (webAccessPanel == null) {
			webAccessPanel = new JPanel(new GridBagLayout());
			webAccessPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Web site access", TitledBorder.LEFT, TitledBorder.TOP));
			GridBagConstraints gbcLblWebRoot = new GridBagConstraints();
			gbcLblWebRoot.anchor = GridBagConstraints.WEST;
			gbcLblWebRoot.insets = new Insets(0, 5, 5, 5);
			gbcLblWebRoot.gridx = 0;
			gbcLblWebRoot.gridy = 0;
			webAccessPanel.add(getLblWebRoot(), gbcLblWebRoot);
			GridBagConstraints gbcWebRoot = new GridBagConstraints();
			gbcWebRoot.fill = GridBagConstraints.HORIZONTAL;
			gbcWebRoot.anchor = GridBagConstraints.WEST;
			gbcWebRoot.insets = new Insets(0, 0, 5, 0);
			gbcWebRoot.weightx = 1.0;
			gbcWebRoot.gridx = 1;
			gbcWebRoot.gridy = 0;
			webAccessPanel.add(getWebRoot(), gbcWebRoot);
			GridBagConstraints gbcWebLogin = new GridBagConstraints();
			gbcWebLogin.gridwidth = 0;
			gbcWebLogin.anchor = GridBagConstraints.WEST;
			gbcWebLogin.weightx = 1.0;
			gbcWebLogin.gridx = 0;
			gbcWebLogin.gridy = 1;
			webAccessPanel.add(getWebLoginPanel(), gbcWebLogin);
		}
		return webAccessPanel;
	}

	private JLabel getLblWebRoot() {
		if (lblWebRoot == null) {
			lblWebRoot = new JLabel("Web root URL:");
		}
		return lblWebRoot;
	}

	private TextWidget getWebRoot() {
		if (webRoot == null) {
			webRoot = new TextWidget();
			webRoot.setColumns(10);
		}
		return webRoot;
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
			betaCheckBox.addActionListener(e -> updateUpdaterJarEnabledState());
		}
		return betaCheckBox;
	}

	private void updateUpdaterJarEnabledState() {
		boolean enabled = getBetaCheckBox().isSelected();
		getLblUpdaterJar().setEnabled(enabled);
		getUpdaterJar().setEnabled(enabled);
		getUpdaterJarBrowseButton().setEnabled(enabled);
	}

	private JLabel getLblUpdaterJar() {
		if (lblUpdaterJar == null) {
			lblUpdaterJar = new JLabel("Updater jar (optional):");
		}
		return lblUpdaterJar;
	}

	private TextWidget getUpdaterJar() {
		if (updaterJar == null) {
			updaterJar = new TextWidget();
			updaterJar.setColumns(10);
		}
		return updaterJar;
	}

	private JButton getSrcFolderBrowseButton() {
		if (srcFolderBrowseButton == null) {
			srcFolderBrowseButton = new JButton("Browse...");
			srcFolderBrowseButton.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser(getSrcFolder().getText().trim());
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(YapbamDeployerPanel.this) == JFileChooser.APPROVE_OPTION) {
					getSrcFolder().setText(chooser.getSelectedFile().getAbsolutePath());
				}
			});
		}
		return srcFolderBrowseButton;
	}

	private JButton getUpdaterJarBrowseButton() {
		if (updaterJarBrowseButton == null) {
			updaterJarBrowseButton = new JButton("Browse...");
			updaterJarBrowseButton.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser(getUpdaterJar().getText().trim());
				chooser.setFileFilter(new FileNameExtensionFilter("Jar files", "jar"));
				if (chooser.showOpenDialog(YapbamDeployerPanel.this) == JFileChooser.APPROVE_OPTION) {
					getUpdaterJar().setText(chooser.getSelectedFile().getAbsolutePath());
				}
			});
		}
		return updaterJarBrowseButton;
	}

	private void registerValidationListeners() {
		PropertyChangeListener textListener = evt -> validateStartButton();
		getSrcFolder().addPropertyChangeListener(TextWidget.TEXT_PROPERTY, textListener);
		getToDeploy().addPropertyChangeListener(TextWidget.TEXT_PROPERTY, textListener);
		getToRemove().addPropertyChangeListener(TextWidget.TEXT_PROPERTY, textListener);
		getWebRoot().addPropertyChangeListener(TextWidget.TEXT_PROPERTY, textListener);
		getUpdaterJar().addPropertyChangeListener(TextWidget.TEXT_PROPERTY, textListener);
		getLoginPanel().addPropertyChangeListener(LoginPanel.LOGIN_PROPERTY, textListener);
		getWebLoginPanel().addPropertyChangeListener(LoginPanel.LOGIN_PROPERTY, textListener);
		getBetaCheckBox().addActionListener(e -> validateStartButton());
	}

	private void validateStartButton() {
		List<String> problems = new ArrayList<>();
		// Check login credentials
		if (isEmpty(getLoginPanel().getLogin().getUser()) || isEmpty(getLoginPanel().getLogin().getPassword())) {
			problems.add("SourceForge login is not set");
		}
		if (isEmpty(getWebLoginPanel().getLogin().getUser()) || isEmpty(getWebLoginPanel().getLogin().getPassword())) {
			problems.add("Web site login is not set");
		}
		// Check web root URL
		if (getWebRoot().getText().trim().isEmpty()) {
			problems.add("Web root URL is not set");
		}
		// Check source folder and version
		String srcPath = getSrcFolder().getText().trim();
		String version = getToDeploy().getText().trim();
		if (srcPath.isEmpty()) {
			problems.add("Deployment source directory is not set");
		} else if (version.isEmpty()) {
			problems.add("Version to deploy is not set");
		} else {
			// Check that all required files exist
			SrcDescription srcDesc = new SrcDescription(new File(srcPath), version, null, null);
			for (File f : srcDesc.getFiles()) {
				if (!f.exists()) {
					problems.add("Missing file: " + f.getAbsolutePath());
				}
			}
			// Check updater jar only when a forced updater is provided (beta mode)
			if (getBetaCheckBox().isSelected()) {
				String updaterPath = getUpdaterJar().getText().trim();
				if (!updaterPath.isEmpty()) {
					File updater = new File(updaterPath);
					if (!updater.exists()) {
						problems.add("Missing updater jar: " + updater.getAbsolutePath());
					}
				}
			}
		}
		if (problems.isEmpty()) {
			getStartButton().setEnabled(true);
			getStartButton().setToolTipText(null);
		} else {
			getStartButton().setEnabled(false);
			StringBuilder tooltip = new StringBuilder("<html>");
			for (int i = 0; i < problems.size(); i++) {
				if (i > 0) {
					tooltip.append("<br>");
				}
				tooltip.append(problems.get(i));
			}
			tooltip.append("</html>");
			getStartButton().setToolTipText(tooltip.toString());
		}
	}

	private static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	public void saveState() {
		Preferences prefs = getPreferences();
		prefs.put("user", getLogin().getUser());
		prefs.put("password", getLogin().getPassword());
		prefs.put("srcFolder", getSrcFolder().getText());
		prefs.put("version", getToDeploy().getText());
		prefs.put("toRemove", getToRemove().getText());
		prefs.put("updaterJar", getUpdaterJar().getText());
		prefs.putBoolean("beta", getBetaCheckBox().isSelected());
		prefs.put("webRoot", getWebRoot().getText());
		prefs.put("webUser", getWebLoginPanel().getLogin().getUser());
		prefs.put("webPassword", getWebLoginPanel().getLogin().getPassword());
	}
	
	public void restoreState() {
		Preferences prefs = getPreferences();
		getLoginPanel().setLogin(prefs.get("user", ""), prefs.get("password", ""));
		getSrcFolder().setText(prefs.get("srcFolder", ""));
		getToDeploy().setText(prefs.get("version", ""));
		getToRemove().setText(prefs.get("toRemove", ""));
		getUpdaterJar().setText(prefs.get("updaterJar", ""));
		getBetaCheckBox().setSelected(prefs.getBoolean("beta", false));
		updateUpdaterJarEnabledState();
		getWebRoot().setText(prefs.get("webRoot", ""));
		getWebLoginPanel().setLogin(prefs.get("webUser", ""), prefs.get("webPassword", ""));
		validateStartButton();
	}
}
