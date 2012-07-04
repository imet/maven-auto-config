/**
 * 
 */
package com.jd.maven.plugin.autoconfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author luolishu
 *
 */
public class AutoConfigEntityResolver implements EntityResolver {

	/* (non-Javadoc)
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		InputStream in =new FileInputStream("D:/workspace-maven/src/main/java/com/jd/maven/plugin/autoconfig/autoconfig.xsd"); /*this.getClass().getResourceAsStream(  
                "/com/jd/maven/plugin/autoconfig/autoconfig.xsd"); */ 
		return new InputSource(in);
	}

}
