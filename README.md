# maven-auto-config

Automatically exported from code.google.com/p/maven-auto-config （自动从 google code 上导出的，仅作备份使用 )

If you find this meet the legal issue, please contact me to delete this repository.  (如果你认为这个有法律问题，请联系我，我将删除这个 repository.)



when we coding in java platform. if we use maven to construt our codes. we have different environments,development,test,live etc. so any of environments have their own configuration, we can config it by maven profiles,but not readable so well, and maintainment is too hard.developer，test and deployer interact is also too hard, time is money,so we develop our own configuration plugins.

在使用java进行开发的时候，当我们在不同的环境部署应用程序的时候，会遇到这样的问题：
````
1.不同环境配置不一样。
2.部署的时候需要修改配置。
3.配置文件只能某一部分人进行修改（涉及到管理安全等原因）。
4.运维发布人员在发布代码的时候，假如涉及到线上环境的修改，多台机器修改的时候，
  会导致修改配置文件不一致的问题，容易引发故障。多次重复操作，成本过高，维护也不方便。
  经过长时间的操作，可能导致各服务器配置信息之间不一致，存在很大的安全隐患。
  另外，各个服务器的代码或者配置信息有可能不一致。
````

为此我们开发了该插件，插件提供以下功能：
````
1.将不同环境的配置进行隔离，不同人员可以维护不同的配置信息。
2.配置文件可以通过模板生成的方式（基于velocity）
3.提供一套配置管理的后台，将不同环境的配置，交给不同的配置人进行配置和变更，集中管理配置信息。
  发布人员不再需要操作不同服务器进行修改配置文件，简化发布的操作，分工更加有序和有效
4.自动感知每次发布上线的修改，检测配置信息的增删改操作，提供review功能，使得上线更加自动化和智能化。
````

插件使用方法： 在classpath目录下新建META-INF目录（推荐建在resources目录下）。 在META-INF目录下新建两个文件夹： 在META-INF下新建配置文件autoconfig.xml. 配置文件内容示例如下：

````xml
<?xml version="1.0" encoding="UTF-8"?>
<autoconfig>
        <remote-resources>
            <global-resouce url="http://localhost:8080/autoconfig?project=abc&amp;env=dev" file="project.properties" />
                <resource url="http://localhost:8080/autoconfig?project=abc&amp;env=dev" file="project.properties" />
        </remote-resources>

        <template-resources>
                <resource template="spring-config.xml.vm" target="spring-config.xml" />
                <resource template="spring/spring-config-struts.xml.vm" target="spring/spring-config-struts.xml" />
        </template-resources>
</autoconfig>
````
## environments和templates

### environments环境解析

environments代表不同的环境，譬如：dev,test,live等，大致目录结构如下：

````
--environments
----dev      
----test
----live
      --beijing
      --shanghai
````      

环境的配置可以支持继承的方式，目录越深的文件配置，会覆盖顶层的配置，在environments下面的属性文件为默认配置。

### templates下的文件是基于velocity模板语言进行生成的
autoconfig.xml配置信息

````
<template-resources>
    <resource template="spring-config.xml.vm" target="spring-config.xml" />
    <resource template="spring/spring-config-struts.xml.vm" target="spring/spring-config-struts.xml" />
</template-resources>
````

在插件运行的时候会对该模板进行处理，模板示例如下：

````
#if($!live)
   生成环境
#elseif($!dev)
   开发环境 
#elseif($!qa)
   测试环境
#end

#if($!env=='live')
   生产环境
#elseif($!env=='dev')
   开发环境 
#elseif($!env=='qa')
   测试环境 
#end
````

### 插件配置
````
     <plugin>
        <groupId>com.jd.maven.plugin</groupId>
        <artifactId>autoconfig-maven-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
            <execution>
            <phase>process-resources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
          </execution>
        </executions>
     </plugin>
````

配置后，当maven进行package或者install或者resource的时候，会自动执行该插件

另外，可以直接执行生成配置文件的命令mvn autoconfig:generate



