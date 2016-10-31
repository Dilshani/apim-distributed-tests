package org.wso2.apim.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.TestNGException;
import org.testng.annotations.BeforeSuite;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.wso2.carbon.automation.distributed.beans.Deployment;
import org.wso2.carbon.automation.distributed.beans.TestLink;

import org.wso2.carbon.automation.distributed.commons.DeploymentConfigurationReader;
import org.wso2.carbon.automation.distributed.utills.TestLinkBuilder;
import org.wso2.carbon.automation.distributed.utills.TestLinkSiteUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Running the TestNG programmatically and Injecting the suites.
 */
public class TestNgExecuter {
    private static final Log log = LogFactory.getLog(TestNgExecuter.class);

    @BeforeSuite
    public void executeEnvironment() throws IOException
    {

        TestLink testLinkBean =  DeploymentConfigurationReader.readConfiguration().getTestLinkConfigurations();
        List<Deployment> deploymentList;

        if (testLinkBean.isEnabled()){

            TestLinkBuilder tlbuilder = new TestLinkBuilder();
            log.info("Connecting to TestLink : " +testLinkBean.getUrl());
            TestLinkSiteUtil tlsite;

            HashMap<String, Deployment> deploymentHashMap =  DeploymentConfigurationReader.readConfiguration().getDeploymentHashMap();
            deploymentList = new ArrayList<>(deploymentHashMap.values());
            ArrayList tcList;

            TestNG tng = new TestNG();
            List<XmlSuite> suites = new ArrayList<>();

            for (Deployment deployment : deploymentList) {
                // The Test Link platform should be same as the deployment pattern name
                tlsite = tlbuilder.getTestLinkSite(testLinkBean.getUrl(),testLinkBean.getDevkey(),testLinkBean.getProjectName(),testLinkBean.getTestPlan(),deployment.getName());
                log.info("Retrieving Automation Test from TestLink. Project : " + testLinkBean.getProjectName() + " Test Plan : " +testLinkBean.getTestPlan() + " Platform : " +deployment.getName());
                tcList = tlsite.getTestCaseClassList(new String[]{testLinkBean.getTestLinkCustomField()});

                XmlSuite suite = new XmlSuite();
//                SuiteGenerator.createSuite()
                suite.setName(deployment.getName());
                XmlTest test = new XmlTest(suite);
                test.setName("AutomationTests");
                List<XmlClass> classes = new ArrayList<>();

                for (Object className : tcList){
                    try {
                        classes.add(new XmlClass((String) className));
                    } catch (TestNGException e){
                        log.error("Error occurred while adding the class : " +e.toString());
                    }
                }
                test.setXmlClasses(classes) ;
                suites.add(suite);
            }
            tng.setXmlSuites(suites);
            tng.setPreserveOrder(true);
            log.info("Running Test Suites.........");
            TestListenerAdapter tl = new TestListenerAdapter();
            tng.addListener(tl);
            tng.setOutputDirectory("apim-intergration-tests"); //TODO remove hardcoding
            tng.run();
        } else {
            log.info("Retrieve Tests from Test-Link is Disabled, Hence Retrieving Tests from default testng.xml");
            TestNG testng = new TestNG();
            List<String> suites = new ArrayList<>();
            suites.add("../src/test/resources/testng.xml"); // TODO The path shouldn't be hardcoded.
            testng.setTestSuites(suites);
            testng.setOutputDirectory("apim-intergration-tests"); //TODO remove hardcoding
            testng.run();
        }

    }
}
