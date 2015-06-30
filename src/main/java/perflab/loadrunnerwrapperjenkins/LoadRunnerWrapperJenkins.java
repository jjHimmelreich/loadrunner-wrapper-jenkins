package perflab.loadrunnerwrapperjenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.remoting.RemoteOutputStream;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.jfree.util.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link LoadRunnerWrapperJenkins} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class LoadRunnerWrapperJenkins extends Builder {

    //private final String name;
    private final String loadRunnerBin;
    private final String loadRunnerScenario;
    private String loadRunnerResultsFolder;
    private String loadRunnerAnalysisHTMLReportFolder;
    private final String loadRunnerControllerAdditionalAttributes;
    private final String loadRunnerAnalysisTemplateName;
    
    private String loadRunnerResultsSummaryFile;
    private final String loadRunnerResultsSummaryFileFormat;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LoadRunnerWrapperJenkins(//String name,
                             String loadRunnerBin,
                             String loadRunnerScenario,
                             String loadRunnerResultsFolder,
                             String loadRunnerAnalysisTemplateName,
                             String loadRunnerAnalysisHTMLReportFolder,
                             String loadRunnerResultsSummaryFile,
                             String loadRunnerControllerAdditionalAttributes,
                             String loadRunnerResultsSummaryFileFormat) {

        //this.name = name;
        this.loadRunnerBin = loadRunnerBin;
        this.loadRunnerScenario = loadRunnerScenario;
        this.loadRunnerResultsFolder = loadRunnerResultsFolder;
        this.loadRunnerAnalysisHTMLReportFolder = loadRunnerAnalysisHTMLReportFolder;
        this.loadRunnerResultsSummaryFile = loadRunnerResultsSummaryFile;
        this.loadRunnerControllerAdditionalAttributes = loadRunnerControllerAdditionalAttributes;
        this.loadRunnerAnalysisTemplateName = loadRunnerAnalysisTemplateName;
        this.loadRunnerResultsSummaryFileFormat = loadRunnerResultsSummaryFileFormat;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getLoadRunnerBin(){
        return this.loadRunnerBin;
    }

    public String getLoadRunnerScenario() {
        return this.loadRunnerScenario;
    }

    public String getLoadRunnerResultsFolder(){
        return this.loadRunnerResultsFolder;
    }

    public String getLoadRunnerAnalysisHTMLReportFolder(){
        return this.loadRunnerAnalysisHTMLReportFolder;
    }

    public String getLoadRunnerResultsSummaryFile(){
        return this.loadRunnerResultsSummaryFile;
    }
    
    public String getLoadRunnerControllerAdditionalAttributes(){
    	return this.loadRunnerControllerAdditionalAttributes;
    }
    
    public String getLoadRunnerAnalysisTemplateName(){
    	return this.loadRunnerAnalysisTemplateName;
    }
    
    public String getLoadRunnerResultsSummaryFileFormat(){
    	return this.loadRunnerResultsSummaryFileFormat;
    }

    //TODO: https://wiki.jenkins-ci.org/display/JENKINS/Making+your+plugin+behave+in+distributed+Jenkins
    //TODO: http://ccoetech.ebay.com/tutorial-dev-jenkins-plugin-distributed-jenkins    
    private static class LauncherCallable implements Callable<String, IOException>{
    	private BuildListener listener;
		private String loadRunnerBin;
		private String loadRunnerScenario;
		private String loadRunnerResultsFolder;
		private String loadRunnerAnalysisHTMLReportFolder;
		private String loadRunnerResultsSummaryFile;
		private String loadRunnerControllerAdditionalAttributes;
		private String loadRunnerAnalysisTemplateName;
		private String loadRunnerResultsSummaryFileFormat;
		
    	public LauncherCallable(BuildListener listener){
    		this.listener = listener;    		
    	}
    	
  	  	private static final long serialVersionUID = 1L;
        
  	  	public String call() throws IOException {
        	final RemoteOutputStream ros = new RemoteOutputStream(listener.getLogger());
            // This code will run on the build slave
        	//Write on node console
            System.out.println("loadRunnerBin = " + loadRunnerBin);
            System.out.println("loadRunnerScenario = " + this.loadRunnerScenario);
            System.out.println("loadRunnerResultsFolder = " + this.loadRunnerResultsFolder);
            System.out.println("loadRunnerAnalysisHTMLReportFolder = " + this.loadRunnerAnalysisHTMLReportFolder);           
            System.out.println("loadRunnerResultsSummaryFile = " + loadRunnerResultsSummaryFile);
            System.out.println("loadRunnerResultsSummaryFileFormat = " + loadRunnerResultsSummaryFileFormat);

        	//Write on jenkins console
            ros.write("=============================================================\n".getBytes());
            
            ros.write(("loadRunnerBin = " + loadRunnerBin+ "\n").getBytes());
            ros.write(("loadRunnerScenario = " + this.loadRunnerScenario+ "\n").getBytes());
            ros.write(("loadRunnerResultsFolder = " + this.loadRunnerResultsFolder+ "\n").getBytes());
            ros.write(("loadRunnerAnalysisHTMLReportFolder = " + this.loadRunnerAnalysisHTMLReportFolder+ "\n").getBytes());
            ros.write(("loadRunnerResultsSummaryFile = " + loadRunnerResultsSummaryFile+ "\n").getBytes());
            ros.write(("loadRunnerResultsSummaryFileFormat = " + loadRunnerResultsSummaryFileFormat+ "\n").getBytes());

            LoadRunnerWrapper loadRunner = new LoadRunnerWrapper(this.loadRunnerBin, 
            		this.loadRunnerScenario, 
            		this.loadRunnerControllerAdditionalAttributes,
            		this.loadRunnerResultsFolder, 
            		this.loadRunnerAnalysisTemplateName,
            		this.loadRunnerAnalysisHTMLReportFolder, 
            		this.loadRunnerResultsSummaryFile,
            		this.loadRunnerResultsSummaryFileFormat,
            		this.listener.getLogger());
 
            boolean okay = loadRunner.execute();
            //boolean okay = true;
            
            return String.valueOf(okay);
         }

		@Override
		public void checkRoles(RoleChecker arg0) throws SecurityException {
			// TODO Auto-generated method stub
			
		}

		public void init(String loadRunnerBin, String loadRunnerScenario,
				String loadRunnerControllerAdditionalAttributes,
				String loadRunnerResultsFolder,
				String loadRunnerAnalysisTemplateName,
				String loadRunnerAnalysisHTMLReportFolder,
				String loadRunnerResultsSummaryFile,
				String loadRunnerResultsSummaryFileFormat) {
						
			this.loadRunnerBin = loadRunnerBin;
	        this.loadRunnerScenario = loadRunnerScenario;
	        this.loadRunnerResultsFolder = loadRunnerResultsFolder;
	        this.loadRunnerAnalysisHTMLReportFolder = loadRunnerAnalysisHTMLReportFolder;
	        this.loadRunnerResultsSummaryFile = loadRunnerResultsSummaryFile;
	        this.loadRunnerControllerAdditionalAttributes = loadRunnerControllerAdditionalAttributes;
	        this.loadRunnerAnalysisTemplateName = loadRunnerAnalysisTemplateName;	
	        this.loadRunnerResultsSummaryFileFormat = loadRunnerResultsSummaryFileFormat;
		}
     }
    
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.
        boolean okay = true;
    	
    	// Get a "channel" to the build machine and run the task there

    	try {
    		LauncherCallable remoteLauncher = new LauncherCallable(listener);
    		
    		if (this.loadRunnerResultsSummaryFileFormat.equals("PerfPublisherReport")){
    			loadRunnerResultsSummaryFile = "%WORKSPACE%\\lr_summary_PerfPublisher.xml";
    		}else if(this.loadRunnerResultsSummaryFileFormat.equals("PlotCSVReport")){
    			loadRunnerResultsSummaryFile = "%WORKSPACE%\\lr_summary_PlotCSV.csv";
    		}else if(this.loadRunnerResultsSummaryFileFormat.equals("jUnitReport")){
    			loadRunnerResultsSummaryFile = "%WORKSPACE%\\lr_summary_jUnit.xml";
    		}    		    		

    		String buildNumber =  String.valueOf(build.getNumber());
    		String workspacePath = StringEscapeUtils.escapeJava(build.getWorkspace().toString()); 
    		
    		loadRunnerResultsFolder = interpolatePath(loadRunnerResultsFolder, "BUILD_NUMBER", buildNumber);
    		loadRunnerAnalysisHTMLReportFolder = interpolatePath(loadRunnerAnalysisHTMLReportFolder, "BUILD_NUMBER", buildNumber);
    		loadRunnerResultsSummaryFile = interpolatePath(loadRunnerResultsSummaryFile, "BUILD_NUMBER", buildNumber);
    		
    		loadRunnerResultsFolder = interpolatePath(loadRunnerResultsFolder, "WORKSPACE", workspacePath);
    		loadRunnerAnalysisHTMLReportFolder = interpolatePath(loadRunnerAnalysisHTMLReportFolder, "WORKSPACE", workspacePath);
    		loadRunnerResultsSummaryFile = interpolatePath(loadRunnerResultsSummaryFile, "WORKSPACE", workspacePath);		
    		
    		remoteLauncher.init(loadRunnerBin, 
					loadRunnerScenario, 
					loadRunnerControllerAdditionalAttributes,
					loadRunnerResultsFolder, 
					loadRunnerAnalysisTemplateName,
					loadRunnerAnalysisHTMLReportFolder, 
					loadRunnerResultsSummaryFile,
					loadRunnerResultsSummaryFileFormat);
    		
    		String okayString = launcher.getChannel().call(remoteLauncher);    		

    		okay = Boolean.valueOf(okayString);
    		
    		listener.getLogger().println("got okayString="+ okayString +" from remote node");
    		
    	} catch (Exception e) {
    		RuntimeException re = new RuntimeException();
    		re.initCause(e);
    		throw re;
    	}

        return okay;
    }

    private String interpolatePath(String pathToInterpolate, String pattern, String replacement) {

        String dbgMessage = "Interpolating " + pathToInterpolate + " replace " + pattern + " with " + replacement;

    	String interpolatedString = pathToInterpolate;//.replaceAll("\\", "\\\\");
    	
		interpolatedString = interpolatedString.replaceAll("%"+pattern+"%", replacement);
		interpolatedString = interpolatedString.replaceAll("\\$\\{"+pattern+"\\}", replacement);

        Log.debug(dbgMessage + " = " + interpolatedString);

		return interpolatedString;		
	}

	// Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link LoadRunnerWrapperJenkins}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/LoadRunnerWrapperJenkins/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        //private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form fields.
         */
        public FormValidation doCheckLoadRunnerBin(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.trimToNull(value) == null || value.length() == 0){
                return FormValidation.error("LoadRunner bin folder should not be empty. For example: C:\\Program Files (x86)\\HP\\LoadRunner\\bin");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckLoadRunnerScenario(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.trimToNull(value) == null || value.length() == 0){
                return FormValidation.error("LoadRunner scenario path should not be empty. For example: C:\\scenario\\Scenario1.lrs");
            }
            
            String fileExtension=null;
            try {
                fileExtension=value.substring(value.lastIndexOf('.') + 1);
            }catch (Exception e) {
                return FormValidation.error("LoadRunner scenario must be a file!");
            }
            if (!fileExtension.equals("lrs")){
            	return FormValidation.error("LoadRunner scenario be a lrs file!");
            }
            
            return FormValidation.ok();
        }

        public FormValidation doCheckLoadRunnerControllerAdditionalAttributes(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.trimToNull(value) != null && value.length() > 0){
            	String[] parts = value.split(" ");
            	
            	//check if there is even number of parameters
            	if(parts.length % 2 != 0){ 
                    return FormValidation.error("Additional attributes should be supplied in pairs or empty. For example -para1 value1 -param2 value2");
            	}
            	
            	for (int i = 0 ; i < parts.length ; i+=2){
            		if(!parts[i].startsWith("-")){
            			return FormValidation.error("Additional attributes should be supplied in pairs or empty. For example -para1 value1 -param2 value2");
            		}
            	}            	
            }
                        
            return FormValidation.ok();
        }
       
        public FormValidation doCheckLoadRunnerResultsFolder(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.trimToNull(value) == null || value.length() == 0){
                return FormValidation.error("Results folder should no be empty. For example: %WORKSPACE%\\%BUILD_NUMBER%");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckLoadRunnerAnalysisTemplateName(@QueryParameter String value) throws IOException, ServletException {
            //if (StringUtils.trimToNull(value) == null || value.length() == 0){
            //    return FormValidation.error("");
            //}
            return FormValidation.ok();
        }

        public FormValidation doCheckLoadRunnerAnalysisHTMLReportFolder(@QueryParameter String value) throws IOException, ServletException {
            if (StringUtils.trimToNull(value) == null || value.length() == 0){
                return FormValidation.error("Path to LoadRunner html report should not be empty. Please supply the part as it configured in Load Runner Analysis template");
            }
            return FormValidation.ok();
        }
        
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "HP LoadRunner Wrapper by Perflab";
        }
    }
}

