package perflab.loadrunnerwrapperjenkins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;
import perflab.loadrunnerwrapperjenkins.LoadRunnerWrapper;
import perflab.loadrunnerwrapperjenkins.LoadRunnerWrapperJenkins;
import perflab.loadrunnerwrapperjenkins.LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary;

public class test_ParseAnalysis extends TestCase {

	@Test
	public void test_parseHTML12_54() throws IOException {
		String loadRunnerBin = ""; 
		String loadRunnerScenario = ""; 
		String loadRunnerControllerAdditionalAttributes = ""; 
		String loadRunnerResultsFolder = ""; 
		String loadRunnerAnalysisTemplateName = ""; 
		String loadRunnerAnalysisHTMLReportFolder = ""; 
		String loadRunnerResultsSummaryFile = ""; 
		String loadRunnerResultsSummaryFileFormat = ""; 
		ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction = new ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary>();
        int acceptedFailurePercentage = 5;
        PrintStream logger = System.out;
        File resourcesDirectory = new File("src/test/resources");
		
        LoadRunnerWrapper lrw = new LoadRunnerWrapper(
				loadRunnerBin, 
				loadRunnerScenario, 
				loadRunnerControllerAdditionalAttributes, 
				loadRunnerResultsFolder, 
				loadRunnerAnalysisTemplateName, 
				loadRunnerAnalysisHTMLReportFolder, 
				loadRunnerResultsSummaryFile, 
				loadRunnerResultsSummaryFileFormat, 
				reportTargetsValuesPerTransaction, 
				acceptedFailurePercentage, 
				logger);
		
		String htmlSummaryFile = resourcesDirectory.getAbsolutePath().concat("\\12.54\\An_Report1\\summary.html");
		String summaryFile = resourcesDirectory.getAbsolutePath().concat("\\12.54\\jUnit.xml");

		lrw.parseSummaryFile(htmlSummaryFile, summaryFile);
		ArrayList<LoadRunnerTransaction> transactions= lrw.getTransactions();
		
		Assert.assertEquals(12, transactions.size());
		
		LoadRunnerTransaction tr = transactions.get(5);
		Assert.assertEquals("Search_a_300_Results", tr.getName());
		Assert.assertEquals(0.346f, tr.getMinRT(), 0);
		Assert.assertEquals(0.442f, tr.getMaxRT(), 0);	
		Assert.assertEquals(0.394f, tr.getAvgRT(), 0);	
		
		String expectedReport = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
								"<testsuite tests=\"12\">"+
								"<testcase classname=\"load.\" name=\"001_SAML\" time=\"0.314\"/>"+
								"<testcase classname=\"load.\" name=\"002_Logon_SDN\" time=\"0.213\"/>"+
								"<testcase classname=\"load.\" name=\"GIT_Commit\" time=\"0.856\"/>"+
								"<testcase classname=\"load.\" name=\"GIT_Fetch\" time=\"0.411\"/>"+
								"<testcase classname=\"load.\" name=\"IDE_Reload\" time=\"8.573\"/>"+
								"<testcase classname=\"load.\" name=\"Search_a_300_Results\" time=\"0.394\"/>"+
								"<testcase classname=\"load.\" name=\"Search_GUID\" time=\"0.048\"/>"+
								"<testcase classname=\"load.\" name=\"Update_User_Preferences\" time=\"0.044\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Create_File\" time=\"0.462\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Delete_File\" time=\"0.586\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Read_File\" time=\"0.052\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Save_1_File\" time=\"0.313\"/>"+
								"</testsuite>";
		String junitXMLReport = lrw.generatejUnitReport(transactions, reportTargetsValuesPerTransaction);
		
		Assert.assertEquals(expectedReport, junitXMLReport);
	}
	
	@Test
	public void test_parseHTML11_52() throws IOException {
		String loadRunnerBin = ""; 
		String loadRunnerScenario = ""; 
		String loadRunnerControllerAdditionalAttributes = ""; 
		String loadRunnerResultsFolder = ""; 
		String loadRunnerAnalysisTemplateName = ""; 
		String loadRunnerAnalysisHTMLReportFolder = ""; 
		String loadRunnerResultsSummaryFile = ""; 
		String loadRunnerResultsSummaryFileFormat = ""; 
		ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction = new ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary>();
        int acceptedFailurePercentage = 5;
        PrintStream logger = System.out;
        File resourcesDirectory = new File("src/test/resources");
		
        LoadRunnerWrapper lrw = new LoadRunnerWrapper(
				loadRunnerBin, 
				loadRunnerScenario, 
				loadRunnerControllerAdditionalAttributes, 
				loadRunnerResultsFolder, 
				loadRunnerAnalysisTemplateName, 
				loadRunnerAnalysisHTMLReportFolder, 
				loadRunnerResultsSummaryFile, 
				loadRunnerResultsSummaryFileFormat, 
				reportTargetsValuesPerTransaction, 
				acceptedFailurePercentage, 
				logger);
		
		String htmlSummaryFile = resourcesDirectory.getAbsolutePath().concat("\\11.52\\An_Report1\\summary.html");
		String summaryFile = resourcesDirectory.getAbsolutePath().concat("\\11.52\\jUnit.xml");

		lrw.parseSummaryFile(htmlSummaryFile, summaryFile);
		ArrayList<LoadRunnerTransaction> transactions= lrw.getTransactions();
		
		//lrw.generatejUnitReport(transactions, reportTargetsValuesPerTransaction);
		
		Assert.assertEquals(15, transactions.size());
		
		LoadRunnerTransaction tr = transactions.get(1);
		Assert.assertEquals("002_Logon_SDN", tr.getName());
		Assert.assertEquals(0.17f,  tr.getMinRT(), 0);
		Assert.assertEquals(0.376f, tr.getMaxRT(), 0);
		Assert.assertEquals(0.179f, tr.getAvgRT(), 0);
		
		String expectedReport = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
								"<testsuite tests=\"15\">"+
								"<testcase classname=\"load.\" name=\"001_SAML\" time=\"0.26\"/>"+
								"<testcase classname=\"load.\" name=\"002_Logon_SDN\" time=\"0.179\"/>"+
								"<testcase classname=\"load.\" name=\"GIT_Commit\" time=\"0.685\"/>"+
								"<testcase classname=\"load.\" name=\"GIT_Fetch\" time=\"0.389\"/>"+
								"<testcase classname=\"load.\" name=\"GIT_Rebase\" time=\"0.599\"/>"+
								"<testcase classname=\"load.\" name=\"IDE_Reload\" time=\"7.277\"/>"+
								"<testcase classname=\"load.\" name=\"Preview_Run_WebApp\" time=\"0.116\"/>"+
								"<testcase classname=\"load.\" name=\"Renew Token - 403\" time=\"0.028\"/>"+
								"<testcase classname=\"load.\" name=\"Search_a_300_Results\" time=\"0.4\"/>"+
								"<testcase classname=\"load.\" name=\"Search_GUID\" time=\"0.061\"/>"+
								"<testcase classname=\"load.\" name=\"Update_User_Preferences\" time=\"0.058\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Create_File\" time=\"0.349\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Delete_File\" time=\"0.267\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Read_File\" time=\"0.062\"/>"+
								"<testcase classname=\"load.\" name=\"WM_Save_1_File\" time=\"0.309\"/>"+
								"</testsuite>";
		
		String junitXMLReport = lrw.generatejUnitReport(transactions, reportTargetsValuesPerTransaction);
		
		Assert.assertEquals(expectedReport, junitXMLReport);
		
	}
	
}
