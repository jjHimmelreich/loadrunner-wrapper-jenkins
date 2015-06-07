package perflab.loadrunnerwrapperjenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
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
public class HelloWorldBuilder extends Builder {

    private final String name;
    private final String loadRunnerBin;
    private final String loadRunnerScenario;
    private final String loadRunnerResultsFolder;
    private final String loadRunnerAnalysisHTMLReportFolder;
    private final String loadRunnerResultsSummaryFile;
    private final String loadRunnerControllerAdditionalAttributes;
    private final String loadRunnerAnalysisTemplateName;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public HelloWorldBuilder(String name,
                             String loadRunnerBin,
                             String loadRunnerScenario,
                             String loadRunnerResultsFolder,
                             String loadRunnerAnalysisTemplateName,
                             String loadRunnerAnalysisHTMLReportFolder,
                             String loadRunnerResultsSummaryFile,
                             String loadRunnerControllerAdditionalAttributes) {

        this.name = name;
        this.loadRunnerBin = loadRunnerBin;
        this.loadRunnerScenario = loadRunnerScenario;
        this.loadRunnerResultsFolder = loadRunnerResultsFolder;
        this.loadRunnerAnalysisHTMLReportFolder = loadRunnerAnalysisHTMLReportFolder;
        this.loadRunnerResultsSummaryFile = loadRunnerResultsSummaryFile;
        this.loadRunnerControllerAdditionalAttributes = loadRunnerControllerAdditionalAttributes;
        this.loadRunnerAnalysisTemplateName = loadRunnerAnalysisTemplateName;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return this.name;
    }

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

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.
        boolean okay = true;

        listener.getLogger().println("run scenario = " + loadRunnerScenario);
        listener.getLogger().println("results-folder = " + loadRunnerResultsFolder);
        listener.getLogger().println("html-results-folder = " + loadRunnerAnalysisHTMLReportFolder);
        listener.getLogger().println("summary-file = " + loadRunnerResultsSummaryFile);
        listener.getLogger().println("loadRunnerBin = " + loadRunnerBin);

        LoadRunnerWrapper loadRunner = new LoadRunnerWrapper(loadRunnerBin, 
        													loadRunnerScenario, 
        													loadRunnerControllerAdditionalAttributes,
        													loadRunnerResultsFolder, 
        													loadRunnerAnalysisTemplateName,
        													loadRunnerAnalysisHTMLReportFolder, 
        													loadRunnerResultsSummaryFile,
        													listener.getLogger());
        
        
        
        okay = loadRunner.execute();

        return okay;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
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
        private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
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

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }
    }
}

