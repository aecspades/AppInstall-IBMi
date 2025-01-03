package com.github.theprez.appinstall;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.github.theprez.jcmdutils.AppLogger;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.jcmdutils.StringUtils.TerminalColor;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class AppInstall {
    private static void doBuild(final AppLogger _logger, final LinkedList<String> _args) throws IOException, URISyntaxException, InterruptedException, ObjectDoesNotExistException, PropertyVetoException {
        String arg = "";
        try {
            final InstallPackageBuilder builder = new InstallPackageBuilder(_logger);
            while (!_args.isEmpty()) {
                arg = _args.removeFirst();
                if ("-o".equalsIgnoreCase(arg)) {
                    builder.setOutputFile(_args.removeFirst());
                } else if ("--pre".equalsIgnoreCase(arg)) {
                    builder.addPreInstall(_args.removeFirst());
                } else if ("--post".equalsIgnoreCase(arg)) {
                    builder.addPostInstall(_args.removeFirst());
                } else if ("--qsys".equalsIgnoreCase(arg)) {
                    builder.addLibrary(_args.removeFirst());
                } else if ("--dir".equalsIgnoreCase(arg)) {
                    builder.addBareDirectory(_args.removeFirst());
                } else if ("--file".equalsIgnoreCase(arg)) {
                    builder.addFile(_args.removeFirst());
                } else if ("--spec".equalsIgnoreCase(arg)) {
                    builder.addFromSpecFile(_logger, _args.removeFirst());
                } else if ("--lodrun".equalsIgnoreCase(arg)) {
                    builder.setLodrunLib(_logger, _args.removeFirst());
                } else {
                    _logger.println_err("Urecognized argument: " + arg);
                    _logger.println();
                    printUsageAndExit();
                }
            }
            builder.build();
        } catch (final NoSuchElementException e) {
            _logger.printfln_err("ERROR: Argument '%s' specified without value", arg);
        }
    }

    private static void doInstall(final AppLogger _logger, InstallOptions installOptions) throws IOException, InterruptedException, ObjectDoesNotExistException, PropertyVetoException {
            _logger.println("Doing the installation");
            final PackageConfiguration config = new PackageConfiguration(_logger);
            final ExtractionTask extraction = new ExtractionTask(_logger, config);
            final InstallationTask install = new InstallationTask(_logger, config, extraction.run());
            install.run(installOptions);
    }

    private static boolean isInstallPackage() {
        return null != AppInstall.class.getClassLoader().getResource("APPINSTALL-INF/manifest.yml");
    }

    public static void main(final String[] _args) {
        final LinkedList<String> args = new LinkedList<String>(Arrays.asList(_args));
        final AppLogger logger = AppLogger.getSingleton(args.remove("-v"));
        try {
            if (args.remove("--help") || args.remove("-h")) {
                printUsageAndExit();
            } else if (args.remove("--version")) {
                printVersionInfo(logger);
                System.exit(0);
            } else if (isInstallPackage()) {
            	InstallOptions installOptions = new InstallOptions();
            	// Allow either -y='yes to all ' or -c 'continue if not delete'
            	installOptions.confirm = args.remove("-y") ? 'y' : args.remove("-c") ? 'c' : ' ';
            	installOptions.lodrun = args.remove("-l") || args.remove("--lodrun");
            	// Allow --rstlib/rstasp/rstaspdev
                while (!args.isEmpty()) {
                	String arg = args.removeFirst().toLowerCase();
                	switch (arg) {
                		case "--rstlib":
                			installOptions.rstlib = args.removeFirst();
                			break;
                		case "--rstasp":
                			installOptions.rstasp = args.removeFirst();
                			break;
                		case "--rstaspdev":
                			installOptions.rstaspdev = args.removeFirst();
                	}
                }
                doInstall(logger, installOptions);
            } else {
                doBuild(logger, args);
            }
        } catch (final Exception e) {
            logger.println_err("ERROR: " + e.getLocalizedMessage());
            logger.printExceptionStack_verbose(e);
        }
    }

    private static void printUsageAndExit() {
        System.out.println("");
        if (isInstallPackage()) {
            System.out.println("Usage: java -jar <jarfile> [-v] [-y]");
            System.exit(-1);
        }
        System.out.println("Usage: java -jar <jarfile> -o <package_file> [options] [[component]...]");
        System.out.println("");
        System.out.println("  <package_file> is the file name of the build package you are creating");
        System.out.println("    (.jar file extension is preferred)");
        System.out.println("");
        System.out.println("   Valid options include:");
        System.out.println("       -v          : verbose mode");
        System.out.println("       --version   : print version information");
        System.out.println("       -h/--help   : print this help");
        System.out.println("");
        System.out.println("  Multiple components can be specified. These identify components");
        System.out.println("  of the application for which you are creating an installer.");
        System.out.println("    Valid component values include:");
        System.out.println("        --qsys <library>   : a library in the QSYS.LIB file system");
        System.out.println("        --dir  <dir>       : a directory (contents are not included)");
        System.out.println("        --file <file/dir>  : a file or directory (if a directory, contents are included)");
        System.out.println("        --pre  <file>      : a pre-install script (only one can be specified)");
        System.out.println("        --post <file>      : a post-install script (only one can be specified)");
        System.out.println("        --spec <file>      : a specification file listing application components");
        System.out.println("");
        System.exit(-1);
    }

    private static void printVersionInfo(final AppLogger _logger) {
        System.out.println(StringUtils.colorizeForTerminal("Version: ", TerminalColor.GREEN) + Version.version);
        System.out.println(StringUtils.colorizeForTerminal("Build Date (UTC): ", TerminalColor.GREEN) + Version.compileDateTime);
    }
}
