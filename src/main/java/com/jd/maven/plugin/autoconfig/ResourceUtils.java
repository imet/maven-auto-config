
package com.jd.maven.plugin.autoconfig;
 
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document; 
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

/**
 * @author luolishu
 *
 */
public abstract class ResourceUtils {
	static final String TEMPLATE_RESOURCES="/autoconfig/template-resources/resource";
	static final String REMOTE_RESOURCES="/autoconfig/remote-resources/resource";
	static final String GLOBAL_REMOTE_RESOURCES="/autoconfig/remote-resources/global-resouce";
	public static List<AutoConfigResource> getTemplateResources(AutoConfigContext context){
		List<AutoConfigResource> templateList=new ArrayList<AutoConfigResource>();
	 
	    try {
	    	SAXReader xmlReader = getSAXReader();
			Document doc = xmlReader.read(context.getConfigFile());
			List<Element> nodeList=doc.selectNodes(TEMPLATE_RESOURCES);
			if(nodeList==null){
				return templateList;
			}
			for(Element e:nodeList){
				AutoConfigResource resource=new AutoConfigResource();
				resource.setTemplate(e.attributeValue("template").trim());
				resource.setTarget(e.attributeValue("target").trim());
				templateList.add(resource);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return templateList;
	}
	
	public static Properties getGlobalRemoteProperties(AutoConfigContext context){ 
	    try {
	    	SAXReader xmlReader = getSAXReader();
			Document doc = xmlReader.read(context.getConfigFile());
			List<Element> nodeList=doc.selectNodes(GLOBAL_REMOTE_RESOURCES); 
			for(Element e:nodeList){ 
				String key=e.attributeValue("file").trim();
				String url=e.attributeValue("url").trim(); 
				return loadProperties(url,context);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
   static SAXReader getSAXReader() throws SAXException{
		SAXReader reader = new SAXReader(true);
		reader.setEntityResolver(new AutoConfigEntityResolver());  
        reader.setFeature("http://xml.org/sax/features/validation", true);  
        reader.setFeature("http://apache.org/xml/features/validation/schema", true);  
        reader.setFeature("http://apache.org/xml/features/validation/schema-full-checking",true); 
        reader.setProperty(
        		   "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
        		   "autoconfig.xsd"
        		  );
        return reader;
	}
	public static  String  getGlobalFileName(AutoConfigContext context){
		
	    try {
	    	SAXReader xmlReader = getSAXReader();
			Document doc = xmlReader.read(context.getConfigFile());
			List<Element> nodeList=doc.selectNodes(GLOBAL_REMOTE_RESOURCES); 
			for(Element e:nodeList){ 
				String key=e.attributeValue("file").trim(); 
				return key;
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
	public static  Map<String, Properties>  getRemoteProperties(AutoConfigContext context){
		Map<String, Properties> propertiesHolder=new HashMap<String, Properties>();  
	    try {
	    	SAXReader xmlReader = getSAXReader();
			Document doc = xmlReader.read(context.getConfigFile());
			List<Element> nodeList=doc.selectNodes(REMOTE_RESOURCES);
			if(nodeList==null){
				return propertiesHolder;
			}
			for(Element e:nodeList){ 
				String key=e.attributeValue("file").trim();
				String url=e.attributeValue("url").trim(); 
				propertiesHolder.put(key,loadProperties(url,context));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return propertiesHolder;
	}

	
	private static Properties loadProperties(String url,AutoConfigContext context) throws Exception{
		Properties props=new Properties();
		String realUrl=getRealUrl(url,context);
		if (context.getAutoConfigMojo().getLog().isDebugEnabled()) {
			context.getAutoConfigMojo().getLog().debug(
					"request autoconfig url="+realUrl);
		}
		props.load(new URL(realUrl).openStream());
		return props;
	}
	private static String getRealUrl(String url,AutoConfigContext context){
		String domain=url.split("[?]")[0];
		StringBuilder sb=new StringBuilder(domain).append("?");
		Map parameters=getParamenters(url);
		parameters.put("artifactId", context.getProject().getArtifactId());
		parameters.put("groupId", context.getProject().getGroupId());
		parameters.put("env", context.getEnv());
		Set<Map.Entry> set=parameters.entrySet();
		for(Map.Entry entry:set){
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		if(set.size()>0){
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}
    private static Map getParamenters(String url){
    	Map parameters=new HashMap();
    	if(url.indexOf('?')==-1){
    		return parameters;
    	}
    	String queryStr=url.split("[?]")[1];
    	String pairs[]=queryStr.split("[&]");
    	for(String item:pairs){
    		String args[]=item.split("=");
    		parameters.put(args[0], args[1]);
    	}
    	return parameters;
    }

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception { 
		 SAXReader xmlReader=getSAXReader();
		 
		 Document doc = xmlReader.read(new File("D:/workspace-maven/src/main/resources/META-INF/autoconfig.xml"));
		 List<Element> nodeList=doc.selectNodes(REMOTE_RESOURCES);
	}

}
