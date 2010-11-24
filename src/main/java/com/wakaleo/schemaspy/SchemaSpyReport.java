package com.wakaleo.schemaspy;

import com.wakaleo.schemaspy.util.JDBCHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.schemaspy.Main;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.doxia.siterenderer.Renderer;

/**
 * The SchemaSpy Maven plugin report.
 * @author John Smart
 * @mainpage The SchemaSpy Maven plugin 
 * This plugin is designed to generate SchemaSpy reports for a Maven web site.
 *
 * SchemaSpy (http://schemaspy.sourceforge.net) does not need to be installed
 * and accessible on your machine. However, SchemaSpy also needs the Graphvis
 * tool (http://www.graphviz.org/) in order to generate graphical
 * representations of the table/view relationships, so this needs to be
 * installed on your machine.
 *
 * The schemaspy goal invokes the SchemaSpy command-line tool. SchemaSpy
 * generates a graphical and HTML report describing a given relational database.
 *
 * @goal schemaspy
 */
public class SchemaSpyReport extends AbstractMavenReport {

    /**
     * The output directory for the intermediate report.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File targetDirectory;

    /**
     * The output directory where the final HTML reports will be generated. Note
     * that the reports are always generated in a directory called "schemaspy".
     * The output directory refers to the directory in which the "schemaspy"
     * will be generated.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     *            default-value="${project.build.directory}/site"
     * @required
     */
    private String outputDirectory;

    /**
     * Site rendering component for generating the HTML report.
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * The Maven project object.
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The name of the database being analysed.
     *
     * @parameter database
     * @required
     */
    private String database;

    /**
     * The host address of the database being analysed.
     *
     * @parameter host
     */
    private String host;

    /**
     * The port, required by some drivers.
     *
     * @parameter port
     */
    private String port;

    /**
     * The JDBC URL to be used to connect to the database. Rather than defining
     * the database and host names and letting SchemaSpy build the URL, you can
     * alternatively specify the complete JDBC URL using this parameter. If this
     * parameter is defined, it will override the host address (which, as a
     * result, is not needed). Note that you still need to specify the database
     * type, since SchemaSpy uses its own database properties file for extra
     * information about each database.
     *
     * @todo Would it be possible to guess the database type from the form of
     *       the URL?
     * @parameter jdbcUrl
     */
    private String jdbcUrl;

    /**
     * The type of database being analysed - defaults to ora.
     *
     * @parameter databaseType
     */
    private String databaseType;

    /**
     * Connect to the database with this user id.
     *
     * @parameter user
     */
    private String user;

    /**
     * Database schema to use - defaults to the specified user.
     *
     * @parameter schema
     */
    private String schema;

    /**
     * Database password to use - defaults to none.
     *
     * @parameter password
     */
    private String password;

    /**
     * If specified, SchemaSpy will look for JDBC drivers on this path, rather
     * than using the application classpath. Useful if your database has a
     * non-O/S driver not bundled with the plugin.
     *
     * @parameter pathToDrivers
     */
    private String pathToDrivers;

    /**
     * Schema description. Displays the specified textual description on summary
     * pages. If your description includes an equals sign then escape it with a
     * backslash. NOTE: This field doesn't seem to be used by SchemaSpy in the
     * current version.
     *
     * @parameter schemaDescription
     */
    private String schemaDescription;

    /**
     * Only include matching tables/views. This is a regular expression that's
     * used to determine which tables/views to include. For example: -i
     * "(.*book.*)|(library.*)" includes only those tables/views with 'book' in
     * their names or that start with 'library'. You might want to use
     * "description" with this option to describe the subset of tables.
     *
     * @parameter includeTableNamesRegex
     */
    private String includeTableNamesRegex;

    /**
     * Exclude matching columns from relationship analysis to simplify the
     * generated graphs. This is a regular expression that's used to determine
     * which columns to exclude. It must match table name, followed by a dot,
     * followed by column name. For example: -x "(book.isbn)|(borrower.address)"
     * Note that each column name regular expression must be surround by ()'s
     * and separated from other column names by a |.
     *
     * @parameter excludeColumnNamesRegex
     */
    private String excludeColumnNamesRegex;

    /**
     * Allow HTML In Comments. Any HTML embedded in comments normally gets
     * encoded so that it's rendered as text. This option allows it to be
     * rendered as HTML.
     *
     * @parameter allowHtmlInComments
     */
    private Boolean allowHtmlInComments;

    /**
     * Comments Initially Displayed. Column comments are normally hidden by
     * default. This option displays them by default.
     *
     * @parameter commentsInitiallyDisplayed
     */
    private Boolean commentsInitiallyDisplayed;

    /**
     * Don't display table-based comments. for databases like MySQL that stuff
     * unrelated data where comments belong.
     *
     * @parameter noTableComments
     */
    private Boolean noTableComments;

    /**
     * Don't include implied foreign key relationships in the generated table
     * details.
     *
     * @parameter noImplied
     */
    private Boolean noImplied;

    /**
     * Only generate files needed for insertion/deletion of data (e.g. for
     * scripts).
     *
     * @parameter noHtml
     */
    private Boolean noHtml;

