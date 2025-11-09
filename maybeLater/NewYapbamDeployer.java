package net.yapbam.deployment;

import java.util.Arrays;

import net.yapbam.deployment.task.JavadocTask;
import net.yapbam.deployment.task.ReleaseTask;
import net.yapbam.deployment.task.TutorialTask;

import com.fathzer.soft.jdeployer.DeployerGUI;
import com.fathzer.soft.jdeployer.Process;
import com.fathzer.soft.jdeployer.Task;

public class NewYapbamDeployer {

	public NewYapbamDeployer() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main (String[] args) {
		Process p = new Process("Deploy Yapbam", "yapbam", Arrays.asList(new Task[] {
				new ReleaseTask(), new JavadocTask(), new TutorialTask()}));
		new DeployerGUI(p).launch();

	}

}
