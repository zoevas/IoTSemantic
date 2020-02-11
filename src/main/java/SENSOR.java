import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SENSOR {

	public static final String NAMESPACE = "http://www.semanticweb.org/user/ontologies/2020/1/activity#";
	
	public static final IRI HAS_ELEMENT = getIRI("hasElement");
	
	public static final IRI HAS_OBSERVATION = getIRI("hasObservation");
	
	public static final IRI HAS_STARTDATE = getIRI("hasStartDate");
	
	public static final IRI HAS_ENDDATE = getIRI("hasEndDate");
	
	public static final IRI HAS_CONTENTSTRING = getIRI("hasContentString");	
	
	private static IRI getIRI(String localName) {
		return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
	}

}