    /**
     * Some databases, like Derby, will crash if you use the old driver object
     * to establish a connection (eg "connection = driver.connect(...)"). In
     * this case, set useDriverManager to true to use the
     * DriverManager.getConnection() method instead (eg "connection =
     * java.sql.DriverManager.getConnection(...)"). Other databases (eg MySQL)
     * seem to only work with the first method, so don't use this parameter
     * unless you have to.
     *
     * @parameter useDriverManager
     */
    private Boolean useDriverManager;

    /**
     * The CSS Stylesheet. Allows you to override the default SchemaSpyCSS
     * stylesheet.
     *
     * @parameter cssStylesheet
     */
    private String cssStylesheet;

    /**
     * Utility class to help determine the type of the target database.
     */
    private JDBCHelper jdbcHelper = new JDBCHelper();

    /**
     * Convenience method used to build the schemaspy command line parameters.
     * @param argList the current list of schemaspy parameter options.
     * @param parameter a new parameter to add
     * @param value the value for this parameter
     */
    private void addToArguments(final List<String> argList,
            final String parameter,
            final Boolean value) {
        if ((value != null) && (value)) {
            argList.add(parameter + "=" + value);
        }
    }

    /**
     * Convenience method used to build the schemaspy command line parameters.
     * @param argList the current list of schemaspy parameter options.
     * @param parameter a new parameter to add
     * @param value the value for this parameter
     */
    private void addFlagToArguments(final List<String> argList,
            final String parameter,
            final Boolean value) {
        if ((value != null) && (value)) {
            argList.add(parameter);
        }
    }

    /**
     * Convenience method used to build the schemaspy command line parameters.
     * @param argList the current list of schemaspy parameter options.
     * @param parameter a new parameter to add
     * @param value the value for this parameter
     */
    private void addToArguments(final List<String> argList,
            final String parameter,
            final String value) {
        if (value != null) {
            argList.add(parameter + "=" + value);
        }
    }

    /**
     * Generate the Schemaspy report.
     * @throws MavenReportException if schemaspy crashes
     * @param locale the language of the report - currently ignored.
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {

        //
        // targetDirectory should be set by the maven framework. This is
        // jusr for unit testing purposes
        //
        if (targetDirectory == null) {
            targetDirectory = new File("target");
        }

        targetDirectory.mkdirs();
        File siteDir = new File(targetDirectory, "site");
        siteDir.mkdirs();
        File outputDir = null;
        if (outputDirectory == null) {
            outputDir = new File(siteDir, "schemaspy");
            outputDir.mkdirs();
            outputDirectory = outputDir.getAbsolutePath();
        } else {
            outputDir = new File(new File(outputDirectory), "schemaspy");
            outputDir.mkdirs();
        }
        String schemaSpyDirectory = outputDir.getAbsolutePath();
        List<String> argList = new ArrayList<String>();

        Boolean useCurrentClasspath = true;
        if (pathToDrivers != null) {
            addToArguments(argList, "-cp", pathToDrivers);
            useCurrentClasspath = false;
        }

        if ((jdbcUrl != null) && (databaseType == null)) {
            databaseType = jdbcHelper.extractDatabaseType(jdbcUrl);
        }
        addToArguments(argList, "-db", database);
        addToArguments(argList, "-host", host);
        addToArguments(argList, "-port", port);
        addToArguments(argList, "-t", databaseType);
        addToArguments(argList, "-u", user);
        addToArguments(argList, "-p", password);
        addToArguments(argList, "-s", schema);
        addToArguments(argList, "-o", schemaSpyDirectory);
        addToArguments(argList, "-desc", schemaDescription);
        addToArguments(argList, "-i", includeTableNamesRegex);
        addToArguments(argList, "-x", excludeColumnNamesRegex);
        addToArguments(argList, "-jdbcUrl", jdbcUrl);
        addFlagToArguments(argList, "-ahic", allowHtmlInComments);
        addFlagToArguments(argList, "-cid", commentsInitiallyDisplayed);
        addFlagToArguments(argList, "-notablecomments", noTableComments);
        addFlagToArguments(argList, "-noimplied", noImplied);
        addFlagToArguments(argList, "-nohtml", noHtml);
        addToArguments(argList, "-useDriverManager", useDriverManager);
        addToArguments(argList, "-useCurrentClasspath", useCurrentClasspath);
        addToArguments(argList, "-css", cssStylesheet);

        String[] args = (String[]) argList.toArray(new String[0]);
        for (String arg : args) {
            getLog().info(arg);
        }
        Main.main(args);
    }

    @Override
    protected String getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }
    
    /**
     * Describes the report.
     */
    public String getDescription(Locale locale) {
        return "SchemaSpy database documentation";
    }

    /**
     * Not really sure what this does ;-).
     */
    public String getName(Locale locale) {
        return "SchemaSpy";
    }

    public String getOutputName() {
        return "schemaspy/index";
    }

    /**
     * Always return true as we're using the report generated by SchemaSpy
     * rather than creating our own report.
     *
     * @return true
     */
    public boolean isExternalReport() {
        return true;
    }

}
