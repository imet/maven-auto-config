/**
 * 
 */
package com.jd.maven.plugin.autoconfig;

import java.io.File; 

import org.apache.maven.project.MavenProject;

/**
 * @author luolishu
 *
 */
public class AutoConfigContext{
	File configFile; 
	protected MavenProject project;
	protected String env;
	protected AutoConfigMojo autoConfigMojo;
	 
	public MavenProject getProject() {
		return project;
	}
	public void setProject(MavenProject project) {
		this.project = project;
	}
	public File getConfigFile() {
		return configFile;
	}
	public void setConfigFile(File configFile) {
		this.configFile = configFile;
	}
	public String getEnv() {
		return env;
	}
	public void setEnv(String env) {
		this.env = env;
	}
	public AutoConfigMojo getAutoConfigMojo() {
		return autoConfigMojo;
	}
	public void setAutoConfigMojo(AutoConfigMojo autoConfigMojo) {
		this.autoConfigMojo = autoConfigMojo;
	}
}
