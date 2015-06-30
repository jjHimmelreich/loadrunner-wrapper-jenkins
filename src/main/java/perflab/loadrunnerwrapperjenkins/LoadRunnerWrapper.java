package perflab.loadrunnerwrapperjenkins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class LoadRunnerWrapper {
	private String loadRunnerScenario;
	private String loadRunnerAnalysisHTMLReportFolder;
	private String loadRunnerResultsFolder;
	private String loadRunnerAnalysisTemplateName;
	private String loadRunnerResultsSummaryFile;
	private String loadRunnerBin;
	private String loadRunnerControllerAdditionalAttributes;
	private String loadRunnerResultsSummaryFileFormat;
	private ArrayList<LoadRunnerTransaction> transactions = new ArrayList<LoadRunnerTransaction>();
	private Date startTime;
	private PrintStream logger;

	/**
	 * @param printStream
	 * @parameter default-value="${basedir}"
	 */
	public LoadRunnerWrapper(String loadRunnerBin, String loadRunnerScenario,
			String loadRunnerControllerAdditionalAttributes,
			String loadRunnerResultsFolder,
			String loadRunnerAnalysisTemplateName,
			String loadRunnerAnalysisHTMLReportFolder,
			String loadRunnerResultsSummaryFile,
			String loadRunnerResultsSummaryFileFormat, PrintStream logger) {
		this.loadRunnerBin = loadRunnerBin;
		this.loadRunnerScenario = loadRunnerScenario;
		this.loadRunnerResultsFolder = loadRunnerResultsFolder;
		this.loadRunnerAnalysisHTMLReportFolder = loadRunnerAnalysisHTMLReportFolder;
		this.loadRunnerResultsSummaryFile = loadRunnerResultsSummaryFile;
		this.loadRunnerControllerAdditionalAttributes = loadRunnerControllerAdditionalAttributes;
		this.loadRunnerAnalysisTemplateName = loadRunnerAnalysisTemplateName;
		this.loadRunnerResultsSummaryFileFormat = loadRunnerResultsSummaryFileFormat;
		this.logger = logger;
	}

	public boolean execute() {

		boolean okay = true;

		startTime = new Date();

		/* Run controller */
		// "c:\Program Files(x86)\HP\LoadRunner\bin\Wlrun.exe" -Run -TestPath "C:\Program Files(x86)\HP\LoadRunner\scenario\Scenario1.lrs" -ResultName "C:\Jenkins\workspace\RunLoadrunner\44"

		//Check if lrs exists
		boolean lrsExists = checkIfScenarioExists();
        if(!lrsExists) {
            logger.println("[ERROR] Scenario file " + this.loadRunnerScenario + " was not found on slave. Aborting job");
            System.out.println("[ERROR] Scenario file " + this.loadRunnerScenario + " was not found on slave. Aborting job");
            okay = false;
            return okay;
        }

		//Check if Analysis template exists
		boolean analysisTemplateExists = checkIfTemplateExists();
        if(!analysisTemplateExists) {
            logger.println("[ERROR] Analysis Template " + this.loadRunnerScenario + " was not found on slave. Aborting job");
            System.out.println("[ERROR] Scenario file " + this.loadRunnerScenario + " was not found on slave. Aborting job");
            okay = false;
            return okay;
        }

		StringBuilder sb = new StringBuilder("\"").append(loadRunnerBin).append("\\").append("Wlrun.exe").append("\"")
                .append(" -Run ")
				.append(" -TestPath ").append("\"").append(loadRunnerScenario).append("\"")
                .append(" -ResultName ").append("\"").append(loadRunnerResultsFolder).append("\"");

		if (loadRunnerControllerAdditionalAttributes != null && !loadRunnerControllerAdditionalAttributes.isEmpty()) {
			sb.append(" ").append(loadRunnerControllerAdditionalAttributes);
		}

		String controllerCommand = sb.toString();

		// String controllerCommand = "\"" + loadRunnerBin + "\\Wlrun.exe\" -Run -TestPath \"" + loadRunnerScenario + "\" -ResultName " + loadRunnerResultsFolder + " " + loadRunnerControllerAdditionalAttributes;
		// Wlrun.exe -Run -TestPath scenario.lrs -ResultName res_folder -InvokeAnalysis

		int controllerRC = runCommand(controllerCommand);

		if (controllerRC != -1) {
			/* Run Analysis if controller return code is okay */
			// "c:\Program Files\Mercury\LoadRunner\bin\AnalysisUI.exe" -RESULTPATH C:\Temp\30users\30users.lrr -TEMPLATENAME
			// WinResTemplate
			String resultsFile = getResultsFile(loadRunnerResultsFolder);

            if(resultsFile.isEmpty()){
                logger.println("[ERROR] Analysis session file (.lrr) was not  found in "+ loadRunnerResultsFolder + " folder. Aborting job");
                System.out.println("[ERROR] Analysis session file (.lrr) was not  found in " + loadRunnerResultsFolder + " folder. Aborting job");
                okay = false;
                return okay;
            }

			String analysisCommand = "\"" + loadRunnerBin + "\\AnalysisUI.exe\" " + " -RESULTPATH " + resultsFile;

			if (loadRunnerAnalysisTemplateName != null && !loadRunnerAnalysisTemplateName.isEmpty()) {
				analysisCommand = analysisCommand + " -TEMPLATENAME " + loadRunnerAnalysisTemplateName;
			}

			int analysisRC = runCommand(analysisCommand);

			/*
			 * Parse analysis results and extract short report if analysis
			 * return code is okay
			 */
			if (analysisRC != -1) {
				extractKPIs(loadRunnerResultsFolder, loadRunnerAnalysisHTMLReportFolder);
			}
			okay = true;
		} else {
			logger.println("Controller failed. Exit code is: " + controllerRC);
			System.out.println("Controller failed. Exit code is: " + controllerRC);
			okay = false;
		}
		return okay;
	}

    private boolean checkIfTemplateExists() {
        boolean okay = false;

        File template=new File(System.getenv("LR_PATH") + "\\AnalysisTemplates\\" + this.loadRunnerAnalysisTemplateName);
        if (template.exists() && template.isDirectory()) {
            okay = true;
        }
        return okay;
    }


    private boolean checkIfScenarioExists() {
		boolean okay = false;

		File lrs=new File(this.loadRunnerScenario);
		if (lrs.exists() && !lrs.isDirectory()) {
			okay = true;
		}

		return okay;
	}

	/**
	 * @param resultsFolder
	 * @return name of lrr file name in results folder
	 */
	private String getResultsFile(String resultsFolder) {

		logger.println("Looking for lrr file in " + resultsFolder);

		String lrrFile = findFilebyRegex(resultsFolder, "*.lrr");

		return lrrFile;
	}

	/**
	 * @param path
	 * @param pattern
	 * @return first file according to the pattern
	 */
	private String findFilebyRegex(String path, String pattern) {
		String foundFile = "";

		try {

			File dir = new File(path);
			if (dir.exists() && dir.isDirectory()) {
				FileFilter fileFilter = new WildcardFileFilter(pattern);
				File[] files = dir.listFiles(fileFilter);

				//logger.println("Length: " + files.length);

				foundFile = files[0].getAbsolutePath();
			} else if (dir.isFile()) {
				logger.println(path + " not exists or not a folder...");
			}

		} catch (Exception e) {
			logger.println("Can't find lrr file " + e.getMessage());
		}
		return foundFile;
	}

	/**
	 * @param command
	 *            - command to execute
	 * @return command exit code
	 */
	private int runCommand(String command) {
		int exitCode = -1;
		logger.println("Command to run: " + command);

		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			exitCode = p.exitValue();
		} catch (Exception err) {
			err.printStackTrace();
		}

		// getLog().info("Exit value: " + exitCode);
		return exitCode;
	}

	/**
	 * Generates report in format expected by
	 * https://wiki.jenkins-ci.org/display/JENKINS/PerfPublisher+Plugin examples
	 * here: file:///C:/Users/i046774/Downloads/master-s-thesis-designing-and-
	 * automating-dynamic-testing-of-software-nightly-builds.pdf
	 * */
	protected void extractKPIs(String resultsFolder, String htmlReportFolder) {
		
		String summaryString = "";
		
		parseSummaryFile(htmlReportFolder + "\\summary.html", loadRunnerResultsSummaryFile);		

		if (this.loadRunnerResultsSummaryFileFormat.equals("PerfPublisherReport")) {
			summaryString = generatePerfPublisherReport(this.transactions);
		} else if (this.loadRunnerResultsSummaryFileFormat.equals("PlotCSVReport")) {
			summaryString = generatePlotCSVReport(this.transactions);
		} else if (this.loadRunnerResultsSummaryFileFormat.equals("jUnitReport")) {
			summaryString = generatejUnitReport(this.transactions);
		}

		try {
			FileUtils.writeStringToFile(new File(loadRunnerResultsSummaryFile),
					summaryString);
			logger.println(summaryString);
			logger.println("Report is saved to " + loadRunnerResultsSummaryFile);
		} catch (IOException e) {
			e.printStackTrace();
			logger.println("Can't write custom csv report for plotting "
					+ e.getMessage());
		}
	}

	/**
	 * @param htmlSummaryFile
	 *            - load runner analysis html report file to parse
	 * @param summaryFile
	 *            - location of summary file to be generated out of loadrunner
	 *            html analysis
	 */
	protected void parseSummaryFile(String htmlSummaryFile, String summaryFile) {
		try {

			File input = new File(htmlSummaryFile);
			Document document = Jsoup.parse(input, "UTF-8");
			Document parse = Jsoup.parse(document.html());
			Elements table = parse.select("table").select(
					"[summary=Transactions statistics summary table]");
			Elements rows = table.select("tr");

			logger.println("number of rows in summary file=" + rows.size());

			for (Element row : rows) {

				// logger.println("table element = " + row.toString());

				String name = row.select("td[headers=LraTransaction Name]")
						.select("span").text();

				if (!name.isEmpty()) {

					float avgRT = Float.valueOf(row.select("td[headers=LraAverage]").select("span").text());
					float minRT = Float.valueOf(row.select("td[headers=LraMinimum]").select("span").text());
					float maxRT = Float.valueOf(row.select("td[headers=LraMaximum]").select("span").text());
					int passed = Integer.valueOf(row.select("td[headers=LraPass]").select("span").text().replace(".", "").replace(",", ""));
					int failed = Integer.valueOf(row.select("td[headers=LraFail]").select("span").text().replace(".", "").replace(",", ""));

					// logger.println("Saving Transaction [" + name + "]");
					this.transactions.add(new LoadRunnerTransaction(name, minRT, avgRT, maxRT, passed, failed));
				}
			}

		} catch (IOException e) {
			logger.println("Can't read LoadRunner Analysis html report " + e.getMessage());
		}

	}

	/**
	 * @param transactions
	 *            - ArrayList of LoadRunnerTransaction objects
	 * @return
	 */
	private String generatejUnitReport(
			ArrayList<LoadRunnerTransaction> transactions) {
		String stringReport = "";
		logger.println("Transformation to jUnit XML started ...");
		try {
			/*
			 * http://llg.cubic.org/docs/junit/
			 *<testsuite tests="3" time="42.5"> 
			 * 	<testcase classname="ZZZ_1" name="ZZZ_1" time="10.1"/>
			 *  <testcase classname="ZZZ_2" name="ZZZ_2" time="11.7"/>
			 *  <testcase classname="ZZZ_3" name="ZZZ_3" time="12.2">
			 *  <!--failure type="NotEnoughFoo"> too slow </failure-->
			 *  </testcase> 
			 *</testsuite>
			 */

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			org.w3c.dom.Document doc = (org.w3c.dom.Document) docBuilder.newDocument();
			org.w3c.dom.Element testsuiteElement = (org.w3c.dom.Element) doc.createElement("testsuite");
			testsuiteElement.setAttribute("tests", String.valueOf(transactions.size()));
			// testsuiteElement.setAttribute("time", "total test duration");
			doc.appendChild(testsuiteElement);

			// //////////////////////////////////////////////////////////////////////////

			for (LoadRunnerTransaction tr : this.transactions) {

				logger.println("Dump " + tr.getName());

				org.w3c.dom.Element testcaseElement = doc.createElement("testcase");
				//testcaseElement.setAttribute("classname", tr.getName());
                testcaseElement.setAttribute("classname", "load." + new File(this.loadRunnerScenario).getName().replace(".lrs",""));
				testcaseElement.setAttribute("name", tr.getName());
				testcaseElement.setAttribute("time", String.valueOf(tr.getAvgRT()));

				testsuiteElement.appendChild(testcaseElement);
			}

			// //////////////////////////////////////////////////////////////////////////
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			stringReport = this.getStringFromDoc(doc);

		} catch (ParserConfigurationException pce) {
			logger.println(pce.getMessage());
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			logger.println(tfe.getMessage());
			tfe.printStackTrace();
		}
		return stringReport;
	}

	/**
	 * @param transactions
	 *            - ArrayList of LoadRunnerTransaction objects
	 * @return
	 */
	private String generatePlotCSVReport(
			ArrayList<LoadRunnerTransaction> transactions) {

		logger.println("Transformation CSV started ...");

		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> averages = new ArrayList<String>();

		for (LoadRunnerTransaction tr : this.transactions) {
			headers.add("\"" + tr.getName() + "\"");
			averages.add(String.valueOf(tr.getAvgRT()));
		}

		String scvReport = org.apache.commons.lang3.StringUtils.join(headers,",")
				+ System.getProperty("line.separator")
				+ org.apache.commons.lang3.StringUtils.join(averages, ",");

		return scvReport;		
	}

	/**
	 * @param transactions
	 *            - ArrayList of LoadRunnerTransaction objects
	 * @param summaryFile
	 *            - location of SCV summary file to be generated out of
	 *            transaction objects in PerfPublisher Report format
	 * @return
	 */
	private String generatePerfPublisherReport( ArrayList<LoadRunnerTransaction> transactions) {
		
		logger.println("Transformation to XML started ...");
		String stringReport = "";
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			org.w3c.dom.Document doc = (org.w3c.dom.Document) docBuilder.newDocument();
			org.w3c.dom.Element reportElement = (org.w3c.dom.Element) doc.createElement("report");
			doc.appendChild(reportElement);

			// //////////////////////////////////////////////////////////////////////////

			// <categories>
			// <category name="memory" scale="mb">
			// <observations>
			// <observation name="Server 1">100</observation>
			// <observation name="Server 2">200</observation>
			// </observations>
			// </category>

			// <category name="disk" scale="gb">
			// <observations>
			// <observation name="Server 1">41</observation>
			// <observation name="Server 2">58</observation>
			// </observations>
			// </category>
			// </categories>

			// start element
			org.w3c.dom.Element startElement = (org.w3c.dom.Element) doc.createElement("start");
			reportElement.appendChild(startElement);

			// date element
			org.w3c.dom.Element date = (org.w3c.dom.Element) doc.createElement("date");
			startElement.appendChild(date);

			date.setAttribute("format", "YYYYMMDD");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMdd");
			date.setAttribute("val", sdf.format(startTime));

			// time element
			org.w3c.dom.Element time = (org.w3c.dom.Element) doc.createElement("date");
			startElement.appendChild(time);

			time.setAttribute("format", "HHMMSS");
			SimpleDateFormat stf = new SimpleDateFormat("hhmmss");
			time.setAttribute("val", stf.format(startTime));

			// //////////////////////////////////////////////////////////////////////////

			// <test name="Smoke test" executed="yes" categ="Smoke test">

			// <description>Tests if ATE LAN socket and communication
			// works.</description>

			// <result>
			// <success passed="yes" state ="100" hasTimedOut="no" />
			// <compiletime unit="s" mesure="0" isRelevant="yes" />
			// <performance unit="%" mesure="0" isRelevant="yes" />
			// <executiontime unit="s" mesure="12" isRelevant="yes" />
			// <metrics>
			// <006_My_Benefits unit="sec" mesure="0.115" isRelevant="yes"/>
			// <007_My_Timesheet unit="sec" mesure="1.247" isRelevant="yes"/>
			// </metrics>
			// </result>
			// </test>
			// </report>

			// //////////////////////////////////////////////////////////////////////////
			// test element
			org.w3c.dom.Element testElement = doc.createElement("test");
			reportElement.appendChild(testElement);

			testElement.setAttribute("name", "Load test");
			testElement.setAttribute("executed", "yes");
			testElement.setAttribute("categ", "Load test");

			// description element
			org.w3c.dom.Element descriptionElement = doc.createElement("description");
			descriptionElement.appendChild(doc.createTextNode("This is the best Load test ever executed..."));
			reportElement.appendChild(descriptionElement);

			// //////////////////////////////////////////////////////////////////////////
			// result
			org.w3c.dom.Element resultElement = doc.createElement("result");
			reportElement.appendChild(resultElement);

			org.w3c.dom.Element successElement = doc.createElement("success");
			resultElement.appendChild(successElement);

			org.w3c.dom.Element compiletimeElement = doc.createElement("compiletime");
			resultElement.appendChild(compiletimeElement);

			org.w3c.dom.Element performanceElement = doc.createElement("performance");
			resultElement.appendChild(performanceElement);

			org.w3c.dom.Element executiontimeElement = doc.createElement("executiontime");
			resultElement.appendChild(executiontimeElement);

			org.w3c.dom.Element metricsElement = doc.createElement("metrics");
			resultElement.appendChild(metricsElement);

			// //////////////////////////////////////////////////////////////////////////

			for (LoadRunnerTransaction tr : this.transactions) {
				// <006_My_Benefits unit="sec" mesure="0.115" isRelevant="yes"/>
				String trName = "tr_" + tr.getName();
				logger.println("Dump " + trName);

				org.w3c.dom.Element trElement = doc.createElement(trName);
				trElement.setAttribute("unit", "sec");
				trElement.setAttribute("mesure", String.valueOf(tr.getAvgRT()));
				trElement.setAttribute("isRelevant", "yes");
				metricsElement.appendChild(trElement);
			}

			// //////////////////////////////////////////////////////////////////////////
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");

			stringReport = this.getStringFromDoc(doc);

		} catch (ParserConfigurationException pce) {
			logger.println(pce.getMessage());
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			logger.println(tfe.getMessage());
			tfe.printStackTrace();
		}
		return stringReport;
	}

	private String getStringFromDoc(org.w3c.dom.Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			writer.flush();
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
