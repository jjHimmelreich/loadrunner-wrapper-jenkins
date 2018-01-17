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

import org.jfree.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.HashMap;

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
	private int acceptedFailurePercentage;
	private ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction;
	private Date startTime;
	private PrintStream logger;
	private HashMap<String, Pair> LRSFlags;
	private HashMap<String, Pair> AnalysisTemplatesFlags;

	private enum TRANSACTION_STATUS {
		SUCCESS, ERROR, FAILURE
	}

	/**
	 * Constructor for LoadRunnerWrapper
	 */
	public LoadRunnerWrapper(String loadRunnerBin, 
			String loadRunnerScenario,
			String loadRunnerControllerAdditionalAttributes, 
			String loadRunnerResultsFolder,
			String loadRunnerAnalysisTemplateName, 
			String loadRunnerAnalysisHTMLReportFolder,
			String loadRunnerResultsSummaryFile, 
			String loadRunnerResultsSummaryFileFormat,
			ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction,
			int acceptedFailurePercentage, 
			PrintStream logger) {

		this.loadRunnerBin = loadRunnerBin;
		this.loadRunnerScenario = loadRunnerScenario;
		this.loadRunnerResultsFolder = loadRunnerResultsFolder;
		this.loadRunnerAnalysisHTMLReportFolder = loadRunnerAnalysisHTMLReportFolder;
		this.loadRunnerResultsSummaryFile = loadRunnerResultsSummaryFile;
		this.loadRunnerControllerAdditionalAttributes = loadRunnerControllerAdditionalAttributes;
		this.loadRunnerAnalysisTemplateName = loadRunnerAnalysisTemplateName;
		this.loadRunnerResultsSummaryFileFormat = loadRunnerResultsSummaryFileFormat;
		this.acceptedFailurePercentage = acceptedFailurePercentage;
		this.reportTargetsValuesPerTransaction = reportTargetsValuesPerTransaction;
		this.logger = logger;
		LRSFlags = new HashMap<String, Pair>();
		AnalysisTemplatesFlags = new HashMap<String, Pair>();
		setFlags();
	}

	private void setFlags() {
		LRSFlags.put("AutoSetResults", new Pair("0", false));
		LRSFlags.put("AutoOverwriteResults", new Pair("0", false));

		AnalysisTemplatesFlags.put("AutoHtml", new Pair("1", false));
		AnalysisTemplatesFlags.put("AutoSave", new Pair("1", false));
		AnalysisTemplatesFlags.put("AutoClose", new Pair("1", false));
	}

	/**
	 * Execute sequence of Controller and Analysis
	 */
	public boolean execute() {

		boolean okay = true;

		startTime = new Date();

		/* Run controller */
		// "c:\Program Files(x86)\HP\LoadRunner\bin\Wlrun.exe" -Run -TestPath
		// "C:\Program Files(x86)\HP\LoadRunner\scenario\Scenario1.lrs"
		// -ResultName "C:\Jenkins\workspace\RunLoadrunner\44"

		// Check if lrs exists
		boolean lrsExists = checkIfScenarioExists();
		if (!lrsExists) {
			Log.error("Scenario file " + this.loadRunnerScenario + " was not found on slave. Aborting job");
			logger.println(
					"[ERROR] Scenario file " + this.loadRunnerScenario + " was not found on slave. Aborting job");

			okay = false;
			return okay;
		}

		if (!isFileWellConfigured(this.loadRunnerScenario, LRSFlags)) {
			okay = false;
			return okay;
		}

		// Check if Analysis template exists
		boolean analysisTemplateExists = checkIfTemplateExists();
		if (!analysisTemplateExists) {
			Log.error("Template file " + this.loadRunnerAnalysisTemplateName + " was not found on slave. Aborting job");
			logger.println("[ERROR] Template file " + this.loadRunnerAnalysisTemplateName
					+ " was not found on slave. Aborting job");
			okay = false;
			return okay;
		}

		String templateName = System.getenv("LR_PATH") + "\\AnalysisTemplates\\" + this.loadRunnerAnalysisTemplateName
				+ "\\" + this.loadRunnerAnalysisTemplateName + ".tem";
		if (!isFileWellConfigured(templateName, AnalysisTemplatesFlags)) {
			okay = false;
			return okay;
		}

		StringBuilder sb = new StringBuilder("\"").append(loadRunnerBin).append("\\").append("Wlrun.exe").append("\"")
				.append(" -Run ").append(" -TestPath ").append("\"").append(loadRunnerScenario).append("\"")
				.append(" -ResultName ").append("\"").append(loadRunnerResultsFolder).append("\"");

		if (loadRunnerControllerAdditionalAttributes != null && !loadRunnerControllerAdditionalAttributes.isEmpty()) {
			sb.append(" ").append(loadRunnerControllerAdditionalAttributes);
		}

		String controllerCommand = sb.toString();

		// String controllerCommand = "\"" + loadRunnerBin + "\\Wlrun.exe\" -Run
		// -TestPath \"" + loadRunnerScenario + "\" -ResultName " +
		// loadRunnerResultsFolder + " " +
		// loadRunnerControllerAdditionalAttributes;
		// Wlrun.exe -Run -TestPath scenario.lrs -ResultName res_folder
		// -InvokeAnalysis

		int controllerRC = runCommand(controllerCommand);

		if (controllerRC != -1) {
			/* Run Analysis if controller return code is okay */
			// "c:\Program Files\Mercury\LoadRunner\bin\AnalysisUI.exe"
			// -RESULTPATH C:\Temp\30users\30users.lrr -TEMPLATENAME
			// WinResTemplate
			String resultsFile = getResultsFile(loadRunnerResultsFolder);

			if (resultsFile.isEmpty()) {
				logger.println("[ERROR] Analysis session file (.lrr) was not  found in " + loadRunnerResultsFolder
						+ " folder. Aborting job");
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
			Log.error("Controller failed. Exit code is: " + controllerRC);
			logger.println("[ERROR] Controller failed. Exit code is: " + controllerRC);
			okay = false;
		}
		return okay;
	}

	/**
	 * check if this.loadRunnerAnalysisTemplateName exists on filesystem
	 * 
	 * return - true if template file exists
	 */
	private boolean checkIfTemplateExists() {
		boolean okay = false;

		File template = new File(
				System.getenv("LR_PATH") + "\\AnalysisTemplates\\" + this.loadRunnerAnalysisTemplateName);
		if (template.exists() && template.isDirectory()) {
			okay = true;
		}
		return okay;
	}

	/**
	 * check if loadrunner scenario exists
	 * 
	 * return - true if LRS file exists
	 */
	private boolean checkIfScenarioExists() {
		boolean okay = false;

		File lrs = new File(this.loadRunnerScenario);
		if (lrs.exists() && !lrs.isDirectory()) {
			okay = true;
		}

		return okay;
	}

	private boolean isFileWellConfigured(String fileName, HashMap map) {
		boolean okay = true;

		String value;
		String flag;

		try {
			String fileContent = FileUtils.readFileToString(new File(fileName));

			for (Object key : map.keySet()) {
				String keyStr = key.toString();

				Pair p = (Pair) map.get(key);
				String strToFind = keyStr + "=" + p.getValue();

				if (!fileContent.contains(strToFind)) {
					okay = false;
					logger.println(
							"[ERROR] " + fileName + ":" + strToFind + "  is missing or misconfigured . Aborting job");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return okay;
	}

	/**
	 * Looing for .lrr file ion results folder
	 * 
	 * param resultsFolder
	 * return name of lrr file name in results folder
	 */
	private String getResultsFile(String resultsFolder) {

		logger.println("Looking for lrr file in " + resultsFolder);

		String lrrFile = findFilebyRegex(resultsFolder, "*.lrr");

		return lrrFile;
	}

	/**
	 * param path
	 * param pattern
	 * return first file according to the pattern
	 */
	private String findFilebyRegex(String path, String pattern) {
		String foundFile = "";

		try {

			File dir = new File(path);
			if (dir.exists() && dir.isDirectory()) {
				FileFilter fileFilter = new WildcardFileFilter(pattern);
				File[] files = dir.listFiles(fileFilter);

				// logger.println("Length: " + files.length);

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
	 * param command - command to execute
	 * return command exit code
	 */
	private int runCommand(String command) {
		int exitCode = -1;

		Log.info("Command to run: " + command);

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
	 */
	protected void extractKPIs(String resultsFolder, String htmlReportFolder) {

		String summaryString = "";

		parseSummaryFile(htmlReportFolder + "\\summary.html", loadRunnerResultsSummaryFile);

		if (this.loadRunnerResultsSummaryFileFormat.equals("PerfPublisherReport")) {
			summaryString = generatePerfPublisherReport(this.transactions);
		} else if (this.loadRunnerResultsSummaryFileFormat.equals("PlotCSVReport")) {
			summaryString = generatePlotCSVReport(this.transactions);
		} else if (this.loadRunnerResultsSummaryFileFormat.equals("jUnitReport")) {
			summaryString = generatejUnitReport(this.transactions, this.reportTargetsValuesPerTransaction);
		}

		try {

			logger.println(summaryString);
			FileUtils.writeStringToFile(new File(loadRunnerResultsSummaryFile), summaryString);
			logger.println("Report is saved to " + loadRunnerResultsSummaryFile);

		} catch (IOException e) {

			e.printStackTrace();
			logger.println("[ERROR] Can't write custom csv report for plotting " + e.getMessage());
		}
	}

	/**
	 * param htmlSummaryFile - load runner analysis html report file to parse
	 * param summaryFile    - location of summary file to be generated out of loadrunner html analysis
	 */
	protected void parseSummaryFile(String htmlSummaryFile, String summaryFile) {
		try {

			File input = new File(htmlSummaryFile);
			Document document = Jsoup.parse(input, "UTF-8");
			Document parse = Jsoup.parse(document.html());
			Elements table = parse.select("table").select("[summary=Transactions statistics summary table]");
			Elements rows = table.select("tr");

			logger.println("number of rows in summary file =" + rows.size());

			for (Element row : rows) {
				String name = extractText(row, "td[headers^=LraTransaction Name]");

				if (!name.isEmpty() && !name.equals("None")) {

					float avgRT = Float.valueOf(extractText(row, "td[headers^=LraAverage]"));
					float minRT = Float.valueOf(extractText(row, "td[headers^=LraMinimum]"));
					float maxRT = Float.valueOf(extractText(row, "td[headers^=LraMaximum]"));
					int passed = Integer
							.valueOf(extractText(row, "td[headers^=LraPass]").replace(".", "").replace(",", ""));
					int failed = Integer
							.valueOf(extractText(row, "td[headers^=LraFail]").replace(".", "").replace(",", ""));
					int failedPrecentage = failed / (failed + passed) * 100;

					logger.println("Saving Transaction name=" + name + " | minRT = " + minRT + " | avgRT=" + avgRT
							+ " | maxRT=" + maxRT + " | passed=" + passed + " | failed=" + failed
							+ " | failedPrecentage=" + failedPrecentage + "");
					this.transactions.add(
							new LoadRunnerTransaction(name, minRT, avgRT, maxRT, passed, failed, failedPrecentage));
				}
			}

		} catch (IOException e) {
			Log.error("Can't read LoadRunner Analysis html report " + e.getMessage());
			logger.println("[ERROR] Can't read LoadRunner Analysis html report " + e.getMessage());
		}

                if (!fileContent.contains(strToFind)){
                    okay  = false;
                    logger.println("[ERROR] " + fileName + ":" + strToFind + "  is missing or misconfigured . Aborting job");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return okay;
    }

    /**
     * Looing for .lrr file ion results folder
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
     * @param command - command to execute
     * @return command exit code
     */
    private int runCommand(String command) {
        int exitCode = -1;

        Log.info("Command to run: " + command);

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
     */
    protected void extractKPIs(String resultsFolder, String htmlReportFolder) {

        String summaryString = "";

        parseSummaryFile(htmlReportFolder + "\\summary.html", loadRunnerResultsSummaryFile);

        if (this.loadRunnerResultsSummaryFileFormat.equals("PerfPublisherReport")) { summaryString = generatePerfPublisherReport(this.transactions); }
        else if (this.loadRunnerResultsSummaryFileFormat.equals("PlotCSVReport"))  { summaryString = generatePlotCSVReport(this.transactions); }
        else if (this.loadRunnerResultsSummaryFileFormat.equals("jUnitReport"))    { summaryString = generatejUnitReport(this.transactions, this.reportTargetsValuesPerTransaction); }

        try {

            logger.println(summaryString);
            FileUtils.writeStringToFile(new File(loadRunnerResultsSummaryFile), summaryString);
            logger.println("Report is saved to " + loadRunnerResultsSummaryFile);

        } catch (IOException e) {

            e.printStackTrace();
            logger.println("[ERROR] Can't write custom csv report for plotting " + e.getMessage());
        }
    }

    /**
     * @param htmlSummaryFile - load runner analysis html report file to parse
     * @param summaryFile     - location of summary file to be generated out of loadrunner
     *                        html analysis
     */
    protected void parseSummaryFile(String htmlSummaryFile, String summaryFile) {
        try {

            File input = new File(htmlSummaryFile);
            Document document = Jsoup.parse(input, "UTF-8");
            Document parse = Jsoup.parse(document.html());
            Elements table = parse.select("table").select("[summary=Transactions statistics summary table]");
            Elements rows = table.select("tr");

            logger.println("number of rows in summary file =" + rows.size());

            for (Element row : rows) {       
                String name = extractText(row, "td[headers^=LraTransaction Name]");
                
                if (!name.isEmpty() && !name.equals("None")) {

                    float avgRT = Float.valueOf(extractText(row, "td[headers^=LraAverage]"));
                    float minRT = Float.valueOf(extractText(row, "td[headers^=LraMinimum]"));
                    float maxRT = Float.valueOf(extractText(row, "td[headers^=LraMaximum]"));
                    int passed = Integer.valueOf(extractText(row, "td[headers^=LraPass]").replace(".", "").replace(",", ""));
                    int failed = Integer.valueOf(extractText(row, "td[headers^=LraFail]").replace(".", "").replace(",", ""));
                    int failedPrecentage = failed/(failed+passed)*100;

                    logger.println("Saving Transaction name="+name+" | minRT = "+minRT+" | avgRT="+avgRT+" | maxRT="+maxRT+" | passed="+passed+" | failed="+failed+" | failedPrecentage="+failedPrecentage+"");
                    this.transactions.add(new LoadRunnerTransaction(name, minRT, avgRT, maxRT, passed, failed, failedPrecentage));
                }
            }

        } catch (IOException e) {
            Log.error("Can't read LoadRunner Analysis html report " + e.getMessage());
            logger.println("[ERROR] Can't read LoadRunner Analysis html report " + e.getMessage());
        }

    }

	private String extractText(Element row, String selector) {
		String result = row.select(selector).select("span").text();
		result = result.replaceAll("&nbsp;", "");
		result = result.replaceAll("\u00A0", "");
		result = result.trim();
		return result;
	}

	public void setLoadRunnerResultsSummaryFileFormat(String loadRunnerResultsSummaryFileFormat) {
		this.loadRunnerResultsSummaryFileFormat = loadRunnerResultsSummaryFileFormat;
	}

	public ArrayList<LoadRunnerTransaction> getTransactions() {
		return transactions;
	}

	/**
	 * return
	 * param transactions
	 * param reportTargetsValuesPerTransaction
	 */
	protected String generatejUnitReport(ArrayList<LoadRunnerTransaction> transactions,
			ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction) {
		logger.println("Transformation to jUnit XML started ...");

		String stringReport = "";

		/*
		 * duplicated for(LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary
		 * kpi : reportTargetsValuesPerTransaction){ logger.println("=====" +
		 * kpi.toString()); }
		 */

		try {
			/*
			 * http://llg.cubic.org/docs/junit/ <testsuite tests="3"
			 * time="42.5"> <testcase classname="ZZZ_1" name="ZZZ_1"
			 * time="10.1"/> <testcase classname="ZZZ_2" name="ZZZ_2"
			 * time="11.7"/> <testcase classname="ZZZ_3" name="ZZZ_3"
			 * time="12.2"> <!--failure type="NotEnoughFoo"> too slow
			 * </failure--> </testcase> </testsuite>
			 */

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			org.w3c.dom.Document doc = (org.w3c.dom.Document) docBuilder.newDocument();
			org.w3c.dom.Element testsuiteElement = (org.w3c.dom.Element) doc.createElement("testsuite");
			testsuiteElement.setAttribute("tests", String.valueOf(transactions.size()));

			// TODO: add total tests duration form HTML report
			// testsuiteElement.setAttribute("time", "total test duration");
			doc.appendChild(testsuiteElement);

			// //////////////////////////////////////////////////////////////////////////

			for (LoadRunnerTransaction tr : transactions) {

				String trName = tr.getName();
				float trValue = tr.getAvgRT();
				int trFailedPercentage = tr.getFailedPrecentage();

				org.w3c.dom.Element testcaseElement = doc.createElement("testcase");
				// testcaseElement.setAttribute("classname", tr.getName());
				testcaseElement.setAttribute("classname",
						"load." + new File(loadRunnerScenario).getName().replace(".lrs", ""));
				testcaseElement.setAttribute("name", trName);
				testcaseElement.setAttribute("time", String.valueOf(trValue));

				TRANSACTION_STATUS trStatus = calculateTransactionStatus(trName, trValue, trFailedPercentage,
						reportTargetsValuesPerTransaction);
				switch (trStatus) {
				case ERROR:
					org.w3c.dom.Element errorElement = doc.createElement("error");
					errorElement.setAttribute("message",
							"Precentage of failed transactions is above " + trFailedPercentage);
					errorElement.setTextContent(
							"Amount of failed transactions is above limit - " + trFailedPercentage + "%");
					testcaseElement.appendChild(errorElement);
					break;
				case FAILURE:
					org.w3c.dom.Element failureElement = doc.createElement("failure");
					failureElement.setAttribute("message", "Response time is above "
							+ getTargetValueByTransactionName(trName, reportTargetsValuesPerTransaction));
					failureElement.setTextContent("Average transaction response time is above limit");
					testcaseElement.appendChild(failureElement);
					break;
				}

				testsuiteElement.appendChild(testcaseElement);
			}

			// //////////////////////////////////////////////////////////////////////////
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			stringReport = this.getStringFromDoc(doc);

		} catch (ParserConfigurationException pce) {
			Log.error(pce.getMessage());
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			Log.error(tfe.getMessage());
			tfe.printStackTrace();
		}
		return stringReport;
	}

	private float getTargetValueByTransactionName(String trName,
			ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction) {
		float transactionErrorValue = -1;

		for (LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary target : reportTargetsValuesPerTransaction) {
			if (trName.equals(target.getTransactionName())) {
				transactionErrorValue = target.getTransactionErrorValue();
				break;
			}
		}

		return transactionErrorValue;
	}

	private TRANSACTION_STATUS calculateTransactionStatus(String trName, float trValue, int trFailedPercentage,
			ArrayList<LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary> reportTargetsValuesPerTransaction) {

		TRANSACTION_STATUS status = TRANSACTION_STATUS.SUCCESS;

		for (LoadRunnerWrapperJenkins.LoadRunnerTransactionBoundary target : reportTargetsValuesPerTransaction) {
			if (trName.equals(target.getTransactionName())) {
				if (target.isDoNotCompare() != true) {

					// Above failed transactions limit percentage
					if (trFailedPercentage > this.acceptedFailurePercentage) {
						status = TRANSACTION_STATUS.ERROR;
						break;
					}
					logger.println("Checking: " + trName + " Error after:" + target.getTransactionErrorValue());

					// Bigger then Error
					if (trValue >= target.getTransactionErrorValue()) {
						status = TRANSACTION_STATUS.FAILURE;
					}
				} else {
					logger.println("Skipping evaluation of : " + trName);

					status = TRANSACTION_STATUS.SUCCESS;
				}
				break;
			}
		}

		return status;
	}

	/**
	 * param transactions - ArrayList of LoadRunnerTransaction objects
	 * return - string with CSV formatted report
	 */
	private String generatePlotCSVReport(ArrayList<LoadRunnerTransaction> transactions) {

		logger.println("Transformation CSV started ...");

		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> averages = new ArrayList<String>();

		for (LoadRunnerTransaction tr : this.transactions) {
			headers.add("\"" + tr.getName() + "\"");
			averages.add(String.valueOf(tr.getAvgRT()));
		}

		String scvReport = org.apache.commons.lang3.StringUtils.join(headers, ",")
				+ System.getProperty("line.separator") + org.apache.commons.lang3.StringUtils.join(averages, ",");

		return scvReport;
	}

	/**
	 * param transactions - ArrayList of LoadRunnerTransaction objects
	 * return - string in PerfPublisherReport format
	 */
	private String generatePerfPublisherReport(ArrayList<LoadRunnerTransaction> transactions) {

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
				String trName = "tr_" + tr.getName();

				org.w3c.dom.Element trElement = doc.createElement(trName);
				trElement.setAttribute("unit", "sec");
				trElement.setAttribute("mesure", String.valueOf(tr.getAvgRT()));
				trElement.setAttribute("isRelevant", "yes");
				metricsElement.appendChild(trElement);
			}

			// //////////////////////////////////////////////////////////////////////////
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			stringReport = this.getStringFromDoc(doc);

		} catch (ParserConfigurationException pce) {
			Log.error(pce.getMessage());
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			Log.error(tfe.getMessage());
			tfe.printStackTrace();
		}
		return stringReport;
	}

	/**
	 * Convert org.w3c.dom.Document to String
	 * 
	 * param doc
	 * return
	 */
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

	private class Pair {
		private String value;
		private Boolean flag;

		public Pair(String value, Boolean flag) {
			this.value = value;
			this.flag = flag;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public boolean getFlag() {
			return flag.booleanValue();
		}

		public void setFlag(boolean value) {
			this.flag = Boolean.valueOf(value);
		}
	}
}
