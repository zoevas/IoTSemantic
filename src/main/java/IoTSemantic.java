import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
/**
* <h1>Loading IoT ontology and data on GraphDB</h1>
*
* <p>
* <b>Note:</b> 
* This application loads an ontology and rdf statetements as instances into
* a GraphDB repository
* It uploads an ontology for the sensor measurements of a house.
* It reads a json file (example_observations.json) containing the
* sensor measurements and converts them into rdf statements.
*
* @author  Zoe Vasileiou
* @version 1.0
* @since   2020-02-11
*/

public class IoTSemantic {

	private RepositoryConnection connection;

	public IoTSemantic(RepositoryConnection connection) {
		this.connection = connection;
	}
    
    /**
     * It reads Activity_Turtle.owl file containing the ontology statements for the activities
     * produced by the sensor house and commits them to a GraphDB repository.
     * @param Nothing
     * @return Nothing.
     * @exception RDFParseException, RepositoryException, IOException
     */
	public void loadOntology() throws RDFParseException, RepositoryException, IOException {
		System.out.println("# Loading ontology ...");
    	
		// When adding data we need to start a transaction
		connection.begin();
		
		connection.add(IoTSemantic.class.getResourceAsStream("/Activity_Ontology.owl"), "urn:base", RDFFormat.TURTLE);
	}
    
    /**
     * It reads example_observation.json file which contains the sensor measurements in json format
     * This function reads the JSON file, converts it to RDF statements and commits them to a GraphDB repository.
     * @param Nothing
     * @return Nothing.
     * @exception IOException, URISyntaxException
     */
    public void loadData() throws IOException, URISyntaxException {
    	System.out.println("# Loading data ...");
    	JsonReader reader = new JsonReader(new FileReader("C:\\example_observations.json"));
    	JsonElement jsonElement = new JsonParser().parse(reader);

    	ModelBuilder builder = new ModelBuilder();
    	builder.setNamespace("a", SENSOR.NAMESPACE);

    	ValueFactory factory = SimpleValueFactory.getInstance();

    	JsonObject jsonObject = jsonElement.getAsJsonObject();
    	jsonObject = jsonObject.getAsJsonObject("model");
    	JsonArray jActivitiesArray = jsonObject.getAsJsonArray("activities");
        
    	for(int i = 0; i < jActivitiesArray.size(); i++) {
    		jsonObject = jActivitiesArray.get(i).getAsJsonObject();        	
        	
    		IRI activityIRI = factory.createIRI(SENSOR.NAMESPACE, "Activity_" + i);
    		IRI elementIRI = factory.createIRI(SENSOR.NAMESPACE, "Element_" + i);
        	
    		builder.subject(activityIRI).add(RDF.TYPE, "a:Activity").add(SENSOR.HAS_ELEMENT,elementIRI);
    		builder.subject(elementIRI).add(RDF.TYPE, "a:Element").add(SENSOR.HAS_STARTDATE,factory.createLiteral(jsonObject.get("start").getAsString(), XMLSchema.DATETIME))
        															.add(SENSOR.HAS_ENDDATE,factory.createLiteral(jsonObject.get("end").getAsString(), XMLSchema.DATETIME))
        															.add(SENSOR.HAS_CONTENTSTRING,jsonObject.get("content").getAsString());
        	

    		JsonArray jObservationsArray = jsonObject.getAsJsonArray("observations");
        	
    		IRI observationIRI = factory.createIRI(SENSOR.NAMESPACE, "Observation_" + i);
        	
    		builder.subject(activityIRI).add(RDF.TYPE, "a:Activity").add(SENSOR.HAS_OBSERVATION, observationIRI);
    		builder.subject(observationIRI).add(RDF.TYPE, "a:Observation");
        	
    		for(int j = 0; j < jObservationsArray.size(); j++) {
    			jsonObject = jObservationsArray.get(j).getAsJsonObject();
        		
    			IRI observationElementIRI = factory.createIRI(SENSOR.NAMESPACE, "Element_" + i + "_" + j);
    			builder.subject(observationIRI).add(SENSOR.HAS_ELEMENT, observationElementIRI);
    			builder.subject(observationElementIRI).add(RDF.TYPE, "a:Element");
        		
    			builder.subject(observationElementIRI).add(SENSOR.HAS_STARTDATE,factory.createLiteral(jsonObject.get("start").getAsString(), XMLSchema.DATETIME))
    											.add(SENSOR.HAS_ENDDATE,factory.createLiteral(jsonObject.get("end").getAsString(), XMLSchema.DATETIME))
        										.add(SENSOR.HAS_CONTENTSTRING,jsonObject.get("content").getAsString());
        	}
        }
        
    	Model model = builder.build();
        
    	//Printing statements for debugging
    	for(Statement st: model) {
    		System.out.println(st);
    	}
        
    	//Outputting the sensor measurements as rdf statements so as SpinActivityOntology app to use them
    	File file = new File("C:\\Activity_Statements.owl");
    	FileOutputStream out = new FileOutputStream(file);
    	try {
    		Rio.write(model, out, RDFFormat.TURTLE);
    	}
    	finally {
        	out.close();
    	}
           
    	connection.add(model);
    }
	
	public static void main(String[] args) throws RDFParseException, UnsupportedRDFormatException, IOException, URISyntaxException {
		// Abstract representation of a remote repository accessible over HTTP
		HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/activity");

		// Separate connection to a repository
		RepositoryConnection connection = repository.getConnection();

		// Clear the repository before we start
		connection.clear();

		IoTSemantic iotSemantic = new IoTSemantic(connection);
		try {
			iotSemantic.loadOntology();
			iotSemantic.loadData();
        	// Committing the transaction persists the data
			connection.commit();
		} finally {
        	connection.close();
		}
	}
}