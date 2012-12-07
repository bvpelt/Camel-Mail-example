package nl.bsoft.camelex;

import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.example.reportincident.InputReportIncident;
import org.apache.camel.example.reportincident.OutputReportIncident;
import org.apache.camel.example.reportincident.ReportIncidentEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.log4j.Logger;


/**
 * The webservice we have implemented.
 */
public class ReportIncidentEndpointImpl implements ReportIncidentEndpoint {
	
	private static final Logger log = Logger.getLogger(ReportIncidentEndpointImpl.class);
	
    private CamelContext camel = null;
    
    private ProducerTemplate template;
	
    public ReportIncidentEndpointImpl() throws Exception {
    	
        // create the camel context that is the "heart" of Camel
        camel = new DefaultCamelContext();
       
        /*
        // add the log component
        camel.addComponent("log", new LogComponent());
        
        // add the file component
        camel.addComponent("file", new FileComponent());
        */
        
        // get the ProducerTemplate thst is a Spring'ish xxxTemplate based producer for very
        // easy sending exchanges to Camel.
        template = camel.createProducerTemplate();


        // start Camel
        camel.start();
    }
    
    /* part 1 implementation
    public OutputReportIncident reportIncident(InputReportIncident parameters) {
        log.info("Hello ReportIncidentEndpointImpl is called with givenname:" + parameters.getGivenName()
        		+ ", details: " + parameters.getDetails()
        		+ ", email: " + parameters.getEmail()
        		+ ", familyname: " + parameters.getFamilyName()
        		+ ", incidentdata: " + parameters.getIncidentDate()
        		+ ", incidentid: " + parameters.getIncidentId()
        		+ ", phone: " + parameters.getPhone()
        		+ ", summary: " + parameters.getSummary()
        		);

        OutputReportIncident out = new OutputReportIncident();
        out.setCode("OK");
        return out;
    }
	*/
    
    public OutputReportIncident reportIncident(final InputReportIncident parameters) {
    	 log.info("Hello ReportIncidentEndpointImpl is called with givenname:" + parameters.getGivenName()
         		+ ", details: " + parameters.getDetails()
         		+ ", email: " + parameters.getEmail()
         		+ ", familyname: " + parameters.getFamilyName()
         		+ ", incidentdata: " + parameters.getIncidentDate()
         		+ ", incidentid: " + parameters.getIncidentId()
         		+ ", phone: " + parameters.getPhone()
         		+ ", summary: " + parameters.getSummary()
         		);
        String name = parameters.getGivenName() + " " + parameters.getFamilyName();

        template.sendBody("log:com.mycompany.part2.easy", name);
        
        generateEmailBodyAndStoreAsFile(parameters);
        
        /* part 2 example 01
        String incidentId = parameters.getIncidentId();
        String filename = "easy-incident-" + incidentId + ".txt";
        template.sendBodyAndHeader("file://target/subfolder", name, FileComponent.HEADER_FILE_NAME, filename);
        */
        
        /* part 2 example 00
        // let Camel do something with the name
        sendToCamelLog(name);
        
        // let Camel do something with the name
        sendToCamelLog(name);
        sendToCamelFile(parameters.getIncidentId(), name);
		*/
        
        OutputReportIncident out = new OutputReportIncident();
        out.setCode("OK");
        return out;
    }
   
    private void sendToCamelLog(final String name) {
        try {
            // get the log component
            Component component = camel.getComponent("log");

            // create an endpoint and configure it.
            // Notice the URI parameters this is a common pratice in Camel to configure
            // endpoints based on URI.
            // com.mycompany.part2 = the log category used. Will log at INFO level as default
            Endpoint endpoint = component.createEndpoint("log:com.mycompany.part2");

            // create an Exchange that we want to send to the endpoint
            Exchange exchange = endpoint.createExchange();
            // set the in message payload (=body) with the name parameter
            exchange.getIn().setBody(name);

            // now we want to send the exchange to this endpoint and we then need a producer
            // for this, so we create and start the producer.
            Producer producer = endpoint.createProducer();
            producer.start();
            // process the exchange will send the exchange to the log component, that will process
            // the exchange and yes log the payload
            producer.process(exchange);

            // stop the producer, we want to be nice and cleanup
            producer.stop();

        } catch (Exception e) {
        	log.error("Error during camel processing: ", e);
            // we ignore any exceptions and just rethrow as runtime
            throw new RuntimeException(e);
        }
    }

    private void sendToCamelFile(String incidentId, String name) {
        try {
            // get the file component
            Component component = camel.getComponent("file");

            /* previous example
            // create an endpoint and configure it.
            // Notice the URI parameters this is a common pratice in Camel to configure
            // endpoints based on URI.
            // file://target instructs the base folder to output the files. We put in the target folder
            // then its actumatically cleaned by mvn clean
            //Endpoint endpoint = component.createEndpoint("file://target");
            */
            // create the file endpoint, we cast to FileEndpoint because then we can do
            // 100% java settter based configuration instead of the URI sting based
            // must pass in an empty string, or part of the URI configuration if wanted 
            FileEndpoint endpoint = (FileEndpoint)component.createEndpoint("");
            endpoint.setFile(new File("target/subfolder"));
            endpoint.setAutoCreate(true);

            // create an Exchange that we want to send to the endpoint
            Exchange exchange = endpoint.createExchange();
            // set the in message payload (=body) with the name parameter
            exchange.getIn().setBody(name);

            // now a special header is set to instruct the file component what the output filename
            // should be
            exchange.getIn().setHeader(FileComponent.HEADER_FILE_NAME, "incident-" + incidentId + ".txt");

            // now we want to send the exchange to this endpoint and we then need a producer
            // for this, so we create and start the producer.
            Producer producer = endpoint.createProducer();
            producer.start();
            // process the exchange will send the exchange to the file component, that will process
            // the exchange and yes write the payload to the given filename
            producer.process(exchange);

            // stop the producer, we want to be nice and cleanup
            producer.stop();
        } catch (Exception e) {
            // we ignore any exceptions and just rethrow as runtime
            throw new RuntimeException(e);
        }
    }
    
    private String createMailBody(InputReportIncident parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident ").append(parameters.getIncidentId());
        sb.append(" has been reported on the ").append(parameters.getIncidentDate());
        sb.append(" by ").append(parameters.getGivenName());
        sb.append(" ").append(parameters.getFamilyName());
        
        // and the rest of the mail body with more appends to the string builder
        
        return sb.toString();
    }
    
    private void generateEmailBodyAndStoreAsFile(InputReportIncident parameters) {
        // generate the mail body using velocity template
        // notice that we just pass in our POJO (= InputReportIncident) that we
        // got from Apache CXF to Velocity.
        Object response = template.sendBody("velocity:MailBody.vm", parameters);
        // Note: the response is a String and can be cast to String if needed

        // store the mail in a file
        String filename = "mail-incident-" + parameters.getIncidentId() + ".txt";
        template.sendBodyAndHeader("file://target/subfolder", response, FileComponent.HEADER_FILE_NAME, filename);
    }
}