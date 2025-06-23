package com.jfc.owl.model.owl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class in an OWL ontology. This class is used to transfer class
 * information between the backend and frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OntologyClass {

	/**
	 * The IRI (Internationalized Resource Identifier) of the class
	 */
	private String iri;

	/**
	 * The local name of the class (without the namespace)
	 */
	private String name;

	/**
	 * A human-readable description of the class
	 */
	private String description;

	/**
	 * The list of superclasses of this class
	 */
	private List<String> superClasses = new ArrayList<>();

	/**
	 * The list of subclasses of this class
	 */
	private List<String> subClasses = new ArrayList<>();

	/**
	 * The list of individuals (instances) of this class
	 */
	private List<String> individuals = new ArrayList<>();

	/**
	 * Flag indicating if this is a defined class (with necessary and sufficient
	 * conditions)
	 */
	private boolean defined;

	/**
	 * Flag indicating if this class is primitive (only necessary conditions)
	 */
	private boolean primitive;

	/**
	 * The namespace of the class
	 */
	private String namespace;

	/**
	 * The restrictions applied to this class
	 */
	private List<String> restrictions = new ArrayList<>();

	/**
	 * Get the full IRI of the class
	 * 
	 * @return The full IRI as a string
	 */
	public String getFullIri() {
		return iri;
	}

	/**
	 * Extract the namespace from the IRI
	 * 
	 * @return The namespace part of the IRI
	 */
	public String getNamespace() {
		if (namespace == null && iri != null) {
			int hashIndex = iri.lastIndexOf('#');
			if (hashIndex > 0) {
				namespace = iri.substring(0, hashIndex + 1);
			} else {
				int slashIndex = iri.lastIndexOf('/');
				if (slashIndex > 0) {
					namespace = iri.substring(0, slashIndex + 1);
				} else {
					namespace = "";
				}
			}
		}
		return namespace;
	}

	/**
	 * Get the local name from the IRI if not already set
	 * 
	 * @return The local name of the class
	 */
	public String getName() {
		if (name == null && iri != null) {
			int hashIndex = iri.lastIndexOf('#');
			if (hashIndex > 0) {
				name = iri.substring(hashIndex + 1);
			} else {
				int slashIndex = iri.lastIndexOf('/');
				if (slashIndex > 0) {
					name = iri.substring(slashIndex + 1);
				} else {
					name = iri;
				}
			}
		}
		return name;
	}

	/**
	 * Add a superclass to this class
	 * 
	 * @param superClass The name of the superclass to add
	 */
	public void addSuperClass(String superClass) {
		if (superClasses == null) {
			superClasses = new ArrayList<>();
		}
		if (!superClasses.contains(superClass)) {
			superClasses.add(superClass);
		}
	}

	/**
	 * Add a subclass to this class
	 * 
	 * @param subClass The name of the subclass to add
	 */
	public void addSubClass(String subClass) {
		if (subClasses == null) {
			subClasses = new ArrayList<>();
		}
		if (!subClasses.contains(subClass)) {
			subClasses.add(subClass);
		}
	}

	/**
	 * Add an individual to this class
	 * 
	 * @param individual The name of the individual to add
	 */
	public void addIndividual(String individual) {
		if (individuals == null) {
			individuals = new ArrayList<>();
		}
		if (!individuals.contains(individual)) {
			individuals.add(individual);
		}
	}

	/**
	 * Add a restriction to this class
	 * 
	 * @param restriction The restriction description to add
	 */
	public void addRestriction(String restriction) {
		if (restrictions == null) {
			restrictions = new ArrayList<>();
		}
		if (!restrictions.contains(restriction)) {
			restrictions.add(restriction);
		}
	}
}