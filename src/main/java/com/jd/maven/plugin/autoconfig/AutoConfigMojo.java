package com.jd.maven.plugin.autoconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer; 
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

/** 
 * 自动生成模板和从远程加载配置信息
 * @author luolishu
 * @goal generate
 * @phase process-resources
 * @threadSafe
 * 
 */
public class AutoConfigMojo extends AbstractMojo implements Contextualizable {
 

	/**
	 * The output directory into which to copy the resources.
	 * 
	 * @parameter default-value="${project.build.outputDirectory}"
	 * @required
	 */
	private File outputDirectory;
	/**
	 * The list of resources we want to transfer.
	 * 
	 * @parameter default-value="${project.resources}"
	 * @required
	 * @readonly
	 */
	private List<Resource> resources;
	/**
	 * The list of resources we want to transfer.
	 * 
	 * @parameter 
	 * @readonly
	 */
	private String remoteUrl;

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * 
	 * @component 
	 *            role="org.apache.maven.shared.filtering.MavenResourcesFiltering"
	 *            role-hint="default"
	 * @required
	 */
	protected MavenResourcesFiltering mavenResourcesFiltering;

	/**
	 * @parameter default-value="${session}"
	 * @readonly
	 * @required
	 */
	protected MavenSession session;

	/**
	 * @since 2.4
	 */
	private PlexusContainer plexusContainer;
	protected String CONFIG_DIRECTORY = "META-INF";
	protected String CONFIG_FILE = "autoconfig.xml";

	private static final String ENV_KEY = "env";
	private static final String DEFAULT_ENV = "dev";
	private static final String ENV_DIR="environments";
	private static final VelocityEngine velocityEngine = new VelocityEngine();

	public void contextualize(Context context) throws ContextException {
		plexusContainer = (PlexusContainer) context
				.get(PlexusConstants.PLEXUS_KEY);
	}

