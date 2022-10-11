# Evaluation

To run the evaluation hooks on the commits listed in `projects/commits` make adaptations marked by TODO in `dirts/dirts-di-walker.py` and execute:
```bash
pip install .
coop coop db postgresql://localhost:5432/dirts migrate # adapt this to your db connection

cd scripts
./clone_projects.sh
./evaluation.sh
```



# Results

Unsafety in STARTS in 7 of 228 commits:
- 5 from infovore
- 1 from spring-cloud-aws
- 1 from weld-testing


# Code snippets

## paulhoule/infovore

## Commit [40691b7d7a7e828c2f884e00d6f5495028994b3b](https://github.com/paulhoule/infovore/commit/40691b7d7a7e828c2f884e00d6f5495028994b3b):
- Test: com.ontology2.bakemono.MainTest
- Affected by: com.ontology2.bakemono.sumRDF.SumRDFTool (added)

Class com.ontology2.bakemono.sumRDF.SumRDFTool (added):
```java
@Component("sumRDF")
public class SumRDFTool extends SelfAwareTool<ReducerOptions> {
}
```

File src/main/resources/com/ontology2/bakemono/applicationContext.xml:
```xml
  <!--  1.0 Spring self-configuration -->
  <context:component-scan base-package="com.ontology2.bakemono"/>
```

Class com.ontology2.bakemono.Main:
```java
public class Main extends MainBase {
[...]
    public List<String> getApplicationContextPath() {
        return Lists.newArrayList("com/ontology2/bakemono/applicationContext.xml");
    }
```

Class com.ontology2.bakemono.MainBase:
```java
public MainBase(String[] arg0) {
        args= Lists.newArrayList(arg0);
        context=new ClassPathXmlApplicationContext(getApplicationContextPath().toArray(new String[] {})); # bean is already instantiated here
    }
```

## Commit [c2d27d85c72249d0d2bce6b1b2cfa92974ecde97](https://github.com/paulhoule/infovore/commit/c2d27d85c72249d0d2bce6b1b2cfa92974ecde97):
- Test: com.ontology2.haruhi.flows.TestFlowBeans
- Affected by: haruhi/src/main/resources/com/ontology2/haruhi/shell/applicationContext.xml (changed)

Class com.ontology2.haruhi.flows.TestFlowBeans:
```java
@ContextConfiguration({"../shell/applicationContext.xml","../shell/testDefaults.xml"})
[...]
@Autowired SpringFlow basekbNowFlow;
```

File haruhi/src/main/resources/com/ontology2/haruhi/shell/applicationContext.xml:
```xml
  <bean name="basekbNowFlow" class="com.ontology2.haruhi.flows.SpringFlow">
    <constructor-arg>
        <list>
            <bean class="com.ontology2.haruhi.flows.JobStep">
                <constructor-arg>
                  <list>
                     <value>'run'</value> # this line is added
                     <value>'freebaseRDFPrefilter'</value>
                     <value>pos[0]+'freebase-rdf-'+pos[1]+'/'</value>
                     <value>tmpDir+'preprocessed/'+pos[1]+'/'</value>
                  </list>
                </constructor-arg>
            </bean>
        </list>
    </constructor-arg>
  </bean>
```


### Commit [56ff4244917204042ecc3b94134ea8d293b7b34d](https://github.com/paulhoule/infovore/commit/56ff4244917204042ecc3b94134ea8d293b7b34d):
- Test: com.ontology2.bakemono.MainTest
- Affected by: com.ontology2.bakemono.sumRDF.SumRDFTool (added)

Class com.ontology2.bakemono.smushObject.SmushObjectTool (added):
```java
@Component("smushObject")
public class SmushObjectTool extends SelfAwareTool<RewriteSubjectOptions> {
}
```

File src/main/resources/com/ontology2/bakemono/applicationContext.xml:
```xml
  <!--  1.0 Spring self-configuration -->
  <context:component-scan base-package="com.ontology2.bakemono"/>
```

Class com.ontology2.bakemono.Main:
```java
public class Main extends MainBase {
[...]
    public List<String> getApplicationContextPath() {
        return Lists.newArrayList("com/ontology2/bakemono/applicationContext.xml");
    }
```

Class com.ontology2.bakemono.MainBase:
```java
public MainBase(String[] arg0) {
        args= Lists.newArrayList(arg0);
        context=new ClassPathXmlApplicationContext(getApplicationContextPath().toArray(new String[] {})); # bean is already instantiated here
    }
```


