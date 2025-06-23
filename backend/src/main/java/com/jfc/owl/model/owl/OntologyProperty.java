package com.jfc.owl.model.owl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a property in an OWL ontology (object property or data property).
 * This class is used to transfer property information between the backend and
 * frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OntologyProperty {

	/**
	 * The IRI (Internationalized Resource Identifier) of the property
	 */
	private String iri;

	/**
	 * The local name of the property (without the namespace)
	 */
	private String name;

	/**
	 * A human-readable description of the property
	 */
	private String description;

	/**
	 * The type of property (object, data, annotation)
	 */
	private String type;

	/**
	 * The domain of the property (classes that can have this property)
	 */
	private List<String> domain = new ArrayList<>();

	/**
	 * The range of the property (allowed values or classes)
	 */
	private List<String> range = new ArrayList<>();

	/**
	 * The list of super-properties of this property
	 */
	private List<String> superProperties = new ArrayList<>();

	/**
	 * The list of sub-properties of this property
	 */
	private List<String> subProperties = new ArrayList<>();

	/**
	 * The namespace of the property
	 */
	private String namespace;

	/**
	 * Flag indicating if the property is functional
	 */
	private boolean functional;

	/**
	 * Flag indicating if the property is inverse functional (only for object
	 * properties)
	 */
	private boolean inverseFunctional;

	/**
	 * Flag indicating if the property is transitive (only for object properties)
	 */
	private boolean transitive;

	/**
	 * Flag indicating if the property is symmetric (only for object properties)
	 */
	private boolean symmetric;

	/**
	 * Flag indicating if the property is asymmetric (only for object properties)
	 */
	private boolean asymmetric;

	/**
	 * Flag indicating if the property is reflexive (only for object properties)
	 */
	private boolean reflexive;

	/**
	 * Flag indicating if the property is irreflexive (only for object properties)
	 */
	private boolean irreflexive;

	/**
	 * The inverse property (only for object properties)
	 */
	private String inverseProperty;

	/**
	 * Get the full IRI of the property
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
	 * @return The local name of the property
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
	 * Add a class to the domain of this property
	 * 
	 * @param domainClass The class name to add to the domain
	 */
	public void addDomain(String domainClass) {
		if (domain == null) {
			domain = new ArrayList<>();
		}
		if (!domain.contains(domainClass)) {
			domain.add(domainClass);
		}
	}

	/**
	 * Add a class or datatype to the range of this property
	 * 
	 * @param rangeClass The class or datatype name to add to the range
	 */
	public void addRange(String rangeClass) {
		if (range == null) {
			range = new ArrayList<>();
		}
		if (!range.contains(rangeClass)) {
			range.add(rangeClass);
		}
	}

	/**
	 * Add a super-property to this property
	 * 
	 * @param superProperty The name of the super-property to add
	 */
	public void addSuperProperty(String superProperty) {
		if (superProperties == null) {
			superProperties = new ArrayList<>();
		}
		if (!superProperties.contains(superProperty)) {
			superProperties.add(superProperty);
		}
	}

	/**
	 * Add a sub-property to this property
	 * 
	 * @param subProperty The name of the sub-property to add
	 */
	public void addSubProperty(String subProperty) {
		if (subProperties == null) {
			subProperties = new ArrayList<>();
		}
		if (!subProperties.contains(subProperty)) {
			subProperties.add(subProperty);
		}
	}

	/**
	 * Checks if this is an object property
	 * 
	 * @return true if this is an object property, false otherwise
	 */
	public boolean isObjectProperty() {
		return "object".equalsIgnoreCase(type);
	}

	/**
	 * Checks if this is a data property
	 * 
	 * @return true if this is a data property, false otherwise
	 */
	public boolean isDataProperty() {
		return "data".equalsIgnoreCase(type);
	}

	/**
	 * Checks if this is an annotation property
	 * 
	 * @return true if this is an annotation property, false otherwise
	 */
	public boolean isAnnotationProperty() {
		return "annotation".equalsIgnoreCase(type);
	}
}
