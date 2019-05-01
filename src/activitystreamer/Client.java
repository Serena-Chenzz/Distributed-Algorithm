package activitystreamer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.client.ClientSkeleton;
import activitystreamer.util.Settings;

public class Client {
	
	private static final Logger log = LogManager.getLogger();
	
	private static void help(Options options){
		String header = "An Ticket Purchaser for Unimelb COMP90020\n\n";
		String footer = "\ncontact dev group for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ClientEnd", header, options, footer, true);
		System.exit(-1);
	}
	
	public static void main(String[] args) {
		
		log.info("reading command line options");
		
		Options options = new Options();
		options.addOption("rp",true,"remote port number");
		options.addOption("rh",true,"remote hostname");
		
		
		// build the parser
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			help(options);
		}
	
		if(cmd.hasOption("rh")){
			Settings.setRemoteHostname(cmd.getOptionValue("rh"));
		}
		
		if(cmd.hasOption("rp")){
			try{
				int port = Integer.parseInt(cmd.getOptionValue("rp"));
				Settings.setRemotePort(port);
			} catch (NumberFormatException e){
				log.error("-rp requires a port number, parsed: "+cmd.getOptionValue("rp"));
				help(options);
			}
		}

		log.info("starting client");
					
		ClientSkeleton c = ClientSkeleton.getInstance();
		
	}

	
}