	public void execute() throws MojoExecutionException { 
		File autoConfigDirectory = this.getAutoConfigDirectory();
		if (autoConfigDirectory == null) {
			this.getLog().warn("groupId="+this.project.getGroupId()+" artifactId="+this.project.getArtifactId()+","+CONFIG_DIRECTORY + " directory not found!");
			return;
		}
		try {
			velocityEngine.setProperty(
					RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
					autoConfigDirectory.getAbsolutePath() + File.separator
							+ "templates");
			velocityEngine.init();
			if (this.getLog().isDebugEnabled()) {
				this.getLog().debug(
						"autoConfigDirectory="
								+ autoConfigDirectory.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			List<AutoConfigResource> templateResourceList = this
					.getTemplateResources();
			if (templateResourceList == null || templateResourceList.isEmpty()) {
				if (this.getLog().isDebugEnabled()) {
					this.getLog().debug(
							"AutoConfigResource is empty!autoconfig="
									+ getAutoConfigFile().getAbsolutePath());
				}
			} else {
				for (AutoConfigResource resource : templateResourceList) {
					if (this.getLog().isDebugEnabled()) {
						this.getLog().debug(
								"Generate template resource template="
										+ resource.getTemplate() + " target="
										+ resource.getTarget());
					}
					this.generateTemplateResource(resource);
				}
			}
			File environmentDir = new File(autoConfigDirectory, ENV_DIR+File.separator+getEnv());
			if (this.getLog().isDebugEnabled()) {
				this.getLog().debug(
						"Generate enviroment=" +environmentDir.getAbsolutePath());
			}
			generateEnvironmentProperties(environmentDir);
			generateGlobalProperties();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private File getAutoConfigFile() {
		return new File(getAutoConfigDirectory().getAbsoluteFile()
				+ File.separator + CONFIG_FILE);
	}

	private File getAutoConfigDirectory() {
		File configFile = null;
		for (Resource resource : this.resources) {
			File file = new File(resource.getDirectory() + File.separator
					+ CONFIG_DIRECTORY);

			if (!file.exists()) {
				continue;
			}
			file = new File(resource.getDirectory() + File.separator
					+ CONFIG_DIRECTORY + File.separator + CONFIG_FILE);
			if (!file.exists()) {
				continue;
			}
			configFile = file.getParentFile();
			break;
		}
		return configFile;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	private List<AutoConfigResource> getTemplateResources() {
		return ResourceUtils.getTemplateResources(getAutoConfigContext());
	}
	private void generateGlobalProperties(){
		String globalFileName=ResourceUtils.getGlobalFileName(getAutoConfigContext());
		if(StringUtils.isBlank(globalFileName)){
			return;
		}
		File file = new File(this.outputDirectory, ResourceUtils.getGlobalFileName(getAutoConfigContext()));
		Properties localProps=new Properties();
		if(file.exists()){
			localProps.putAll(loadProperties(file));
		} 
		FileOutputStream fos=null;
		try{
			fos=new FileOutputStream(file);
			this.replace(localProps);
			localProps.store(fos, "Generate by autoconfig,document reference=");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			this.closeQuietly(fos);
		}
		
	}
	private AutoConfigContext getAutoConfigContext(){
		AutoConfigContext context=new AutoConfigContext();		 
		context.setConfigFile(getAutoConfigFile());
		context.setProject(this.project); 
		context.setEnv(this.getEnv());
		context.setAutoConfigMojo(this);
		return context;
	}
	void generateEnvironmentProperties(File environmentDir) {
		Map<String, List<File>> propertiesHolder = new LinkedHashMap<String, List<File>>();
		Map<String, Properties> remotePropertiesHolder = ResourceUtils
				.getRemoteProperties( getAutoConfigContext());
		this.generateEnvironmentResources(environmentDir, propertiesHolder);
		Set<Map.Entry<String, List<File>>> entrys = propertiesHolder.entrySet();
	 
		for (Map.Entry<String, List<File>> entry : entrys) {
			if (this.getLog().isDebugEnabled()) {
				this.getLog().debug(
						"Generate environment ["+this.getEnv()+"] autoconfig properties file="
								+ entry.getKey());
			}
			File file = new File(this.outputDirectory, entry.getKey());
			FileOutputStream fos = null;
			try {
				if(!file.exists()){
					file.createNewFile();
				}
				fos = new FileOutputStream(file);
				List<File> targetFiles = entry.getValue();
				if (targetFiles != null) {
					Properties remote = remotePropertiesHolder.get(entry
							.getKey());
					Properties target=new Properties();
					for(int i=targetFiles.size()-1;i>=0;i--){
						target.putAll(this.loadProperties(targetFiles.get(i)));
					}
					if (remote != null) {
						target.putAll(remote);
					}
					this.replace(target);
					target.store(fos, "Generated by maven,AutoConfig plugin");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				closeQuietly(fos);
			}

		}

	}

	private String getEnv() {
		String env = System.getProperty(ENV_KEY); 
		if (StringUtils.isBlank(env)) {
			return DEFAULT_ENV;
		}
		return env;
	}

	void generateEnvironmentResources(File file,
			Map<String, List<File>> propertiesHolder) {
		if (file != null && file.isFile()) {
			this.generateProperties(file, propertiesHolder);
			return;
		}

		File files[] = file.listFiles();
		if (files == null||files.length<=0) {
			return;
		}
		for (File item : files) {
			if (item == null) {
				continue;
			}
			if (item.isDirectory()) {
				this.generateEnvironmentResources(item, propertiesHolder);
			} else {
				this.generateProperties(item, propertiesHolder);
			}
		}

	}

	void generateProperties(File file, Map<String, List<File>> propertiesHolder) {
		String key = file.getName();
		List<File> props = propertiesHolder.get(key);
		if (props == null) {
			props =  new LinkedList<File>();
			propertiesHolder.put(key, props); 
		} 
		props.add(file); 
	}
	private Properties loadProperties(File file){
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			props.load(fis);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			closeQuietly(fis);
		}
		return props;
	}

	public static void closeQuietly(InputStream input) {
		try {
			if (input != null) {
				input.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	public static void closeQuietly(OutputStream output) {
		try {
			if (output != null) {
				output.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	public static void closeQuietly(Writer output) {
		try {
			if (output != null) {
				output.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	void generateTemplateResource(AutoConfigResource configResource) {
		Template template = null;
		Writer writer = null;
		try {
			template = velocityEngine.getTemplate(configResource.getTemplate());
			writer = this.getOutputWriter(configResource);
			template.merge(this.getVelocityContext(), writer);
		} catch (ResourceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			closeQuietly(writer);
		}
	}

	Writer getOutputWriter(AutoConfigResource configResource) {
		File file = new File(outputDirectory, configResource.getTarget());
		FileWriter writer = null;
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			writer = new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writer;
	}
 

	private void replace(Properties properties) {
		Properties global = ResourceUtils
				.getGlobalRemoteProperties(getAutoConfigContext());
		if (global != null) {
			properties.putAll(global);
		}
		Properties replaced=project.getProperties();
		Set<Entry<Object, Object>> entrySet=replaced.entrySet();
		for(Entry entry:entrySet){
			if(properties.containsKey(entry.getKey())){
				properties.put(entry.getKey(), entry.getValue());
			}
		}
		
		replaced=System.getProperties();
		entrySet=replaced.entrySet();
		for(Entry entry:entrySet){
			if(properties.containsKey(entry.getKey())){
				properties.put(entry.getKey(), entry.getValue());
			}
		}
		/*properties.putAll(project.getProperties());
		properties.putAll(System.getProperties());*/
	}

	private VelocityContext getVelocityContext() {
		VelocityContext context = new VelocityContext();
		Properties properties = new Properties();

		this.replace(properties);
		if (properties != null) {
			Set<Entry<Object, Object>> entrySet = properties.entrySet();
			for (Entry entry : entrySet) {
				context.put(entry.getKey() + "", entry.getValue());
			}
			String env = properties.getProperty(ENV_KEY);
			if (StringUtils.isBlank(env)) {
				env = DEFAULT_ENV;
			}
			context.put(env, true);
			context.put("env", env);
		}
		return context;
	}

	public static void main(String args[]) {
		AutoConfigMojo mojo = new AutoConfigMojo();
		File file = new File(
				"D:/workspace-maven/src/main/resources/META-INF/environments/dev");
		File outputDirectory = new File("D:/tmp");
		mojo.setOutputDirectory(outputDirectory);
		List<Resource> resources=new LinkedList();
		Resource res=new Resource();
		res.setDirectory("D:/workspace-maven/src/main/resources/");
		resources.add(res);
		mojo.setResources(resources);
		mojo.project=new MavenProject();

		mojo.generateEnvironmentProperties(file);

	}

	public List getResources() {
		return resources;
	}

	public void setResources(List resources) {
		this.resources = resources;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

}
