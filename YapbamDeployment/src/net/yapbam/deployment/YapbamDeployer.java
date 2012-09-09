package net.yapbam.deployment;
import java.awt.Container;

import net.astesana.ajlib.swing.framework.Application;

public class YapbamDeployer extends Application {
	private YapbamDeployerPanel panel;

	@Override
	protected Container buildMainPanel() {
		panel = new YapbamDeployerPanel();
		return panel;
	}

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.framework.Application#saveState()
	 */
	@Override
	protected void saveState() {
		panel.saveState();
		super.saveState();
	}


	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.framework.Application#restoreState()
	 */
	@Override
	protected void restoreState() {
		super.restoreState();
		panel.restoreState();
	}


	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.framework.Application#onStart()
	 */
	@Override
	protected boolean onStart() {
		getJFrame().pack();
		return super.onStart();
	}

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.framework.Application#getName()
	 */
	@Override
	public String getName() {
		return "Yapbam deployement";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new YapbamDeployer().launch();
	}
}