### Commit [1cfd98a7873eda453178316e02e1f1ab6bda3767](https://github.com/paulhoule/infovore/commit/1cfd98a7873eda453178316e02e1f1ab6bda3767):
Tests: com.ontology2.haruhi.TestEMRCluster, com.ontology2.haruhi.TestApplicationConfigurationLoader
Affected by: com/ontology2/haruhi/shell/applicationContext.xml, com/ontology2/haruhi/shell/awsClustersContext.xml

Class com.ontology2.haruhi.TestEMRCluster:
```java
@ContextConfiguration({"shell/applicationContext.xml","shell/testDefaults.xml"})
public class TestEMRCluster {
    @Autowired AmazonEMRCluster tinyAwsCluster;
    [...]
}
```

Class com.ontology2.haruhi.TestApplicationConfigurationLoader:
```java
@ContextConfiguration({"shell/applicationContext.xml","shell/testDefaults.xml"})
public class TestApplicationConfigurationLoader {
    @Autowired AmazonEMRCluster tinyAwsCluster;
    [...]
}
```

File com/ontology2/haruhi/shell/applicationContext.xml (changed):
```xml
  <import resource="awsClustersContext.xml"/>
```

File com/ontology2/haruhi/shell/awsClustersContext.xml (added):
```xml
<!-- bean was moved to different file -->
<bean name="tinyAwsCluster" class="com.ontology2.haruhi.AmazonEMRCluster">
  <constructor-arg>
    <bean parent="rootInstancesDefinition">
      <property name="instanceCount" value="1" />
    </bean>
  </constructor-arg>
</bean>
```


### Commit [f24eb4fab303282f804dd889945b69094073686f](https://github.com/paulhoule/infovore/commit/f24eb4fab303282f804dd889945b69094073686f):
Test: com.ontology2.bakemono.MainTest
Affected by: com.ontology2.bakemono.baseKBToDBpedia. BaseKBToDBpediaTool (added)

Class com.ontology2.bakemono.baseKBToDBpedia. BaseKBToDBpediaTool (added):
```java
@Component("baseKBToDBpedia")
public class BaseKBToDBpediaTool extends SelfAwareTool<CommonOptions> {
}
```

File src/main/resources/com/ontology2/bakemono/applicationContext.xml:
```xml
  <!--  1.0 Spring self-configuration -->
  <context:component-scan base-package="com.ontology2.bakemono"/>
```

Class com.ontology2.bakemono.Main:
```java
public class Main extends MainBase {
[...]
    public List<String> getApplicationContextPath() {
        return Lists.newArrayList("com/ontology2/bakemono/applicationContext.xml");
    }
```

Class com.ontology2.bakemono.MainBase:
```java
public MainBase(String[] arg0) {
        args= Lists.newArrayList(arg0);
        context=new ClassPathXmlApplicationContext(getApplicationContextPath().toArray(new String[] {})); # bean is already instantiated here
    }
```

## awspring/spring-cloud-aws

### Commit [1c423725a7c1d87b72a5b36da4edf968fffc316f](https://github.com/awspring/spring-cloud-aws/commit/1c423725a7c1d87b72a5b36da4edf968fffc316f):
Test: io.awspring.cloud.autoconfigure.sns.it.SnsTemplateIntegrationTest
Affected by: io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration

Class io.awspring.cloud.autoconfigure.sns.it.SnsTemplateIntegrationTest:
```java
  SnsClient client = context.getBean(SnsClient.class);
```

Class io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration:
```java
// Before:
	@ConditionalOnMissingBean
	@Bean
	public SnsClient snsClient(SnsProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		return (SnsClient) awsClientBuilderConfigurer.configure(SnsClient.builder(), properties).build();
	}

// After:
	@ConditionalOnMissingBean
	@Bean
	public SnsClient snsClient(SnsProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<SnsClientBuilder>> configurer) {
		return awsClientBuilderConfigurer.configure(SnsClient.builder(), properties, configurer.getIfAvailable())
				.build();
	}
```


## weld/weld-testing

### Commit [7243c80931ff04d6c9369655f5157250ad9e9d18](https://github.com/weld/weld-testing/commit/7243c80931ff04d6c9369655f5157250ad9e9d18):
Test: org.jboss.weld.junit5.auto.AddBeanClassesTest
Affected by: org.jboss.weld.junit5.auto.beans.V8

Class org.jboss.weld.junit5.auto.AddBeanClassesTest:
```java
@EnableAutoWeld
@AddBeanClasses(V8.class)
public class AddBeanClassesTest {

  @Inject
  private Engine engine;

  [...]
}
```

Class org.jboss.weld.junit5.auto.beans.V8:
```java
@ApplicationScoped // This line has been removed
public class V8 implements Engine, Serializable {
  [...]
}
```